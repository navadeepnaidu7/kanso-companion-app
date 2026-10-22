package com.navadeep.kanso.companion.data

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray

data class CompanionState(
    val connected: Boolean = false,
    val preview: Boolean = false,
    val busy: Boolean = false,
    val tvName: String = "Connect",
    val devices: List<TvDevice> = emptyList(),
    val selectedDevice: TvDevice? = null,
    val apps: List<TvApp> = emptyList(),
    val dockIds: List<String> = emptyList(),
    val loadingApps: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val activeAppId: String? = null,
    val launchingId: String? = null,
    val launchRevision: Int = 0,
    val receiverVersion: Int = 1,
)

class CompanionViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("paired-tv", 0)
    private val arrangement = application.getSharedPreferences("app-arrangement", 0)
    private val artwork = AppArtwork()
    private val client = RemoteClient()
    // Artwork transfers must never hold the input connection's request lock.
    private val libraryClient = RemoteClient()
    private val mutableState = MutableStateFlow(CompanionState())
    val state = mutableState.asStateFlow()
    private val discovery = TvDiscovery(application) { devices -> mutableState.update { it.copy(devices = devices) } }
    private val commands = Channel<Pair<String, String>>(32)
    private var connectionJob: Job? = null
    private var heartbeat: Job? = null
    private var libraryJob: Job? = null
    private var noticeJob: Job? = null
    private var epoch = 0

    init {
        // Restore saved artwork even when the TV is asleep. Launches still require a connection.
        viewModelScope.launch {
            val certificate = prefs.getString("certificate", null) ?: return@launch
            val cache = android.util.AtomicFile(java.io.File(application.cacheDir, "library-${dockKey()}.json"))
            val saved = withContext(Dispatchers.IO) {
                runCatching { JSONArray(cache.readFully().toString(Charsets.UTF_8)) }.getOrNull()
            } ?: return@launch
            val apps = try { decodeApps(saved) } catch (_: org.json.JSONException) { emptyList() }
            if (prefs.getString("certificate", null) == certificate && !state.value.preview && state.value.apps.isEmpty()) {
                mutableState.update { it.copy(apps = AppArrangement.library(apps), dockIds = loadDock(apps),
                    tvName = prefs.getString("name", "TV") ?: "TV") }
            }
        }
        viewModelScope.launch {
            for ((type, value) in commands) {
                if (!state.value.connected) continue
                val generation = epoch
                try {
                    if (type == "launch") {
                        client.request("launch", JSONObject().put("id", value))
                        if (generation == epoch) mutableState.update { it.copy(activeAppId = value,
                            launchingId = null, launchRevision = it.launchRevision + 1) }
                    } else {
                        client.request("command", JSONObject().put("key", value))
                        if (value == "home" && generation == epoch) mutableState.update { it.copy(activeAppId = null) }
                    }
                } catch (error: Exception) {
                    if (generation == epoch) {
                        if (type == "launch") mutableState.update { it.copy(launchingId = null) }
                        if (error is RemoteError && error.message != "Pair this phone again") notice(error.message ?: "Action unavailable")
                        else handleFailure(error)
                    }
                }
            }
        }
    }

    fun startDiscovery() = discovery.start()
    fun stopDiscovery() = discovery.stop()
    fun clearError() { mutableState.update { it.copy(error = null) } }
    fun select(device: TvDevice) { mutableState.update { it.copy(selectedDevice = device, error = null) } }
    fun unselect() { mutableState.update { it.copy(selectedDevice = null, error = null) } }

    fun pair(code: String) {
        val device = state.value.selectedDevice ?: return
        if (state.value.busy) return
        connect(device, code)
    }

    private fun connect(device: TvDevice, code: String? = null) {
        connectionJob?.cancel()
        heartbeat?.cancel()
        libraryJob?.cancel()
        val generation = ++epoch
        client.close()
        libraryClient.close()
        mutableState.update { it.copy(busy = true, preview = false, connected = false, error = null) }
        connectionJob = viewModelScope.launch {
            try {
                val certificate = if (code == null) prefs.getString("certificate", null) else null
                client.connect(device, certificate)
                val token = if (code != null) {
                    client.request("pair", JSONObject().put("code", code)).getString("token")
                } else prefs.getString("token", "") ?: ""
                client.authorize(token)
                val hello = client.request("hello")
                prefs.edit().putString("host", device.host).putInt("port", device.port)
                    .putString("name", hello.optString("name", device.name))
                    .putString("token", token).putString("certificate", client.fingerprint).apply()
                if (generation != epoch) return@launch
                mutableState.update { it.copy(connected = true, busy = false,
                    tvName = hello.optString("name", device.name), selectedDevice = null,
                    receiverVersion = hello.optInt("version", 1), activeAppId = hello.optString("activeApp").ifBlank { null }) }
                refreshApps()
                heartbeat = viewModelScope.launch {
                    while (true) {
                        delay(1200)
                        try {
                            val status = client.request("hello")
                            if (generation == epoch && status.has("activeApp") && state.value.launchingId == null) {
                                mutableState.update { it.copy(activeAppId = status.optString("activeApp").ifBlank { null }) }
                            }
                        } catch (error: Exception) {
                            if (generation == epoch) handleFailure(error)
                            break
                        }
                    }
                }
            } catch (error: Exception) {
                if (generation == epoch) handleFailure(error)
            }
        }
    }

    fun resume() {
        if (state.value.preview || state.value.connected || state.value.busy) return
        val host = prefs.getString("host", null) ?: return
        connect(TvDevice(prefs.getString("name", "TV") ?: "TV", host, prefs.getInt("port", 9876)))
    }

    fun suspendConnection() {
        ++epoch
        connectionJob?.cancel(); heartbeat?.cancel(); libraryJob?.cancel(); discovery.stop(); client.close(); libraryClient.close()
        while (commands.tryReceive().isSuccess) { /* Never replay old taps after reconnect. */ }
        mutableState.update { it.copy(connected = false, busy = false, launchingId = null, activeAppId = null, loadingApps = false) }
    }

    fun forget() {
        suspendConnection()
        prefs.edit().clear().apply()
        mutableState.value = CompanionState()
    }

    fun preview() {
        suspendConnection()
        mutableState.value = CompanionState(preview = true, tvName = "Preview",
            apps = AppArrangement.library(previewApps), dockIds = loadDock(previewApps, preview = true))
    }

    private fun dockKey(preview: Boolean = state.value.preview): String =
        if (preview) "preview" else "tv-${prefs.getString("certificate", "unpaired")?.take(16)}"

    private fun loadDock(apps: List<TvApp>, preview: Boolean = state.value.preview): List<String> {
        val saved = arrangement.getString(dockKey(preview), null)?.split('|')?.filter { it.isNotEmpty() }
        return AppArrangement.dock(apps, saved)
    }

    private fun saveDock(ids: List<String>) {
        arrangement.edit().putString(dockKey(), ids.joinToString("|")).apply()
        mutableState.update { it.copy(dockIds = ids) }
    }

    fun pin(app: TvApp, replace: String? = null) {
        val ids = state.value.dockIds
        if (app.id in ids) return
        if (ids.size < AppArrangement.DockSize) saveDock(ids + app.id)
        else if (replace in ids) saveDock(ids.map { if (it == replace) app.id else it })
    }

    fun unpin(app: TvApp) = saveDock(state.value.dockIds.filter { it != app.id })
    fun movePin(app: TvApp, direction: Int) = saveDock(AppArrangement.move(state.value.dockIds, app.id, direction))

    fun command(key: String) {
        if (state.value.preview) {
            if (key == "home") mutableState.update { it.copy(activeAppId = null) }
            return
        }
        if (!state.value.connected) { notice("Connect your TV"); return }
        if (key in setOf("channel_up", "channel_down", "volume_mute") && state.value.receiverVersion < 3) {
            notice("Update Kanso Receiver on your TV for this control")
            return
        }
        if (!commands.trySend("command" to key).isSuccess) notice("TV is catching up")
    }

    fun launch(app: TvApp) {
        if (state.value.launchingId != null) return
        if (state.value.preview) {
            mutableState.update { it.copy(activeAppId = app.id, launchRevision = it.launchRevision + 1) }
            return
        }
        if (!state.value.connected) { notice("Connect your TV"); return }
        mutableState.update { it.copy(launchingId = app.id) }
        if (!commands.trySend("launch" to app.id).isSuccess) {
            mutableState.update { it.copy(launchingId = null) }
            notice("TV is catching up")
        }
    }

    fun refreshApps() {
        if (!state.value.connected || state.value.loadingApps) return
        mutableState.update { it.copy(loadingApps = true, error = null) }
        val generation = epoch
        val cache = android.util.AtomicFile(java.io.File(getApplication<Application>().cacheDir, "library-${dockKey()}.json"))
        libraryJob = viewModelScope.launch {
            try {
                if (state.value.apps.isEmpty()) {
                    val cached = withContext(Dispatchers.IO) {
                        runCatching { JSONArray(cache.readFully().toString(Charsets.UTF_8)) }.getOrNull()
                    }
                    if (cached != null) {
                        val restored = try { decodeApps(cached) } catch (_: org.json.JSONException) { emptyList() }
                        if (generation == epoch) mutableState.update { it.copy(apps = AppArrangement.library(restored), dockIds = loadDock(restored)) }
                    }
                }
                val host = prefs.getString("host", null) ?: error("TV disconnected")
                libraryClient.connect(TvDevice(state.value.tvName, host, prefs.getInt("port", 9876)), prefs.getString("certificate", null))
                libraryClient.authorize(prefs.getString("token", "").orEmpty())
                val result = libraryClient.request("apps").getJSONArray("apps")
                val apps = decodeApps(result)
                if (generation == epoch) mutableState.update { it.copy(apps = AppArrangement.library(apps),
                    dockIds = loadDock(apps), loadingApps = false) }
                withContext(Dispatchers.IO) {
                    runCatching {
                        val stream = cache.startWrite()
                        try { stream.write(result.toString().toByteArray()); cache.finishWrite(stream) }
                        catch (error: Exception) { cache.failWrite(stream); throw error }
                    }
                }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                if (generation == epoch) {
                    android.util.Log.w("KansoCompanion", "App library load failed", error)
                    mutableState.update { it.copy(loadingApps = false, error = "Couldn't load apps. Try again.") }
                }
            } finally {
                if (generation == epoch) libraryClient.close()
            }
        }
    }

    private suspend fun decodeApps(result: JSONArray): List<TvApp> = withContext(Dispatchers.Default) {
        (0 until result.length()).map { result.getJSONObject(it) }.map {
            val id = it.getString("id")
            val decoded = artwork.decode(id, it.optString("icon"))
            TvApp(id, it.getString("name"), artwork = decoded,
                banner = artwork.decode("$id/banner", it.optString("banner")),
                accent = artwork.accent(decoded), category = it.optInt("category", -1))
        }
    }

    private fun handleFailure(error: Exception) {
        if (error is kotlinx.coroutines.CancellationException) return
        client.close()
        libraryJob?.cancel(); libraryClient.close()
        mutableState.update { it.copy(connected = false, busy = false, loadingApps = false,
            launchingId = null, activeAppId = null,
            error = if (error is RemoteError) error.message else "Couldn't reach the TV. Check the connection.") }
    }

    private fun notice(text: String) {
        noticeJob?.cancel()
        mutableState.update { it.copy(notice = text) }
        noticeJob = viewModelScope.launch { delay(1800); mutableState.update { it.copy(notice = null) } }
    }

    override fun onCleared() { discovery.stop(); client.close(); libraryClient.close(); commands.close() }

    companion object {
        val previewApps = listOf(
            TvApp("youtube", "YouTube"), TvApp("netflix", "Netflix"), TvApp("prime", "Prime Video"),
            TvApp("spotify", "Spotify"), TvApp("plex", "Plex"), TvApp("disney", "Disney+"),
            TvApp("apple", "Apple TV"), TvApp("vlc", "VLC"), TvApp("settings", "Settings"),
        )
    }
}

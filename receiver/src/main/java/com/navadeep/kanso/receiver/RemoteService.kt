package com.navadeep.kanso.receiver

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.Socket
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLServerSocket
import kotlin.concurrent.thread

data class ReceiverStatus(val running: Boolean = false, val code: String = "", val error: String? = null)

class RemoteService : AccessibilityService() {
    private var server: SSLServerSocket? = null
    private val clients = java.util.concurrent.ConcurrentHashMap.newKeySet<Socket>()
    private val workers = ThreadPoolExecutor(4, 4, 30, TimeUnit.SECONDS, ArrayBlockingQueue(4))
        .apply { allowCoreThreadTimeOut(true) }
    private var registration: NsdManager.RegistrationListener? = null
    private val main = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("remote-tokens", 0) }
    private val random = SecureRandom()
    private val pinLock = Any()
    private var pinExpires = 0L
    private var attempts = 0
    private var attemptWindow = 0L
    @Volatile private var appPackages: Set<String> = emptySet()
    private var foregroundPackage = ""

    override fun onServiceConnected() {
        android.util.Log.i("KansoReceiver", "Service connected")
        instance = this
        thread(name = "kanso-listener") {
            try {
                server = (serverTls().serverSocketFactory.createServerSocket(9876) as SSLServerSocket)
                    .apply { enabledProtocols = supportedProtocols.filter { it == "TLSv1.3" || it == "TLSv1.2" }.toTypedArray() }
                if (instance !== this) { server?.close(); return@thread }
                status.update { it.copy(running = true, error = null) }
                android.util.Log.i("KansoReceiver", "Listening")
                advertise()
                while (server?.isClosed == false) {
                    val socket = server?.accept() ?: break
                    socket.soTimeout = 12000
                    socket.tcpNoDelay = true
                    clients.add(socket)
                    try { workers.execute { serve(socket) } }
                    catch (_: java.util.concurrent.RejectedExecutionException) { clients.remove(socket); socket.close() }
                }
            } catch (error: Exception) {
                if (instance === this) {
                    android.util.Log.w("KansoReceiver", "Listener stopped", error)
                    status.update { it.copy(running = false, error = "Receiver couldn't start. Check network access.") }
                }
            }
        }
    }

    fun showPairingCode() = synchronized(pinLock) {
        val code = (100000 + random.nextInt(900000)).toString()
        pinExpires = android.os.SystemClock.elapsedRealtime() + 120000
        status.update { it.copy(code = code) }
    }

    fun hidePairingCode() = synchronized(pinLock) { pinExpires = 0; status.update { it.copy(code = "") } }
    fun forgetPhones() { prefs.edit().clear().apply(); clients.forEach { runCatching { it.close() } }; showPairingCode() }

    private fun advertise() {
        val nsd = getSystemService(NsdManager::class.java)
        val listener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = Unit
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) = Unit
        }
        registration = listener
        nsd.registerService(NsdServiceInfo().apply {
            serviceName = "Kanso · ${Build.MODEL}"
            serviceType = "_kanso._tcp."
            port = 9876
            setAttribute("version", "4")
        }, NsdManager.PROTOCOL_DNS_SD, listener)
    }

    private fun serve(socket: Socket) {
        try {
            socket.use {
                val reader = it.inputStream.bufferedReader()
                val writer = it.outputStream.bufferedWriter()
                while (!it.isClosed) {
                    // A phone request is at most a few hundred bytes; bound input before allocating JSON.
                    val line = StringBuilder()
                    while (true) {
                        val c = reader.read()
                        if (c == -1) return
                        if (c == 10) break
                        require(line.length < 8192)
                        line.append(c.toChar())
                    }
                    val result = try { dispatch(JSONObject(line.toString())) }
                        catch (error: Exception) { JSONObject().put("ok", false).put("error", error.message ?: "Command failed") }
                    writer.write(result.toString()); writer.newLine(); writer.flush()
                }
            }
        } catch (error: Exception) { android.util.Log.d("KansoReceiver", "Connection closed: ${error.javaClass.simpleName}: ${error.message}") }
        finally { clients.remove(socket); runCatching { socket.close() } }
    }

    private fun dispatch(request: JSONObject): JSONObject {
        val type = request.getString("type")
        if (type == "pair") return pair(request.optString("code"))
        val hash = digest(request.optString("token"))
        check(prefs.getStringSet("tokens", emptySet())!!.contains(hash)) { "Pair this phone again" }
        val response = JSONObject().put("ok", true)
        when (type) {
            "hello" -> {
                var active = ""
                onMain {
                    val candidate = rootInActiveWindow?.packageName?.toString().orEmpty()
                    // Volume overlays and the keyboard must not replace the app being controlled.
                    if (candidate in appPackages || candidate == "com.navadeep.kanso.tv") foregroundPackage = candidate
                    active = foregroundPackage
                    true
                }
                response.put("name", Build.MODEL).put("version", 4).put("activeApp", active)
                    .put("muted", getSystemService(AudioManager::class.java).isStreamMute(AudioManager.STREAM_MUSIC))
            }
            "apps" -> response.put("apps", apps())
            "command" -> {
                val key = request.getString("key")
                check(onMain {
                    val handled = navigate(key)
                    if (key.startsWith("volume_")) response.put("muted",
                        getSystemService(AudioManager::class.java).isStreamMute(AudioManager.STREAM_MUSIC))
                    handled
                }) { "This action isn't available on this screen" }
            }
            "launch" -> {
                val packageName = request.getString("id")
                val intent = packageManager.getLeanbackLaunchIntentForPackage(packageName)
                    ?: packageManager.getLaunchIntentForPackage(packageName)
                requireNotNull(intent) { "App is no longer installed" }
                check(onMain { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }) { "Couldn't open app" }
            }
            else -> error("Unknown command")
        }
        return response
    }

    private fun pair(code: String): JSONObject = synchronized(pinLock) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - attemptWindow > 60000) { attempts = 0; attemptWindow = now }
        check(attempts++ < 6) { "Wait a minute before trying again" }
        check(now < pinExpires && code.length == 6 && MessageDigest.isEqual(code.toByteArray(), status.value.code.toByteArray())) {
            "Code expired or incorrect. Check your TV."
        }
        val token = Base64.encodeToString(ByteArray(32).also(random::nextBytes), Base64.NO_WRAP or Base64.URL_SAFE)
        val tokens = prefs.getStringSet("tokens", emptySet())!!.toMutableSet()
        check(tokens.size < 8) { "Forget old phones on the TV first" }
        tokens.add(digest(token))
        prefs.edit().putStringSet("tokens", tokens).commit()
        hidePairingCode()
        JSONObject().put("ok", true).put("token", token)
    }

    @Suppress("DEPRECATION")
    private fun apps(): JSONArray {
        val apps = linkedMapOf<String, android.content.pm.ResolveInfo>()
        listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER).forEach { category ->
            packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0).forEach {
                if (it.activityInfo.packageName != packageName) apps.putIfAbsent(it.activityInfo.packageName, it)
            }
        }
        appPackages = apps.keys.toSet()
        return JSONArray().apply {
            apps.entries.sortedBy { it.value.loadLabel(packageManager).toString().lowercase() }.forEach { (id, info) ->
                val icon = runCatching {
                    val bitmap = info.loadIcon(packageManager).toBitmap(192, 192)
                    val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
                    Base64.encodeToString(bytes, Base64.NO_WRAP)
                }.getOrDefault("")
                val banner = runCatching {
                    val drawable = info.activityInfo.loadBanner(packageManager)
                        ?: info.activityInfo.applicationInfo.loadBanner(packageManager)
                    drawable?.let {
                        val bitmap = it.toBitmap(480, 270)
                        val bytes = ByteArrayOutputStream().also { output -> bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 95, output) }.toByteArray()
                        Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }.orEmpty()
                }.getOrDefault("")
                put(JSONObject().put("id", id).put("name", info.loadLabel(packageManager).toString())
                    .put("icon", icon).put("banner", banner)
                    .put("category", info.activityInfo.applicationInfo.category))
            }
        }
    }

    private fun navigate(key: String): Boolean {
        val action = when (key) {
            "up" -> GLOBAL_ACTION_DPAD_UP; "down" -> GLOBAL_ACTION_DPAD_DOWN
            "left" -> GLOBAL_ACTION_DPAD_LEFT; "right" -> GLOBAL_ACTION_DPAD_RIGHT
            "select" -> GLOBAL_ACTION_DPAD_CENTER; "back" -> GLOBAL_ACTION_BACK
            "home" -> {
                val kanso = packageManager.getLaunchIntentForPackage("com.navadeep.kanso.tv")
                if (kanso != null) { startActivity(kanso.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return true }
                GLOBAL_ACTION_HOME
            }
            "long_select" -> {
                val focused = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                return focused?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) == true
            }
            "volume_up", "volume_down", "volume_mute" -> {
                getSystemService(AudioManager::class.java).adjustStreamVolume(AudioManager.STREAM_MUSIC,
                    when (key) {
                        "volume_up" -> AudioManager.ADJUST_RAISE
                        "volume_mute" -> AudioManager.ADJUST_TOGGLE_MUTE
                        else -> AudioManager.ADJUST_LOWER
                    }, AudioManager.FLAG_SHOW_UI)
                return true
            }
            "channel_up", "channel_down" -> return changeChannel(key == "channel_up")
            "play_pause", "rewind", "forward", "previous", "next" -> {
                val audio = getSystemService(AudioManager::class.java)
                val code = when (key) {
                    "rewind" -> KeyEvent.KEYCODE_MEDIA_REWIND
                    "forward" -> KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
                    "previous" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
                    "next" -> KeyEvent.KEYCODE_MEDIA_NEXT
                    else -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                }
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
                return true
            }
            else -> return false
        }
        return performGlobalAction(action)
    }

    /** Use the live-TV app's explicit accessible channel controls, never guess with D-pad keys. */
    private fun changeChannel(up: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val labels = if (up) setOf("channel up", "next channel", "ch+", "ch +", "channel +")
            else setOf("channel down", "previous channel", "ch-", "ch -", "channel -")
        val pending = java.util.ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var visited = 0
        while (pending.isNotEmpty() && visited++ < 600) {
            val node = pending.removeFirst()
            val matches = listOf(node.text, node.contentDescription).any {
                it?.toString()?.trim()?.lowercase(java.util.Locale.ROOT) in labels
            }
            if (matches && node.isVisibleToUser && node.isEnabled) {
                var target: AccessibilityNodeInfo? = node
                repeat(3) {
                    if (target?.isClickable == true && target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
                    target = target?.parent
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let(pending::addLast)
        }
        return false
    }

    private fun onMain(action: () -> Boolean): Boolean {
        val done = CountDownLatch(1)
        var result = false
        main.post { try { result = runCatching(action).getOrDefault(false) } finally { done.countDown() } }
        return done.await(2, TimeUnit.SECONDS) && result
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() {
        android.util.Log.i("KansoReceiver", "Service destroyed")
        instance = null
        runCatching { server?.close() }
        clients.forEach { runCatching { it.close() } }
        registration?.let { runCatching { getSystemService(NsdManager::class.java).unregisterService(it) } }
        workers.shutdownNow()
        status.value = ReceiverStatus()
        super.onDestroy()
    }
    companion object {
        var instance: RemoteService? = null
            private set
        internal val status = MutableStateFlow(ReceiverStatus())
        val state = status.asStateFlow()
        private fun digest(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

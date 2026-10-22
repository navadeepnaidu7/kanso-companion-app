package com.navadeep.kanso.companion.data

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class TvDevice(val name: String, val host: String, val port: Int = 9876)
@androidx.compose.runtime.Immutable
data class TvApp(val id: String, val name: String, val icon: String = "",
    val artwork: androidx.compose.ui.graphics.ImageBitmap? = null,
    val banner: androidx.compose.ui.graphics.ImageBitmap? = null,
    val accent: Int = 0xFF78838F.toInt(),
    val category: Int = -1)
data class SavedTv(val device: TvDevice, val token: String, val certificate: String)

/** One ordered, persistent TLS connection. A new connection never replays a command. */
class RemoteClient {
    private val lock = Mutex()
    private val sessionLock = Any()
    private var generation = 0
    private var socket: SSLSocket? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var token = ""
    var fingerprint: String = ""
        private set

    suspend fun connect(device: TvDevice, certificate: String? = null) = withContext(Dispatchers.IO) {
        var peerFingerprint = ""
        val trust = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                require(chain.isNotEmpty()) { "TV certificate missing" }
                val found = MessageDigest.getInstance("SHA-256").digest(chain[0].encoded)
                    .joinToString("") { "%02x".format(it) }
                if (certificate != null && found != certificate) {
                    throw java.security.cert.CertificateException("TV identity changed. Pair again.")
                }
                peerFingerprint = found
            }
        }
        val context = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trust), SecureRandom())
        }
        val connection = context.socketFactory.createSocket() as SSLSocket
        val session = synchronized(sessionLock) { close(); socket = connection; generation }
        try {
            connection.connect(InetSocketAddress(device.host, device.port), 3000)
            connection.soTimeout = 3500
            connection.tcpNoDelay = true
            connection.startHandshake()
            currentCoroutineContext().ensureActive()
            synchronized(sessionLock) {
                check(session == generation) { "Connection cancelled" }
                fingerprint = peerFingerprint
                reader = connection.inputStream.bufferedReader()
                writer = connection.outputStream.bufferedWriter()
            }
        } catch (error: Exception) {
            synchronized(sessionLock) { if (session == generation) close() else runCatching { connection.close() } }
            throw error
        }
    }

    fun authorize(value: String) { token = value }

    suspend fun request(type: String, body: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
        lock.withLock {
            val connection = socket ?: error("TV disconnected")
            val output = writer ?: error("TV disconnected")
            val input = reader ?: error("TV disconnected")
            // Artwork is a larger response than a remote key. Keep key failures fast while
            // allowing a cold TV package scan and transfer to finish on ordinary Wi-Fi.
            connection.soTimeout = if (type == "apps") 15000 else 3500
            try {
            body.put("type", type)
            if (token.isNotEmpty()) body.put("token", token)
            output.write(body.toString())
            output.newLine()
            output.flush()
            val response = JSONObject(input.readLine() ?: error("TV disconnected"))
            if (!response.optBoolean("ok")) throw RemoteError(response.optString("error", "Try again"))
            response
            } catch (error: java.io.IOException) {
                synchronized(sessionLock) { if (socket === connection) close() }
                throw error
            } finally {
                if (!connection.isClosed) runCatching { connection.soTimeout = 3500 }
            }
        }
    }

    fun close() = synchronized(sessionLock) {
        generation++
        runCatching { socket?.close() }
        socket = null; reader = null; writer = null; token = ""
    }
}

class RemoteError(message: String) : Exception(message)

/** Discovery is scoped to the connection sheet, and resolution requests are serialized. */
@Suppress("DEPRECATION")
class TvDiscovery(context: Context, private val onDevices: (List<TvDevice>) -> Unit) {
    private val nsd = context.getSystemService(NsdManager::class.java)
    private val devices = linkedMapOf<String, TvDevice>()
    private val pending = ArrayDeque<NsdServiceInfo>()
    private var resolving = false
    private var running = false
    private val listener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(type: String) = Unit
        override fun onDiscoveryStopped(type: String) = Unit
        override fun onStartDiscoveryFailed(type: String, code: Int) { stop() }
        override fun onStopDiscoveryFailed(type: String, code: Int) = Unit
        override fun onServiceFound(service: NsdServiceInfo) {
            synchronized(this@TvDiscovery) { pending.addLast(service); resolveNext() }
        }
        override fun onServiceLost(service: NsdServiceInfo) {
            synchronized(this@TvDiscovery) { devices.remove(service.serviceName); onDevices(devices.values.toList()) }
        }
    }
    @Synchronized private fun resolveNext() {
        if (!running || resolving || pending.isEmpty()) return
        resolving = true
        val service = pending.removeFirst()
        nsd.resolveService(service, object : NsdManager.ResolveListener {
            override fun onResolveFailed(info: NsdServiceInfo, code: Int) = finished()
            override fun onServiceResolved(info: NsdServiceInfo) {
                synchronized(this@TvDiscovery) {
                    if (running) info.host?.hostAddress?.let { host ->
                        devices[info.serviceName] = TvDevice(info.serviceName.removePrefix("Kanso · "), host, info.port)
                        onDevices(devices.values.toList())
                    }
                }
                finished()
            }
            private fun finished() {
                synchronized(this@TvDiscovery) { resolving = false; resolveNext() }
            }
        })
    }
    @Synchronized fun start() {
        if (running) return
        running = true
        runCatching { nsd.discoverServices("_kanso._tcp.", NsdManager.PROTOCOL_DNS_SD, listener) }
            .onFailure { running = false }
    }
    @Synchronized fun stop() {
        if (running) runCatching { nsd.stopServiceDiscovery(listener) }
        running = false
        pending.clear()
        devices.clear()
    }
}

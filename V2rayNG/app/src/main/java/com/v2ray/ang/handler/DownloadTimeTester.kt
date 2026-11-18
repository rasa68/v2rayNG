package com.v2ray.ang.handler

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.util.JsonUtil
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import libv2ray.CoreCallbackHandler
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * "Download time test": measures how long a config needs to download a fixed amount of data.
 *
 * Each config gets its own short-lived core instance with a local SOCKS inbound on a free port. The
 * test downloads a set number of bytes from a public endpoint through that inbound and reports the
 * whole time (connection set-up included) in milliseconds. It reports -1 when the data did not arrive
 * within the allowed time or the download failed in any other way.
 */
object DownloadTimeTester {
    private const val INBOUND_TAG = "download-test-in"
    private const val READ_BUFFER_SIZE = 16 * 1024

    /**
     * @return the time in milliseconds, or -1 on failure or timeout.
     */
    suspend fun measure(context: Context, guid: String): Long {
        val sizeBytes = SettingsManager.getDownloadTestSizeKb() * 1024L
        val timeoutMillis = SettingsManager.getDownloadTestTimeoutSeconds() * 1000L
        val job = currentCoroutineContext()[Job]

        // Same outbound-only config the delay test uses (server address pinned), plus a local inbound.
        val configResult = CoreConfigManager.getV2rayConfig4Speedtest(context, guid)
        if (!configResult.status) return FAILURE
        val port = findFreePort() ?: return FAILURE
        val config = withLocalInbound(configResult.content, port) ?: return FAILURE

        val controller = CoreNativeManager.newCoreController(NoopCoreCallback)
        return try {
            controller.startLoop(config, 0)
            download(port, sizeBytes, timeoutMillis) { job?.isActive != false }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            LogUtil.w(AppConfig.TAG, "Download time test failed for $guid: ${e.message}")
            FAILURE
        } finally {
            runCatching { controller.stopLoop() }
        }
    }

    private fun download(port: Int, sizeBytes: Long, timeoutMillis: Long, isActive: () -> Boolean): Long {
        val client = OkHttpClient.Builder()
            .proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)))
            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            // Covers the whole call, body included: this is the "maximum time to wait".
            .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()
        val request = Request.Builder()
            .url(String.format(AppConfig.DOWNLOAD_TEST_URL, sizeBytes))
            .header("Cache-Control", "no-cache")
            .build()

        val start = System.nanoTime()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return FAILURE
                val body = response.body ?: return FAILURE
                var received = 0L
                val buffer = ByteArray(READ_BUFFER_SIZE)
                body.byteStream().use { input ->
                    while (received < sizeBytes) {
                        if (!isActive()) return FAILURE
                        val read = input.read(buffer)
                        if (read < 0) break
                        received += read
                    }
                }
                if (received < sizeBytes) return FAILURE
            }
            ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1L)
        } catch (e: Exception) {
            FAILURE
        }
    }

    /** Replaces the config's inbounds with a single local SOCKS inbound on [port]. */
    private fun withLocalInbound(configJson: String, port: Int): String? {
        val root = JsonUtil.parseString(configJson) ?: return null
        val inbound = JsonObject().apply {
            addProperty("tag", INBOUND_TAG)
            addProperty("listen", "127.0.0.1")
            addProperty("port", port)
            addProperty("protocol", "socks")
            add("settings", JsonObject().apply {
                addProperty("auth", "noauth")
                addProperty("udp", false)
            })
        }
        root.add("inbounds", JsonArray().apply { add(inbound) })
        return root.toString()
    }

    private fun findFreePort(): Int? = try {
        ServerSocket(0).use { it.localPort }
    } catch (e: Exception) {
        null
    }

    private const val FAILURE = -1L

    private object NoopCoreCallback : CoreCallbackHandler {
        override fun startup(): Long = 0
        override fun shutdown(): Long = 0
        override fun onEmitStatus(l: Long, s: String?): Long = 0
    }
}

package com.straylabs.hound.client

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Read-only discovery client. The APK never receives the Worker write token. */
class DiscoveryClient(
    private val discoveryUrl: String,
    private val deviceId: String,
    private val readKey: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    val enabled: Boolean
        get() = discoveryUrl.isNotBlank() && deviceId.isNotBlank() && readKey.isNotBlank()

    suspend fun currentUrl(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!enabled) throw IOException("Discovery is not configured")
            val base = discoveryUrl.toHttpUrlOrNull()
                ?: throw IOException("Invalid discovery URL")
            val url = base.newBuilder()
                .addQueryParameter("device", deviceId)
                .addQueryParameter("key", readKey)
                .build()
            val request = Request.Builder().url(url).get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Discovery HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val value = JSONObject(body).optString("url").trim().trimEnd('/')
                if (!value.startsWith("https://") && !value.startsWith("http://")) {
                    throw IOException("Discovery returned no valid server URL")
                }
                value
            }
        }
    }
}

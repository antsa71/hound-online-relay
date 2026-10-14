package com.straylabs.hound.relay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Direct Android-to-Cloudflare relay API. No Termux/cloudflared is required.
 * The pairing token is the same on Server and Client and is sent as a header.
 */
class RelayApi(
    private val workerBaseUrl: String,
    private val pairingToken: String
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val base = workerBaseUrl.trimEnd('/')
    private val jsonType = "application/json; charset=utf-8".toMediaType()

    private fun Request.Builder.auth() = header("X-Pairing-Token", pairingToken)

    suspend fun registerServer(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        requestJson("/api/device/register", "POST", JSONObject()
            .put("device_id", deviceId)
            .put("device_label", "Hound Server"))
    }

    suspend fun uploadManifest(items: JSONArray, page: Int = 0, pages: Int = 1): Result<Unit> =
        withContext(Dispatchers.IO) {
            requestJson("/api/manifest", "POST", JSONObject()
                .put("page", page)
                .put("pages", pages)
                .put("total", items.length())
                .put("root_name", "/storage/emulated/0")
                .put("items", items))
        }

    suspend fun nextCommand(): Result<JSONObject?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$base/api/commands").auth().get().build()
            http.newCall(request).execute().use { response ->
                if (response.code == 204) return@use null
                if (!response.isSuccessful) throw IOException("Relay HTTP ${response.code}")
                JSONObject(response.body?.string().orEmpty())
            }
        }
    }

    suspend fun startDownload(path: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().put("path", path).toString().toRequestBody(jsonType)
            val request = Request.Builder().url("$base/api/download/start").auth().post(body).build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Relay HTTP ${response.code}")
                JSONObject(response.body?.string().orEmpty()).getString("id")
            }
        }
    }

    suspend fun putChunk(transferId: String, seq: Int, bytes: ByteArray): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$base/api/download/chunk?transfer=$transferId&seq=$seq")
                    .auth().post(bytes.toRequestBody("application/octet-stream".toMediaType())).build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful && response.code != 204) throw IOException("Relay HTTP ${response.code}")
                }
            }
        }

    suspend fun getChunk(transferId: String, seq: Int): Result<ByteArray?> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$base/api/download/chunk?transfer=$transferId&seq=$seq").auth().get().build()
            http.newCall(request).execute().use { response ->
                if (response.code == 204) return@use null
                if (!response.isSuccessful) throw IOException("Relay HTTP ${response.code}")
                response.body?.bytes()
            }
        }
    }

    suspend fun finishDownload(transferId: String): Result<Unit> = withContext(Dispatchers.IO) {
        requestEmpty("/api/download/finish?transfer=$transferId", "POST")
    }

    suspend fun completeDownload(transferId: String): Result<Unit> = withContext(Dispatchers.IO) {
        requestEmpty("/api/download/complete?transfer=$transferId", "POST")
    }

    suspend fun manifest(): Result<JSONObject> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url("$base/api/manifest").auth().get().build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Relay HTTP ${response.code}")
                JSONObject(response.body?.string().orEmpty())
            }
        }
    }

    private fun requestJson(path: String, method: String, json: JSONObject): Result<Unit> = runCatching {
        val body = json.toString().toRequestBody(jsonType)
        val builder = Request.Builder().url(base + path).auth()
        val request = if (method == "POST") builder.post(body).build() else builder.put(body).build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Relay HTTP ${response.code}")
        }
    }

    private fun requestEmpty(path: String, method: String): Result<Unit> = runCatching {
        val builder = Request.Builder().url(base + path).auth()
        val request = if (method == "POST") builder.post(ByteArray(0).toRequestBody(null)).build()
        else builder.get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code != 204) throw IOException("Relay HTTP ${response.code}")
        }
    }
}

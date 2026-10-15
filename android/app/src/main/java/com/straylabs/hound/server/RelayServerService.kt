package com.straylabs.hound.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.straylabs.hound.R
import com.straylabs.hound.relay.RelayApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream

/** Relay lives in the APK; no Termux or cloudflared is needed. */
class RelayServerService : Service() {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL).orEmpty()
        val token = intent?.getStringExtra(EXTRA_TOKEN).orEmpty()
        val root = File(intent?.getStringExtra(EXTRA_ROOT) ?: "/storage/emulated/0")
        startForeground(NOTIFICATION_ID, notification())
        job?.cancel()
        if (url.isNotBlank() && token.isNotBlank() && root.isDirectory) {
            job = scope.launch { serve(RelayApi(url, token), root) }
        }
        return START_STICKY
    }

    private suspend fun serve(api: RelayApi, root: File) {
        val registration = api.registerServer(DEVICE_ID)
        updateNotification(if (registration.isSuccess) "Connected" else "Disconnected: ${registration.exceptionOrNull()?.message ?: "check Worker URL/token"}")
        while (scope.isActive) {
            val manifestResult = try { publishManifest(api, root) } catch (e: Exception) { Result.failure(e) }
            if (manifestResult.isFailure) {
                updateNotification("Disconnected: ${manifestResult.exceptionOrNull()?.message ?: "check Internet/Worker"}")
                delay(5000)
                continue
            }
            api.nextCommand().onSuccess { command ->
                if (command != null) {
                    updateNotification("Sending requested file: ${command.optString("path")}")
                    streamSelectedFile(api, root, command.optString("id"), command.optString("path"))
                    updateNotification("Connected")
                } else updateNotification("waiting")
            }.onFailure { e -> updateNotification("Disconnected: ${e.message ?: "unknown error"}") }
            delay(2500)
        }
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification(message))
    }

    private suspend fun publishManifest(api: RelayApi, root: File): Result<Unit> {
        val items = JSONArray()
        fun add(directory: File, prefix: String) {
            directory.listFiles()?.sortedBy { it.name.lowercase() }?.forEach { file ->
                val path = if (prefix.isBlank()) file.name else "$prefix/${file.name}"
            items.put(org.json.JSONObject()
                .put("name", file.name)
                .put("path", path)
                .put("size", if (file.isFile) file.length() else 0)
                .put("isDirectory", file.isDirectory)
                .put("modified", file.lastModified()))
                if (file.isDirectory) add(file, path)
            }
        }
        add(root, "")
        return api.uploadManifest(items)
    }

    private suspend fun streamSelectedFile(api: RelayApi, root: File, id: String, relativePath: String) {
        if (id.isBlank() || relativePath.isBlank()) return
        val file = File(root, relativePath).canonicalFile
        if (!file.path.startsWith(root.canonicalFile.path + File.separator) || !file.isFile) return
        FileInputStream(file).use { input ->
            val buffer = ByteArray(CHUNK_SIZE)
            var seq = 0
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                val bytes = if (count == buffer.size) buffer.copyOf() else buffer.copyOf(count)
                if (api.putChunk(id, seq, bytes).isFailure) return
                seq++
            }
        }
        api.finishDownload(id)
    }

    private fun notification(message: String = "Connecting"): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Background service", NotificationManager.IMPORTANCE_MIN)
                    .apply {
                        description = "Silent background connection status"
                        setShowBadge(false)
                        lockscreenVisibility = android.app.Notification.VISIBILITY_SECRET
                    }
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ConvertA online")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        job?.cancel()
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        stopForeground(true)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "convertA_background_v2"
        private const val NOTIFICATION_ID = 7
        private const val CHUNK_SIZE = 1024 * 1024
        private const val DEVICE_ID = "hound-server-1"
        const val EXTRA_URL = "relay_url"
        const val EXTRA_TOKEN = "pairing_token"
        const val EXTRA_ROOT = "root_path"

        fun stop(context: Context) {
            context.stopService(Intent(context, RelayServerService::class.java))
        }

        fun start(context: Context, url: String, token: String, root: String) {
            val intent = Intent(context, RelayServerService::class.java)
                .putExtra(EXTRA_URL, url).putExtra(EXTRA_TOKEN, token).putExtra(EXTRA_ROOT, root)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }
    }
}

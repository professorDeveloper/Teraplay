// Add this file: ForegroundDownloadService.kt
package com.saikou.teraplay.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.saikou.teraplay.R
import java.io.File
import java.net.URL
import java.util.concurrent.Executors

class ForegroundDownloadService : Service() {
    companion object {
        const val CHANNEL_ID = "download_channel"
        const val ACTION_START = "ACTION_START_DOWNLOAD"
        const val EXTRA_URL = "extra_url"
        const val EXTRA_FILE_NAME = "extra_file_name"

        fun start(context: Context, url: String, fileName: String) {
            val intent = Intent(context, ForegroundDownloadService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_FILE_NAME, fileName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }
    }

    private val executor = Executors.newSingleThreadExecutor()

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            val url = intent.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
            val fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "download.file"
            createNotificationChannel()
            startForeground(1, buildNotification(0, fileName))
            executor.execute {
                try {
                    val connection = URL(url).openConnection()
                    val input = connection.getInputStream()
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                    val output = file.outputStream()
                    val total = connection.contentLength
                    val buffer = ByteArray(4096)
                    var downloaded = 0
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = ((downloaded * 100f) / total).toInt()
                        updateNotification(progress, fileName)
                    }
                    input.close()
                    output.close()
                    stopSelf()
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Download Progress", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(progress: Int, fileName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Downloading: $fileName")
            .setContentText("$progress% completed")
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(progress: Int, fileName: String) {
        val notification = buildNotification(progress, fileName)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(1, notification)
    }
}

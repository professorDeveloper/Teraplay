package com.saikou.teraplay.broadcast

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saikou.teraplay.data.models.DownloadStatus
import com.saikou.teraplay.presentation.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DownloadReceiver : BroadcastReceiver(), KoinComponent {
    private val viewModel: HomeViewModel by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val progressJobs = mutableMapOf<Long, Job>()

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    progressJobs[downloadId]?.cancel()
                    progressJobs.remove(downloadId)
                    updateDownloadStatus(context, downloadId)
                }
            }
        }
    }

    fun startProgressMonitoring(context: Context, downloadId: Long) {
        if (progressJobs[downloadId] != null) return
        val job = scope.launch {
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            while (true) {
                updateDownloadStatus(context, downloadId)
                delay(500)
            }
        }
        progressJobs[downloadId] = job
    }

    fun stopMonitoring(downloadId: Long) {
        progressJobs[downloadId]?.cancel()
        progressJobs.remove(downloadId)
    }

    private fun updateDownloadStatus(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val status =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal =
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val progress =
                    if (bytesTotal > 0) ((bytesDownloaded * 100) / bytesTotal).toInt() else 0
                val downloadStatus = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> DownloadStatus.COMPLETED
                    DownloadManager.STATUS_FAILED -> DownloadStatus.FAILED
                    DownloadManager.STATUS_PAUSED -> DownloadStatus.PAUSED
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> DownloadStatus.DOWNLOADING
                    else -> DownloadStatus.IDLE
                }
                if (downloadStatus != DownloadStatus.DOWNLOADING) {
                    stopMonitoring(downloadId)
                }
            }
        }
    }
}
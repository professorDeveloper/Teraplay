package com.saikou.teraplay.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.saikou.teraplay.download.DownloadService
import com.saikou.teraplay.presentation.home.HomeViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class DownloadReceiver : BroadcastReceiver(), KoinComponent {
    private val viewModel: HomeViewModel by inject()
    private val TAG = "DownloadReceiver"

    companion object {
        const val ACTION_CANCEL_DOWNLOAD = "ACTION_CANCEL_DOWNLOAD"
        const val ACTION_PAUSE_DOWNLOAD = "ACTION_PAUSE_DOWNLOAD"
        const val ACTION_RESUME_DOWNLOAD = "ACTION_RESUME_DOWNLOAD"
        const val EXTRA_DOWNLOAD_ID = "downloadId"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Received broadcast with action: ${intent?.action}")

        if (context == null) {
            Log.e(TAG, "Context is null, cannot process broadcast")
            return
        }

        val downloadId = intent?.getLongExtra(EXTRA_DOWNLOAD_ID, -1L) ?: -1L
        if (downloadId == -1L) {
            Log.w(TAG, "Invalid downloadId: $downloadId")
            return
        }

        when (intent?.action) {
            ACTION_CANCEL_DOWNLOAD -> {
                Log.d(TAG, "Cancelling download with ID: $downloadId")
                viewModel.cancelDownload(context, downloadId)
                val serviceIntent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_CANCEL_DOWNLOAD
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                }
                context.startService(serviceIntent)
            }
            ACTION_PAUSE_DOWNLOAD -> {
                Log.d(TAG, "Pausing download with ID: $downloadId")
                val serviceIntent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_PAUSE_DOWNLOAD
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                }
                context.startService(serviceIntent)
            }
            ACTION_RESUME_DOWNLOAD -> {
                Log.d(TAG, "Resuming download with ID: $downloadId")
                val serviceIntent = Intent(context, DownloadService::class.java).apply {
                    action = ACTION_RESUME_DOWNLOAD
                    putExtra(EXTRA_DOWNLOAD_ID, downloadId)
                }
                context.startService(serviceIntent)
            }
            else -> {
                Log.w(TAG, "Unknown action: ${intent?.action}")
            }
        }
    }
}
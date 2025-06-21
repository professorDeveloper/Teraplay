package com.saikou.teraplay.presentation.home

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saikou.teraplay.R
import com.saikou.teraplay.broadcast.DownloadReceiver
import com.saikou.teraplay.data.local.room.database.AppDatabase
import com.saikou.teraplay.data.local.room.entity.DownloadEntity
import com.saikou.teraplay.data.models.DownloadItem
import com.saikou.teraplay.data.models.DownloadResponse
import com.saikou.teraplay.data.models.DownloadStatus
import com.saikou.teraplay.domain.repository.DownloadRepository
import com.saikou.teraplay.utils.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class HomeViewModel(private val repo: DownloadRepository) : ViewModel(), KoinComponent {
    private val database: AppDatabase by inject()

    private val _searchResponse = MutableLiveData<UiState<DownloadResponse>>(UiState.Idle)
    val searchResponse: LiveData<UiState<DownloadResponse>> get() = _searchResponse

    private val _downloads = MutableLiveData<List<DownloadItem>>(emptyList())
    val downloads: LiveData<List<DownloadItem>> get() = _downloads

    private val _downloadProgress = MutableLiveData<Triple<Long?, Int, DownloadStatus>>(Triple(null, 0, DownloadStatus.IDLE))
    val downloadProgress: LiveData<Triple<Long?, Int, DownloadStatus>> get() = _downloadProgress

    fun trySearch(query: String) {
        viewModelScope.launch {
            _searchResponse.postValue(UiState.Loading)
            val result = repo.fetchDownloadInfo(query)
            _searchResponse.postValue(
                if (result.isSuccess) {
                    UiState.Success(result.getOrNull()!!)
                } else {
                    UiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
                }
            )
        }
    }

    fun startDownload(context: Context, response: DownloadResponse) {
        viewModelScope.launch {
            val downloadId = UUID.randomUUID().hashCode().toLong()
            val downloadItem = DownloadItem(response = response, downloadId = downloadId)
            _downloads.postValue(_downloads.value.orEmpty() + downloadItem)
            downloadFile(context, downloadItem)
            saveDownloadState(context, downloadItem)
        }
    }

    fun cancelDownload(context: Context, downloadId: Long) {
        val currentItem = _downloads.value.orEmpty().firstOrNull { it.downloadId == downloadId } ?: return
        updateDownloadStatus(context, downloadId, DownloadStatus.CANCELLED, currentItem.progress)
        viewModelScope.launch(Dispatchers.IO) {
            database.downloadDao().insert(
                DownloadEntity(
                    id = currentItem.id,
                    fileName = currentItem.response.fileName,
                    status = DownloadStatus.CANCELLED.name,
                    progress = currentItem.progress,
                    downloadId = currentItem.downloadId
                )
            )
        }
    }

    private fun downloadFile(context: Context, item: DownloadItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(item.response.directLink).build()
                val downloadDir = File(context.getExternalFilesDir(null), "TeraPlay")
                if (!downloadDir.exists()) downloadDir.mkdirs()
                val file = File(downloadDir, item.response.fileName)

                item.downloadId?.let { downloadId ->
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("Download failed")
                        val totalBytes = item.response.sizeBytes
                        var downloadedBytes = 0L

                        response.body?.byteStream()?.use { input ->
                            FileOutputStream(file).use { output ->
                                val buffer = ByteArray(8 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    downloadedBytes += read
                                    val progress = if (totalBytes > 0) (downloadedBytes * 100 / totalBytes).toInt() else 0
                                    updateDownloadStatus(context, downloadId, DownloadStatus.DOWNLOADING, progress)
                                    saveDownloadState(context, item.copy(progress = progress, status = DownloadStatus.DOWNLOADING))
                                }
                            }
                        }
                        updateDownloadStatus(context, downloadId, DownloadStatus.COMPLETED, 100)
                        saveDownloadState(context, item.copy(status = DownloadStatus.COMPLETED, progress = 100))
                    }
                }
            } catch (e: Exception) {
                item.downloadId?.let { downloadId ->
                    updateDownloadStatus(context, downloadId, DownloadStatus.FAILED, item.progress)
                    saveDownloadState(context, item.copy(status = DownloadStatus.FAILED))
                }
            }
        }
    }

    fun updateDownloadStatus(
        context: Context,
        downloadId: Long,
        status: DownloadStatus,
        progress: Int
    ) {
        _downloadProgress.postValue(Triple(downloadId, progress, status))
        _downloads.postValue(_downloads.value.orEmpty().map {
            if (it.downloadId == downloadId) it.copy(status = status, progress = progress)
            else it
        })
        val item = _downloads.value?.firstOrNull { it.downloadId == downloadId }
        item?.let { updateNotification(context, it, progress) }
    }

    private fun updateNotification(context: Context, item: DownloadItem, progress: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "download_channel"
        val notifId = item.downloadId?.toInt() ?: item.id.hashCode()

        val smallView = RemoteViews(context.packageName, R.layout.notification_collapsed).apply {
            setTextViewText(R.id.tvTitle, item.response.fileName)
            setTextViewText(R.id.tvProgressText, "$progress%")
            setProgressBar(R.id.progressBar, 100, progress, false)
        }

        val bigView = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setTextViewText(R.id.tvTitleBig, item.response.fileName)
            setTextViewText(R.id.tvProgressTextBig, "$progress% completed")
            setProgressBar(R.id.progressBarBig, 100, progress, false)
            val cancelIntent = Intent("ACTION_CANCEL_DOWNLOAD").apply {
                putExtra("downloadId", item.downloadId)
            }
            val pCancel = PendingIntent.getBroadcast(
                context,
                notifId,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            setOnClickPendingIntent(R.id.btnCancel, pCancel)
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_download)
            .setCustomContentView(smallView)
            .setCustomBigContentView(bigView)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setOngoing(item.status == DownloadStatus.DOWNLOADING)



        manager.notify(notifId, builder.build())
    }

    private suspend fun saveDownloadState(context: Context, item: DownloadItem) {
        withContext(Dispatchers.IO) {
            database.downloadDao().insert(
                DownloadEntity(
                    id = item.id,
                    fileName = item.response.fileName,
                    status = item.status.name,
                    progress = item.progress,
                    downloadId = item.downloadId
                )
            )
        }
    }

    fun loadDownloads(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloads = database.downloadDao().getAllDownloads().map {
                DownloadItem(
                    id = it.id,
                    response = DownloadResponse(it.fileName, "", "", it.fileName, 0),
                    status = DownloadStatus.valueOf(it.status),
                    progress = it.progress,
                    downloadId = it.downloadId
                )
            }
            _downloads.postValue(downloads)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
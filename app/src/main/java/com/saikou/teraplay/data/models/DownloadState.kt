package com.saikou.teraplay.data.models

import java.io.Serializable
import java.util.UUID


data class DownloadItem(
    val id: String = UUID.randomUUID().toString(),
    val response: DownloadResponse,
    var status: DownloadStatus = DownloadStatus.IDLE,
    var progress: Int = 0,
    var downloadId: Long? = null
) : Serializable

enum class DownloadStatus {
    IDLE, DOWNLOADING, COMPLETED, FAILED, PAUSED, CANCELLED
}

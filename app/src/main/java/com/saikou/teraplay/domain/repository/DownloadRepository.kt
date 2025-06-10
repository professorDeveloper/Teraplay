// domain/repository/DownloadRepository.kt
package com.saikou.teraplay.domain.repository

import com.saikou.teraplay.data.models.DownloadResponse


interface DownloadRepository {
    suspend fun fetchDownloadInfo(url: String): Result<DownloadResponse>
}

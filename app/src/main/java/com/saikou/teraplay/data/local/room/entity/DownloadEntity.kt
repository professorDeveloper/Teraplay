package com.saikou.teraplay.data.local.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val status: String,
    val progress: Int,
    val downloadId: Long?
)
package com.saikou.teraplay.data.local.room.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.saikou.teraplay.data.local.room.dao.DownloadDao
import com.saikou.teraplay.data.local.room.entity.DownloadEntity

@Database(entities = [DownloadEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao
}
package com.example.yapenotifier.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CapturedEventEntity::class], version = 1, exportSchema = false)
abstract class YapeDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}

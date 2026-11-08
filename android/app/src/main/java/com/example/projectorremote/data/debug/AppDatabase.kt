package com.example.projectorremote.data.debug

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Database lokal di HP (Room/SQLite) bernama "projector_remote_db", berisi tabel debug_log.
// PENTING: setiap kali kolom di DebugLogEntity diubah, naikkan angka "version" ini
// (misal 1 -> 2) dan tambahkan .fallbackToDestructiveMigration() sebelum .build().
@Database(entities = [DebugLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun debugLogDao(): DebugLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "projector_remote_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

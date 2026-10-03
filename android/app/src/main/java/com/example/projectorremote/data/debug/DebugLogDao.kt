package com.example.projectorremote.data.debug

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DebugLogDao {

    @Insert
    suspend fun insert(log: DebugLogEntity)

    @Query("SELECT * FROM debug_log ORDER BY timestamp DESC")
    fun getAllLive(): LiveData<List<DebugLogEntity>>

    @Query("SELECT * FROM debug_log ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DebugLogEntity>

    @Query("DELETE FROM debug_log")
    suspend fun clearAll()
}

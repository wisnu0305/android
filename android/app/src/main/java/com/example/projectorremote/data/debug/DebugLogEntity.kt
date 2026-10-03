package com.example.projectorremote.data.debug

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas log untuk menu debugging / data penelitian.
 * Menyimpan: waktu, ucapan asli, hasil NLP, perintah yang dikirim, respons ESP32, status.
 */
@Entity(tableName = "debug_log")
data class DebugLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val rawSpeech: String,
    val normalizedText: String,
    val matchedCommand: String,
    val esp32Code: String,
    val requestUrl: String?,
    val responseBody: String?,
    val success: Boolean,
    val errorMessage: String?
)

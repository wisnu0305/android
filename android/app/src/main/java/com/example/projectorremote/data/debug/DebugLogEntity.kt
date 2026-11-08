package com.example.projectorremote.data.debug

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * TABEL DATA PENELITIAN (tab Debug).
 *
 * Bahasa sederhananya: setiap kali pengguna bicara, satu baris dicatat di tabel ini.
 * Dari tabel ini bisa dihitung akurasi: berapa ucapan yang dikenali, berapa yang
 * berhasil terkirim, dan berapa yang gagal.
 *
 * Kolom-kolomnya:
 *   timestamp      : kapan perintah diucapkan
 *   rawSpeech      : teks asli hasil Speech-to-Text
 *   normalizedText : teks setelah dirapikan NLP (huruf kecil, spasi rapi)
 *   matchedCommand : perintah yang dikenali (atau "Tidak dikenali")
 *   esp32Code      : kode yang dikirim, misal "PON"
 *   requestUrl     : alamat lengkap yang dibuka ke ESP32
 *   responseBody   : balasan dari ESP32 (termasuk balasan Uno)
 *   success        : berhasil terkirim atau tidak
 *   errorMessage   : alasan gagal, jika ada
 *
 * >>> TEMPAT MENAMBAH KOLOM BARU DI TABEL DEBUG <<<
 *   1. Tambah field di sini.
 *   2. Naikkan "version" di AppDatabase.kt (wajib, kalau tidak aplikasi crash).
 *   3. Isi nilainya di HomeViewModel.logToDebug().
 *   4. Tampilkan di res/layout/item_debug_log.xml dan ui/debug/DebugLogAdapter.kt.
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

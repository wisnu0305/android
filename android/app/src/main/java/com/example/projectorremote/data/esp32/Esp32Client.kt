package com.example.projectorremote.data.esp32

import com.example.projectorremote.data.Command
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * ALUR 4 — "KURIR" YANG MENGANTAR PERINTAH DARI HP KE ESP32 LEWAT WIFI.
 *
 * Bahasa sederhananya: HP "membuka alamat web" milik ESP32, sama seperti
 * browser membuka sebuah halaman. Alamat yang dibuka itulah API-nya:
 *   http://<ip-esp32>/command?cmd=PON          -> perintah standar (kode pendek)
 *   http://<ip-esp32>/commandRaw?raw=*pow=on#  -> perintah mentah dari profil Settings
 *   http://<ip-esp32>/setCustom?...            -> kirim profil (di ESP32 hanya dibalas OK)
 *
 * Library: OkHttp (permintaan HTTP GET) + Kotlin Coroutine (supaya layar tidak macet
 * selama menunggu balasan).
 */
class Esp32Client(private val baseUrl: String) {

    // Batas tunggu 5 detik. Jika ESP32 mati atau beda jaringan WiFi, aplikasi
    // menyerah setelah 5 detik dan menampilkan pesan "Gagal: ..." di layar.
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    /**
     * Mengirim perintah ke ESP32.
     * URL contoh: http://192.168.1.100/command?cmd=PON
     * @return Result dengan body response atau error
     */
    suspend fun sendCommand(command: Command): Result<String> = withContext(Dispatchers.IO) {
        if (command == Command.UNKNOWN) {
            return@withContext Result.failure(IllegalArgumentException("Perintah tidak valid"))
        }
        val url = buildUrl(command.esp32Code)
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                response.body?.string() ?: "OK"
            }
        }
    }

    private fun buildUrl(cmd: String): String {
        val base = baseUrl.removeSuffix("/")
        return if (base.contains("?")) "$base&cmd=$cmd" else "$base/command?cmd=$cmd"
    }

    /**
     * Cek koneksi ke ESP32 (ping/health).
     */
    suspend fun checkConnection(): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = baseUrl.removeSuffix("/") + "/"
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    /**
     * Mengirim custom command string langsung ke ESP32 (untuk diteruskan ke Arduino Uno).
     * Contoh: sendRawCommand("*sour=hdmi#") akan mengirim "*sour=hdmi#" langsung ke proyektor.
     */
    suspend fun sendRawCommand(rawCommand: String): Result<String> = withContext(Dispatchers.IO) {
        val base = baseUrl.removeSuffix("/")
        val url = "$base/commandRaw?raw=${URLEncoder.encode(rawCommand, "UTF-8")}"
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                response.body?.string() ?: "OK"
            }
        }
    }

    /**
     * Mengirim konfigurasi profil proyektor (baudrate + command) ke ESP32.
     * Semua profil sekarang user-defined (dibuat dari aplikasi Android).
     */
    suspend fun setCustomProfile(
        baud: Int,
        cmdPon: String,
        cmdPoff: String,
        cmdVup: String,
        cmdVdown: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val base = baseUrl.removeSuffix("/")
        fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
        val url = "$base/setCustom?baud=$baud" +
                "&pon=${enc(cmdPon)}" +
                "&poff=${enc(cmdPoff)}" +
                "&vup=${enc(cmdVup)}" +
                "&vdown=${enc(cmdVdown)}"
        val request = Request.Builder().url(url).get().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}: ${response.message}")
                }
                response.body?.string() ?: "OK"
            }
        }
    }
}

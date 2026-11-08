package com.example.projectorremote.data

/**
 * DAFTAR PERINTAH YANG DIKENAL APLIKASI.
 *
 * Bahasa sederhananya: ini "menu" perintah yang bisa diucapkan pengguna.
 * Setiap baris punya dua isi:
 *   - displayName : tulisan yang tampil di layar HP (untuk manusia).
 *   - esp32Code   : kode pendek yang dikirim lewat WiFi ke ESP32 (untuk mesin).
 *
 * >>> TEMPAT MENAMBAH PERINTAH BARU (langkah 1 dari 3 di Android) <<<
 * Contoh: INPUT_HDMI("Input HDMI", "HDMI"),
 * Setelah menambah di sini, tambahkan juga:
 *   - kata kuncinya di NlpProcessor.kt (supaya ucapannya dikenali),
 *   - kode yang sama di esp32_firmware.ino (handleCommand),
 *   - terjemahan ke perintah proyektor di arduino_uno_benq.ino (handleCommandFromEsp).
 * Kode (misalnya "PON") harus persis sama di ketiga tempat itu.
 */
enum class Command(val displayName: String, val esp32Code: String) {
    PROJECTOR_ON("Aktifkan Proyektor", "PON"),
    PROJECTOR_OFF("Matikan Proyektor", "POFF"),
    VOLUME_UP("Volume Naik", "VUP"),
    VOLUME_DOWN("Volume Turun", "VDOWN"),
    // UNKNOWN = ucapan tidak cocok dengan perintah apa pun. Tidak pernah dikirim ke ESP32.
    UNKNOWN("Tidak dikenali", "");

    companion object {
        fun fromEsp32Code(code: String): Command =
            entries.find { it.esp32Code == code } ?: UNKNOWN
    }
}

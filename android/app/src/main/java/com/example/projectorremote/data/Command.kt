package com.example.projectorremote.data

/**
 * Perintah yang didukung sistem untuk mengendalikan proyektor.
 * Harus sesuai dengan kode perintah yang diprogram di ESP32.
 */
enum class Command(val displayName: String, val esp32Code: String) {
    PROJECTOR_ON("Aktifkan Proyektor", "PON"),
    PROJECTOR_OFF("Matikan Proyektor", "POFF"),
    VOLUME_UP("Volume Naik", "VUP"),
    VOLUME_DOWN("Volume Turun", "VDOWN"),
    UNKNOWN("Tidak dikenali", "");

    companion object {
        fun fromEsp32Code(code: String): Command =
            entries.find { it.esp32Code == code } ?: UNKNOWN
    }
}

package com.example.projectorremote.data.nlp

import com.example.projectorremote.data.Command
import java.util.Locale
import java.util.regex.Pattern

/**
 * Pemrosesan Natural Language untuk mencocokkan ucapan pengguna
 * dengan kata kunci perintah proyektor.
 *
 * Metode: normalisasi teks, tokenisasi, pencocokan kata kunci (keyword matching).
 */
object NlpProcessor {

    // Kata kunci untuk setiap perintah (bisa diperluas untuk variasi ucapan)
    private val keywordsProjectorOn = setOf(
        "aktifkan", "nyalakan", "hidupkan", "on", "nyala", "buka"
    ).flatMap { listOf(it, "${it} proyektor", "proyektor $it") }.toSet() +
            setOf("proyektor nyala", "proyektor hidup", "nyalakan proyektor", "hidupkan proyektor")

    private val keywordsProjectorOff = setOf(
        "matikan", "padamkan", "off", "padam", "tutup"
    ).flatMap { listOf(it, "${it} proyektor", "proyektor $it") }.toSet() +
            setOf("proyektor mati", "matikan proyektor", "padamkan proyektor")

    private val keywordsVolumeUp = setOf(
        "volume naik", "naikkan volume", "volume up", "perbesar volume",
        "volume besar", "tinggikan volume", "volume tambah"
    )

    private val keywordsVolumeDown = setOf(
        "volume turun", "turunkan volume", "volume down", "perkecil volume",
        "volume kecil", "volume kurang"
    )

    /**
     * Menentukan perintah dari teks ucapan.
     * @param speechText teks hasil speech recognition
     * @return Command yang cocok atau UNKNOWN
     */
    fun matchCommand(speechText: String): Command {
        val normalized = normalize(speechText)
        if (normalized.isBlank()) return Command.UNKNOWN

        return when {
            matchesAny(normalized, keywordsProjectorOn) -> Command.PROJECTOR_ON
            matchesAny(normalized, keywordsProjectorOff) -> Command.PROJECTOR_OFF
            matchesAny(normalized, keywordsVolumeUp) -> Command.VOLUME_UP
            matchesAny(normalized, keywordsVolumeDown) -> Command.VOLUME_DOWN
            else -> Command.UNKNOWN
        }
    }

    /**
     * Normalisasi teks: lowercase, hapus karakter non-alfanumerik berlebih, trim.
     */
    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        var t = text.lowercase(Locale.getDefault()).trim()
        t = Pattern.compile("\\s+").matcher(t).replaceAll(" ")
        return t
    }

    private fun matchesAny(normalizedText: String, keywords: Set<String>): Boolean {
        return keywords.any { keyword ->
            normalizedText.contains(keyword) || keyword.split(" ").all { normalizedText.contains(it) }
        }
    }
}

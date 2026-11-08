package com.example.projectorremote.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.projectorremote.data.Command
import com.example.projectorremote.data.debug.AppDatabase
import com.example.projectorremote.data.debug.DebugLogEntity
import com.example.projectorremote.data.esp32.Esp32Client
import com.example.projectorremote.data.nlp.NlpProcessor
import com.example.projectorremote.data.settings.SettingsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val db = AppDatabase.getInstance(application)
    private val debugDao = db.debugLogDao()

    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    private val _lastSpeechText = MutableLiveData<String>()
    val lastSpeechText: LiveData<String> = _lastSpeechText

    private val _lastCommand = MutableLiveData<Command>()
    val lastCommand: LiveData<Command> = _lastCommand

    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    fun setListening(value: Boolean) {
        _isListening.value = value
    }

    /**
     * PUSAT ALUR APLIKASI: teks ucapan masuk, perintah keluar ke ESP32.
     * Urutannya:
     *   1. Teks diartikan oleh NlpProcessor menjadi sebuah Command.
     *   2. Kalau tidak dikenali -> tampilkan "Perintah tidak dikenali", catat ke Debug, selesai.
     *   3. Kalau dikenali -> lanjut kirim ke esp32 (lihat penjelasan di bawah).
     *   4. Hasilnya (berhasil/gagal) ditampilkan di layar dan dicatat ke menu Debug.
     */
    fun processSpeechAndSend(speechText: String) {
        viewModelScope.launch {
            val normalized = NlpProcessor.normalize(speechText)
            val command = NlpProcessor.matchCommand(speechText)
            _lastSpeechText.value = speechText
            _lastCommand.value = command

            if (command == Command.UNKNOWN) {
                _statusMessage.value = "Perintah tidak dikenali: \"$speechText\""
                logToDebug(speechText, normalized, command, null, null, false, "Perintah tidak dikenali")
                return@launch
            }

            val baseUrl = settingsRepo.getEsp32BaseUrl()
            val client = Esp32Client(baseUrl)
            val activeProfile = settingsRepo.getActiveProfile()

            // DUA JALUR PENGIRIMAN:
            //  - Jalur CUSTOM   : jika profil proyektor aktif di Settings punya isian untuk perintah ini (misal "*pow=on#"), string itu dikirim apa adanya lewat /commandRaw. Ganti merek proyektor cukup ganti profil, tidak perlu upload ulang firmware.
            //  - Jalur STANDAR  : jika isian profil kosong, kirim kode pendek (misal "PON") lewat /command, lalu Arduino Uno yang menerjemahkannya.
            // >>> PERINTAH BARU yang juga bisa diisi di profil: tambahkan barisnya di sini,
            //     contoh: Command.INPUT_HDMI -> activeProfile?.cmdHdmi
            val rawCommand = when (command) {
                Command.PROJECTOR_ON -> activeProfile?.cmdPon
                Command.PROJECTOR_OFF -> activeProfile?.cmdPoff
                Command.VOLUME_UP -> activeProfile?.cmdVup
                Command.VOLUME_DOWN -> activeProfile?.cmdVdown
                else -> null
            }

            val useRawCommand = !rawCommand.isNullOrBlank()

            if (useRawCommand) {
                val requestUrl = "$baseUrl/commandRaw?raw=${java.net.URLEncoder.encode(rawCommand, "UTF-8")}"
                val result = client.sendRawCommand(rawCommand)
                result.fold(
                    onSuccess = { response ->
                        _statusMessage.value = "Perintah \"${command.displayName}\" terkirim (custom)."
                        logToDebug(speechText, normalized, command, requestUrl, response, true, null)
                    },
                    onFailure = { e ->
                        _statusMessage.value = "Gagal: ${e.message}"
                        logToDebug(speechText, normalized, command, requestUrl, null, false, e.message)
                    }
                )
            } else {
                val requestUrl = "$baseUrl/command?cmd=${command.esp32Code}"
                val result = client.sendCommand(command)
                result.fold(
                    onSuccess = { response ->
                        _statusMessage.value = "Perintah \"${command.displayName}\" terkirim."
                        logToDebug(speechText, normalized, command, requestUrl, response, true, null)
                    },
                    onFailure = { e ->
                        _statusMessage.value = "Gagal: ${e.message}"
                        logToDebug(speechText, normalized, command, requestUrl, null, false, e.message)
                    }
                )
            }
        }
    }

    // ALUR 7 — Setiap percobaan perintah dicatat ke database HP (Room) untuk data penelitian. Isinya tampil di tab Debug. Kolom baru di tabel Debug juga diisi di sini.
    private suspend fun logToDebug(
        rawSpeech: String,
        normalizedText: String,
        command: Command,
        requestUrl: String?,
        responseBody: String?,
        success: Boolean,
        errorMessage: String?
    ) {
        debugDao.insert(
            DebugLogEntity(
                timestamp = System.currentTimeMillis(),
                rawSpeech = rawSpeech,
                normalizedText = normalizedText,
                matchedCommand = command.displayName,
                esp32Code = command.esp32Code,
                requestUrl = requestUrl,
                responseBody = responseBody,
                success = success,
                errorMessage = errorMessage
            )
        )
    }
}

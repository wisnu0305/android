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

            // Jika ada profil aktif, kirim command string dari profil via /commandRaw
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

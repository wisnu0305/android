package com.example.projectorremote.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.projectorremote.data.esp32.Esp32Client
import com.example.projectorremote.data.settings.SavedProjectorProfile
import com.example.projectorremote.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Satu item di dropdown: profil tersimpan atau opsi "Tambah proyektor baru".
 */
data class ProjectorProfileOption(
    val id: String,
    val label: String,
    val isAddNew: Boolean = false,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SettingsRepository(application)

    private val _currentIp = MutableLiveData<String>()
    val currentIp: LiveData<String> = _currentIp

    /** Daftar opsi dropdown: profil tersimpan + "Tambah proyektor baru". */
    private val _profileOptions = MutableLiveData<List<ProjectorProfileOption>>(emptyList())
    val profileOptions: LiveData<List<ProjectorProfileOption>> = _profileOptions

    /** ID yang sedang dipilih di dropdown (bisa id profil atau ADD_NEW_ID). */
    private val _selectedOptionId = MutableLiveData<String?>(null)
    val selectedOptionId: LiveData<String?> = _selectedOptionId

    /** Data form untuk mengisi/edit: profil yang sedang diedit atau kosong untuk tambah baru. */
    private val _formProfile = MutableLiveData<SavedProjectorProfile?>(null)
    val formProfile: LiveData<SavedProjectorProfile?> = _formProfile

    private val _saveMessage = MutableLiveData<String>()
    val saveMessage: LiveData<String> = _saveMessage

    companion object {
        const val ADD_NEW_ID = "__add_new__"
    }

    fun loadSettings() {
        _currentIp.value = repo.getEsp32Ip()
        val saved = repo.getSavedProfiles()
        val options = saved.map { ProjectorProfileOption(it.id, it.name, false) } +
                ProjectorProfileOption(ADD_NEW_ID, "➕ Tambah proyektor baru", true)
        _profileOptions.value = options

        val selectedId = repo.getSelectedProfileId()
        _selectedOptionId.value = when {
            selectedId != null && saved.any { it.id == selectedId } -> selectedId
            saved.isNotEmpty() -> saved.first().id
            else -> ADD_NEW_ID
        }
        refreshFormFromSelection()
    }

    fun onProfileOptionSelected(optionId: String) {
        _selectedOptionId.value = optionId
        refreshFormFromSelection()
    }

    private fun refreshFormFromSelection() {
        val optionId = _selectedOptionId.value
        _formProfile.value = when {
            optionId == null || optionId == ADD_NEW_ID -> null
            else -> repo.getProfileById(optionId)
        }
    }

    fun saveSettings(
        ip: String,
        profileName: String,
        baud: String?,
        cmdPon: String?,
        cmdPoff: String?,
        cmdVup: String?,
        cmdVdown: String?
    ) {
        val trimmedIp = ip.trim()
        if (trimmedIp.isEmpty()) {
            _saveMessage.value = "IP tidak boleh kosong"
            return
        }
        val name = profileName.trim()
        if (name.isEmpty()) {
            _saveMessage.value = "Nama proyektor tidak boleh kosong"
            return
        }
        val baudInt = baud?.trim()?.toIntOrNull() ?: 0
        if (baudInt <= 0) {
            _saveMessage.value = "Baudrate tidak valid (mis. 9600)"
            return
        }

        val pon = (cmdPon ?: "").trim()
        val poff = (cmdPoff ?: "").trim()
        val vup = (cmdVup ?: "").trim()
        val vdown = (cmdVdown ?: "").trim()

        val optionId = _selectedOptionId.value
        val existingProfile = if (optionId != null && optionId != ADD_NEW_ID) repo.getProfileById(optionId) else null
        val profile = existingProfile?.copy(
            name = name,
            baud = baudInt,
            cmdPon = pon,
            cmdPoff = poff,
            cmdVup = vup,
            cmdVdown = vdown
        ) ?: SavedProjectorProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            baud = baudInt,
            cmdPon = pon,
            cmdPoff = poff,
            cmdVup = vup,
            cmdVdown = vdown
        )

        repo.setEsp32Ip(trimmedIp)
        repo.saveProfile(profile)
        repo.setSelectedProfileId(profile.id)
        _currentIp.value = trimmedIp
        _selectedOptionId.value = profile.id
        _formProfile.value = profile
        updateProfileOptions()

        viewModelScope.launch {
            val baseUrl = repo.getEsp32BaseUrl()
            val client = Esp32Client(baseUrl)
            val result = withContext(Dispatchers.IO) {
                client.setCustomProfile(
                    baud = profile.baud,
                    cmdPon = profile.cmdPon,
                    cmdPoff = profile.cmdPoff,
                    cmdVup = profile.cmdVup,
                    cmdVdown = profile.cmdVdown
                )
            }
            result.fold(
                onSuccess = {
                    _saveMessage.value = "Pengaturan disimpan & dikirim ke ESP32"
                },
                onFailure = { e ->
                    _saveMessage.value = "Disimpan lokal, gagal kirim ke ESP32: ${e.message}"
                }
            )
        }
    }

    private fun updateProfileOptions() {
        val saved = repo.getSavedProfiles()
        _profileOptions.value = saved.map { ProjectorProfileOption(it.id, it.name, false) } +
                ProjectorProfileOption(ADD_NEW_ID, "➕ Tambah proyektor baru", true)
    }

    /**
     * Hapus profil yang sedang dipilih. Hanya berlaku jika yang dipilih adalah profil tersimpan (bukan "Tambah baru").
     * Setelah hapus, pilihan pindah ke profil lain atau "Tambah proyektor baru".
     */
    fun deleteSelectedProfile() {
        val optionId = _selectedOptionId.value ?: return
        if (optionId == ADD_NEW_ID) return
        val profile = repo.getProfileById(optionId) ?: return
        repo.removeProfile(optionId)
        val remaining = repo.getSavedProfiles()
        _selectedOptionId.value = remaining.firstOrNull()?.id ?: ADD_NEW_ID
        refreshFormFromSelection()
        updateProfileOptions()
        _saveMessage.value = "Profil dihapus."
    }

    /** Apakah opsi yang dipilih bisa dihapus (profil tersimpan, bukan "Tambah baru"). */
    fun canDeleteSelectedProfile(): Boolean {
        val id = _selectedOptionId.value ?: return false
        return id != ADD_NEW_ID && repo.getProfileById(id) != null
    }

    /** Nama profil yang dipilih (untuk dialog konfirmasi). */
    fun getSelectedProfileName(): String? =
        _selectedOptionId.value?.let { repo.getProfileById(it)?.name }

    fun clearSaveMessage() {
        _saveMessage.value = ""
    }
}

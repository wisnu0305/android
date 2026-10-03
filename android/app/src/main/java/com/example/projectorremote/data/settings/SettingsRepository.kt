package com.example.projectorremote.data.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val PREFS_NAME = "projector_remote_settings"
private const val KEY_ESP32_IP = "esp32_ip"
private const val KEY_SAVED_PROFILES = "saved_profiles"
private const val KEY_SELECTED_PROFILE_ID = "selected_profile_id"

private const val DEFAULT_IP = "192.168.1.100"

/**
 * Profil proyektor yang disimpan pengguna (nama, baudrate, perintah RS232).
 */
data class SavedProjectorProfile(
    val id: String,
    val name: String,
    val baud: Int,
    val cmdPon: String,
    val cmdPoff: String,
    val cmdVup: String,
    val cmdVdown: String,
) {
    fun toCustomProfile(): CustomProjectorProfile = CustomProjectorProfile(
        baud = baud,
        cmdPon = cmdPon,
        cmdPoff = cmdPoff,
        cmdVup = cmdVup,
        cmdVdown = cmdVdown
    )
}

data class CustomProjectorProfile(
    val baud: Int,
    val cmdPon: String,
    val cmdPoff: String,
    val cmdVup: String,
    val cmdVdown: String,
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getEsp32BaseUrl(): String {
        val ip = prefs.getString(KEY_ESP32_IP, null) ?: DEFAULT_IP
        return "http://$ip"
    }

    fun setEsp32Ip(ip: String) {
        prefs.edit().putString(KEY_ESP32_IP, ip.trim()).apply()
    }

    fun getEsp32Ip(): String = prefs.getString(KEY_ESP32_IP, DEFAULT_IP) ?: DEFAULT_IP

    // ---------- Profil proyektor (list + selected) ----------

    fun getSavedProfiles(): List<SavedProjectorProfile> {
        val json = prefs.getString(KEY_SAVED_PROFILES, "[]") ?: "[]"
        return parseProfilesJson(json)
    }

    fun saveProfile(profile: SavedProjectorProfile) {
        val list = getSavedProfiles().toMutableList()
        val index = list.indexOfFirst { it.id == profile.id }
        if (index >= 0) {
            list[index] = profile
        } else {
            list.add(profile)
        }
        prefs.edit().putString(KEY_SAVED_PROFILES, toJson(list)).apply()
    }

    fun removeProfile(id: String) {
        val list = getSavedProfiles().filter { it.id != id }.toMutableList()
        prefs.edit().putString(KEY_SAVED_PROFILES, toJson(list)).apply()
        if (getSelectedProfileId() == id) {
            prefs.edit().putString(KEY_SELECTED_PROFILE_ID, list.firstOrNull()?.id).apply()
        }
    }

    fun setSelectedProfileId(id: String?) {
        prefs.edit().putString(KEY_SELECTED_PROFILE_ID, id).apply()
    }

    fun getSelectedProfileId(): String? =
        prefs.getString(KEY_SELECTED_PROFILE_ID, null)?.takeIf { it.isNotBlank() }

    fun getProfileById(id: String): SavedProjectorProfile? =
        getSavedProfiles().find { it.id == id }

    /** Profil yang sedang dipakai (untuk kirim ke ESP32). */
    fun getActiveProfile(): SavedProjectorProfile? {
        val id = getSelectedProfileId() ?: return null
        return getProfileById(id)
    }

    private fun toJson(profiles: List<SavedProjectorProfile>): String {
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("baud", p.baud)
                put("cmdPon", p.cmdPon)
                put("cmdPoff", p.cmdPoff)
                put("cmdVup", p.cmdVup)
                put("cmdVdown", p.cmdVdown)
            })
        }
        return arr.toString()
    }

    private fun parseProfilesJson(json: String): List<SavedProjectorProfile> {
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SavedProjectorProfile(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", ""),
                    baud = o.optInt("baud", 0),
                    cmdPon = o.optString("cmdPon", ""),
                    cmdPoff = o.optString("cmdPoff", ""),
                    cmdVup = o.optString("cmdVup", ""),
                    cmdVdown = o.optString("cmdVdown", "")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

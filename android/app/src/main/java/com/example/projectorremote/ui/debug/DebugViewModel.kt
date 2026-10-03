package com.example.projectorremote.ui.debug

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.projectorremote.data.debug.AppDatabase
import com.example.projectorremote.data.debug.DebugLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DebugViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).debugLogDao()

    val logs: LiveData<List<DebugLogEntity>> = dao.getAllLive()

    fun clearLogs() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { dao.clearAll() }
        }
    }
}

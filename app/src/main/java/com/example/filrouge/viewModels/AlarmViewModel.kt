package com.example.filrouge.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.data.Alarm
import com.example.filrouge.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(
    private val repo: AlarmRepository = AlarmRepository()
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repo.alarmsFlow()
        .catch { Log.e("AlarmVM", "Erreur de lecture", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createAlarm(hour: Int, minute: Int) = viewModelScope.launch {
        repo.createAlarm(hour, minute).onFailure { Log.e("AlarmVM", "Création échouée", it) }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(alarm.id, enabled).onFailure { Log.e("AlarmVM", "Mise à jour échouée", it) }
    }

    fun delete(alarm: Alarm) = viewModelScope.launch {
        repo.deleteAlarm(alarm.id).onFailure { Log.e("AlarmVM", "Suppression échouée", it) }
    }
}
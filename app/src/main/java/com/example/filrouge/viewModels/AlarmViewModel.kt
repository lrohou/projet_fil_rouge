package com.example.filrouge.viewModels

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.AlarmReceiver
import com.example.filrouge.data.Alarm
import com.example.filrouge.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmViewModel(
    private val repo: AlarmRepository = AlarmRepository()
) : ViewModel() {

    val alarms: StateFlow<List<Alarm>> = repo.alarmsFlow()
        .catch { Log.e("AlarmVM", "Erreur de lecture", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        name: String,
        days: List<Int>,
        soundName: String,
        vibrate: Boolean
    ) = viewModelScope.launch {
        repo.createAlarm(hour, minute, name, days, soundName, vibrate)
            .onSuccess { alarmId ->
                scheduleAlarm(context, alarmId, hour, minute, name, soundName, vibrate, days, true)
            }
            .onFailure { Log.e("AlarmVM", "Création échouée", it) }
    }

    fun updateAlarm(
        context: Context,
        alarm: Alarm,
        hour: Int,
        minute: Int,
        name: String,
        days: List<Int>,
        soundName: String,
        vibrate: Boolean
    ) = viewModelScope.launch {
        repo.updateAlarm(alarm.id, hour, minute, name, days, soundName, vibrate)
            .onSuccess {
                // Re-programmer l'alarme avec les nouvelles valeurs
                cancelAlarm(context, alarm.id)
                scheduleAlarm(context, alarm.id, hour, minute, name, soundName, vibrate, days, true)
            }
            .onFailure { Log.e("AlarmVM", "Mise à jour échouée", it) }
    }

    fun setEnabled(context: Context, alarm: Alarm, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(alarm.id, enabled)
            .onSuccess {
                if (enabled) {
                    scheduleAlarm(context, alarm.id, alarm.hour, alarm.minute,
                        alarm.name, alarm.soundName, alarm.vibrate, alarm.days, true)
                } else {
                    cancelAlarm(context, alarm.id)
                }
            }
            .onFailure { Log.e("AlarmVM", "Mise à jour échouée", it) }
    }

    fun delete(context: Context, alarm: Alarm) = viewModelScope.launch {
        cancelAlarm(context, alarm.id)
        repo.deleteAlarm(alarm.id).onFailure { Log.e("AlarmVM", "Suppression échouée", it) }
    }

    //programme ou reprogramme toutes les alarmes actives (appelé au boot)
    fun rescheduleAll(context: Context) {
        viewModelScope.launch {
            //on attend que le flow emette une première valeur
            val currentAlarms = alarms.value
            currentAlarms.filter { it.enabled }.forEach { alarm ->
                scheduleAlarm(context, alarm.id, alarm.hour, alarm.minute,
                    alarm.name, alarm.soundName, alarm.vibrate, alarm.days, true)
            }
        }
    }

    companion object {
        fun scheduleAlarm(
            context: Context,
            alarmId: String,
            hour: Int,
            minute: Int,
            name: String,
            soundName: String,
            vibrate: Boolean,
            days: List<Int>,
            enabled: Boolean
        ) {
            if (!enabled) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = alarmId.hashCode()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm_name", name)
                putExtra("alarm_sound", soundName)
                putExtra("alarm_vibrate", vibrate)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Si l'heure est déjà passée aujourd'hui, programmer pour demain
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Si des jours spécifiques sont définis, trouver le prochain jour valide
            if (days.isNotEmpty()) {
                // Convertir nos jours ISO (1=Lun) en Calendar (2=Lun)
                val calendarDays = days.map { if (it == 7) Calendar.SUNDAY else it + 1 }
                var found = false
                for (i in 0..7) {
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    if (dayOfWeek in calendarDays) {
                        found = true
                        break
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                if (!found) return
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                            pendingIntent
                        )
                    } else {
                        // Fallback si pas la permission d'alarmes exactes
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                        pendingIntent
                    )
                }
                Log.d("AlarmVM", "Alarme programmée : $name à $hour:$minute, id=$alarmId")
            } catch (e: SecurityException) {
                Log.e("AlarmVM", "Pas la permission pour les alarmes exactes", e)
            }
        }

        fun cancelAlarm(context: Context, alarmId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, alarmId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
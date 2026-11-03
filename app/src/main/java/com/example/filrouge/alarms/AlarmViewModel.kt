package com.example.filrouge.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel gérant l'état UI des alarmes et des invitations.
 * Assure la communication avec AlarmRepository et orchestre les appels système à AlarmManager.
 */
class AlarmViewModel(
    private val repo: AlarmRepository = AlarmRepository()
) : ViewModel() {

    /** Flux réactif de la liste des alarmes de l'utilisateur connecté */
    val alarms: StateFlow<List<Alarm>> = repo.alarmsFlow()
        .catch { Log.e("AlarmVM", "Erreur de lecture", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Flux réactif des invitations d'alarme reçues */
    val alarmInvitations: StateFlow<List<AlarmInvitation>> = repo.alarmInvitationsFlow()
        .catch { Log.e("AlarmVM", "Erreur de lecture invitations", it); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Accepte une invitation d'alarme partagée */
    fun acceptInvitation(invitation: AlarmInvitation) = viewModelScope.launch {
        repo.acceptAlarmInvitation(invitation)
            .onSuccess { Log.d("AlarmVM", "Invitation acceptée: ${invitation.alarmName}") }
            .onFailure { Log.e("AlarmVM", "Échec acceptation invitation", it) }
    }

    /** Refuse une invitation d'alarme */
    fun declineInvitation(invitationId: String) = viewModelScope.launch {
        repo.declineAlarmInvitation(invitationId)
            .onFailure { Log.e("AlarmVM", "Échec refus invitation", it) }
    }

    /** Crée une nouvelle alarme dans Firestore et la programme sur le système */
    fun createAlarm(
        context: Context,
        hour: Int,
        minute: Int,
        name: String,
        days: List<Int>,
        soundName: String,
        vibrate: Boolean,
        contacts: List<String>,
        discordWebhookUrl: String = ""
    ) = viewModelScope.launch {
        Log.d("AlarmVM", "createAlarm appelée avec ${contacts.size} contacts: $contacts")
        repo.createAlarm(hour, minute, name, days, soundName, vibrate, contacts, discordWebhookUrl)
            .onSuccess { alarmId ->
                Log.d("AlarmVM", "Alarme créée avec succès. id=$alarmId")
                scheduleAlarm(context, alarmId, hour, minute, name, soundName, vibrate, days, true, discordWebhookUrl)
            }
            .onFailure { e ->
                Log.e("AlarmVM", "Création d'alarme échouée: ${e.message}", e)
            }
    }

    /** Met à jour une alarme existante et la reprogramme */
    fun updateAlarm(
        context: Context,
        alarm: Alarm,
        hour: Int,
        minute: Int,
        name: String,
        days: List<Int>,
        soundName: String,
        vibrate: Boolean,
        contacts: List<String>,
        discordWebhookUrl: String = ""
    ) = viewModelScope.launch {
        repo.updateAlarm(alarm.id, hour, minute, name, days, soundName, vibrate, contacts, discordWebhookUrl)
            .onSuccess {
                // Re-programmer l'alarme avec les nouvelles valeurs
                cancelAlarm(context, alarm.id)
                scheduleAlarm(context, alarm.id, hour, minute, name, soundName, vibrate, days, true, discordWebhookUrl)
            }
            .onFailure { Log.e("AlarmVM", "Mise à jour échouée", it) }
    }

    /** Active ou désactive une alarme */
    fun setEnabled(context: Context, alarm: Alarm, enabled: Boolean) = viewModelScope.launch {
        repo.setEnabled(alarm.id, enabled)
            .onSuccess {
                if (enabled) {
                    scheduleAlarm(context, alarm.id, alarm.hour, alarm.minute,
                        alarm.name, alarm.soundName, alarm.vibrate, alarm.days, true, alarm.discordWebhookUrl)
                } else {
                    cancelAlarm(context, alarm.id)
                }
            }
            .onFailure { Log.e("AlarmVM", "Mise à jour échouée", it) }
    }

    /** Supprime une alarme et annule sa programmation système */
    fun delete(context: Context, alarm: Alarm) = viewModelScope.launch {
        cancelAlarm(context, alarm.id)
        repo.deleteAlarm(alarm.id).onFailure { Log.e("AlarmVM", "Suppression échouée", it) }
    }

    /** Reprogramme toutes les alarmes actives (utilisé notamment après démarrage système) */
    fun rescheduleAll(context: Context) {
        viewModelScope.launch {
            //on attend que le flow emette une première valeur
            val currentAlarms = alarms.value
            currentAlarms.filter { it.enabled }.forEach { alarm ->
                scheduleAlarm(context, alarm.id, alarm.hour, alarm.minute,
                    alarm.name, alarm.soundName, alarm.vibrate, alarm.days, true, alarm.discordWebhookUrl)
            }
        }
    }

    companion object {
        /**
         * Programme une alarme exacte via l'AlarmManager Android.
         * Détermine la prochaine occurrence selon l'heure et les jours de répétition spécifiés.
         */
        fun scheduleAlarm(
            context: Context,
            alarmId: String,
            hour: Int,
            minute: Int,
            name: String,
            soundName: String,
            vibrate: Boolean,
            days: List<Int>,
            enabled: Boolean,
            discordWebhookUrl: String = ""
        ) {
            if (!enabled) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = alarmId.hashCode()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm_name", name)
                putExtra("alarm_sound", soundName)
                putExtra("alarm_vibrate", vibrate)
                putExtra("alarm_webhook_url", discordWebhookUrl)
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

            // Si l'heure exacte est déjà passée aujourd'hui, décaler au lendemain
            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            // Filtrage selon les jours de la semaine si spécifiés
            if (days.isNotEmpty()) {
                // Conversion des jours ISO (1=Lun .. 7=Dim) vers Calendar (2=Lun .. 1=Dim)
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

        /** Annule une alarme enregistrée dans l'AlarmManager */
        fun cancelAlarm(context: Context, alarmId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, alarmId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            cancelSnooze(context, alarmId)
            SnoozeManager.resetSnoozeCount(context, alarmId)
        }

        /** Programme un rappel snooze */
        fun scheduleSnooze(
            context: Context,
            alarmId: String,
            name: String,
            soundName: String,
            vibrate: Boolean,
            minutesLater: Int = 10,
            discordWebhookUrl: String = ""
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val requestCode = (alarmId + "_snooze").hashCode()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm_name", name)
                putExtra("alarm_sound", soundName)
                putExtra("alarm_vibrate", vibrate)
                putExtra("alarm_webhook_url", discordWebhookUrl)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + minutesLater * 60 * 1000L

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setAlarmClock(
                        AlarmManager.AlarmClockInfo(triggerTime, pendingIntent),
                        pendingIntent
                    )
                }
                Log.d("AlarmVM", "Snooze programmé dans $minutesLater min pour $name (id=$alarmId)")
            } catch (e: SecurityException) {
                Log.e("AlarmVM", "Pas la permission pour programmer le snooze", e)
            }
        }

        /** Annule un snooze en attente */
        fun cancelSnooze(context: Context, alarmId: String) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val requestCode = (alarmId + "_snooze").hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}

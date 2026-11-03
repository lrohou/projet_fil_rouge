package com.example.filrouge.alarms

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Singleton de gestion du mode Snooze.
 * Gère le comptage local du nombre de réimpressions via SharedPreferences,
 * calcule le retard accumulé et envoie des alertes HTTP POST vers des webhooks Discord.
 */
object SnoozeManager {
    private const val PREFS_NAME = "alarm_snooze_prefs"
    private const val KEY_SNOOZE_COUNT_PREFIX = "snooze_count_"
    private const val KEY_USER_NAME = "user_display_name"

    /** Enregistre le nom d'affichage personnalisé */
    fun saveUserName(context: Context, name: String) {
        if (name.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_NAME, name).apply()
    }

    /** Récupère le nom du réveil ou de l'utilisateur connecté */
    fun getUserName(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, null)
        if (!saved.isNullOrBlank()) return saved
        return FirebaseAuth.getInstance()
            .currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Utilisateur"
    }

    /** Récupère le nombre de snoozes enregistrés pour une alarme spécifique */
    fun getSnoozeCount(context: Context, alarmId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SNOOZE_COUNT_PREFIX + alarmId, 0)
    }

    /** Incrémente et sauvegarde le nombre de report pour une alarme */
    fun incrementSnoozeCount(context: Context, alarmId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newCount = getSnoozeCount(context, alarmId) + 1
        prefs.edit().putInt(KEY_SNOOZE_COUNT_PREFIX + alarmId, newCount).apply()
        return newCount
    }

    /** Calcule le retard estimé en minutes (10 min par snooze) */
    fun getEstimatedDelayMinutes(context: Context, alarmId: String): Int {
        return getSnoozeCount(context, alarmId) * 10
    }

    /** Réinitialise le compteur de report d'une alarme */
    fun resetSnoozeCount(context: Context, alarmId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SNOOZE_COUNT_PREFIX + alarmId).apply()
    }

    /**
     * Envoie une requête HTTP POST au webhook Discord configuré pour notifier du retard accumulé.
     */
    fun sendDiscordSnoozeNotification(
        userName: String,
        alarmName: String,
        snoozeCount: Int,
        webhookUrl: String
    ) {
        if (webhookUrl.isBlank()) return

        val retardMinutes = snoozeCount * 10
        val safeName = userName.replace("\"", "\\\"")
        val safeAlarm = alarmName.replace("\"", "\\\"")
        val json = """{"content":"⏰ **$safeName** a appuyé sur snooze pour l'alarme **$safeAlarm** (x$snoozeCount). Retard estimé : **$retardMinutes min**"}"""

        // Envoi asynchrone HTTP dans le Dispatcher IO
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(webhookUrl.trim())
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true
                OutputStreamWriter(conn.outputStream).use { it.write(json) }
                conn.responseCode // déclenche l'envoi
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("SnoozeManager", "Erreur envoi webhook Discord", e)
            }
        }
    }
}

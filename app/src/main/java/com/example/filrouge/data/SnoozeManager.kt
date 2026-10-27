package com.example.filrouge.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object SnoozeManager {
    private const val PREFS_NAME = "alarm_snooze_prefs"
    private const val KEY_SNOOZE_COUNT_PREFIX = "snooze_count_"
    private const val KEY_USER_NAME = "user_display_name"
    private const val DISCORD_WEBHOOK_URL =
        "https://discord.com/api/webhooks/1564708643335704646/0lUA1MX6nrK7SL3mKEKMu6nGKuA4IoflFyZPOalS28DryHCkcwgxWIvjdsk7NKKKicoE"

    fun saveUserName(context: Context, name: String) {
        if (name.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_NAME, name).apply()
    }

    fun getUserName(context: Context): String {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, null)
        if (!saved.isNullOrBlank()) return saved
        return com.google.firebase.auth.FirebaseAuth.getInstance()
            .currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Utilisateur"
    }

    fun getSnoozeCount(context: Context, alarmId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SNOOZE_COUNT_PREFIX + alarmId, 0)
    }

    fun incrementSnoozeCount(context: Context, alarmId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val newCount = getSnoozeCount(context, alarmId) + 1
        prefs.edit().putInt(KEY_SNOOZE_COUNT_PREFIX + alarmId, newCount).apply()
        return newCount
    }

    fun getEstimatedDelayMinutes(context: Context, alarmId: String): Int {
        return getSnoozeCount(context, alarmId) * 10
    }

    fun resetSnoozeCount(context: Context, alarmId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SNOOZE_COUNT_PREFIX + alarmId).apply()
    }

    fun sendDiscordSnoozeNotification(userName: String, alarmName: String, snoozeCount: Int) {
        val retardMinutes = snoozeCount * 10
        val safeName = userName.replace("\"", "\\\"")
        val safeAlarm = alarmName.replace("\"", "\\\"")
        val json = """{"content":"⏰ **$safeName** a appuyé sur snooze pour l'alarme **$safeAlarm** (x$snoozeCount). Retard estimé : **$retardMinutes min**"}"""

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(DISCORD_WEBHOOK_URL)
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

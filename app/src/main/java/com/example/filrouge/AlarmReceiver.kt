package com.example.filrouge

import android.app.ActivityOptions
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 9999
        const val ACTION_STOP = "com.example.filrouge.STOP_ALARM"
        const val ACTION_SNOOZE = "com.example.filrouge.SNOOZE_ALARM"

        //reference statique au mediaplayer et vibrator pour pouvoir les arrêter
        var mediaPlayer: MediaPlayer? = null
        var vibrator: Vibrator? = null

        fun stopAlarm() {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
            vibrator?.cancel()
            vibrator = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP) {
            stopAlarm()
            val alarmId = intent.getStringExtra("alarm_id")
            if (!alarmId.isNullOrEmpty()) {
                com.example.filrouge.data.SnoozeManager.resetSnoozeCount(context, alarmId)
                com.example.filrouge.viewModels.AlarmViewModel.cancelSnooze(context, alarmId)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            return
        }

        if (intent.action == ACTION_SNOOZE) {
            stopAlarm()
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)

            val alarmId = intent.getStringExtra("alarm_id") ?: return
            val alarmName = intent.getStringExtra("alarm_name") ?: "Alarme"
            val soundName = intent.getStringExtra("alarm_sound") ?: "aura_tone"
            val shouldVibrate = intent.getBooleanExtra("alarm_vibrate", false)

            val newCount = com.example.filrouge.data.SnoozeManager.incrementSnoozeCount(context, alarmId)

            //envoie de la notification Discord
            val userName = com.example.filrouge.data.SnoozeManager.getUserName(context)
            com.example.filrouge.data.SnoozeManager.sendDiscordSnoozeNotification(userName, alarmName, newCount)

            com.example.filrouge.viewModels.AlarmViewModel.scheduleSnooze(
                context = context,
                alarmId = alarmId,
                name = alarmName,
                soundName = soundName,
                vibrate = shouldVibrate,
                minutesLater = 10
            )
            return
        }

        val alarmId = intent.getStringExtra("alarm_id") ?: return
        val alarmName = intent.getStringExtra("alarm_name") ?: "Alarme"
        val soundName = intent.getStringExtra("alarm_sound") ?: "aura_tone"
        val shouldVibrate = intent.getBooleanExtra("alarm_vibrate", false)

        Log.d("AlarmReceiver", "Alarme reçue : $alarmName (son=$soundName, vibrer=$shouldVibrate)")

        //allumer l'écran brièvement avec un wakeLock pour s'assurer que l'appareil est réveillé
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "filrouge:AlarmWakeLock"
        )
        wakeLock?.acquire(3000)

        //créer le canal de notification
        createNotificationChannel(context)

        //jouer le son sur la piste ALARM
        playAlarmSound(context, soundName)

        //vibrer si activé
        if (shouldVibrate) {
            startVibration(context)
        }

        //préparer l'intent pour l'écran plein de l'alarme
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            putExtra("alarm_id", alarmId)
            putExtra("alarm_name", alarmName)
            putExtra("alarm_sound", soundName)
            putExtra("alarm_vibrate", shouldVibrate)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, alarmId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //intent pour le bouton éteindre de la notification
        val stopIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_STOP
            putExtra("alarm_id", alarmId)
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //intent pour le bouton snooze de la notification
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra("alarm_id", alarmId)
            putExtra("alarm_name", alarmName)
            putExtra("alarm_sound", soundName)
            putExtra("alarm_vibrate", shouldVibrate)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, (alarmId + "_snooze_notif").hashCode(), snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        //construire la notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(alarmName)
            .setContentText("Appuyez pour éteindre")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_close, "Éteindre", stopPendingIntent)
            .addAction(R.drawable.ic_bell, "Snooze (+10 min)", snoozePendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)

        //tenter de lancer l'activité par-dessus toute autre application
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }.toBundle()
                fullScreenPendingIntent.send(context, 0, null, null, null, null, options)
            } else {
                fullScreenPendingIntent.send()
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Erreur lancement PendingIntent", e)
        }

        try {
            context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Erreur context.startActivity", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarmes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications d'alarme"
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun playAlarmSound(context: Context, soundName: String) {
        stopAlarm() //arrêter tout son précédent

        try {
            //chercher la ressource raw correspondante
            val resId = context.resources.getIdentifier(soundName, "raw", context.packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer().apply {
                    val afd = context.resources.openRawResourceFd(resId)
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    afd.close()
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            } else {
                //fallback: sonnerie par défaut
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, uri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Erreur lecture son", e)
        }
    }

    private fun startVibration(context: Context) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib

        val pattern = longArrayOf(0, 500, 500)
        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }
}

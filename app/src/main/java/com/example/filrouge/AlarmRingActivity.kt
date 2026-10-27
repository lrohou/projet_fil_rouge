package com.example.filrouge

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.filrouge.data.SnoozeManager
import com.example.filrouge.ui.theme.FilRougeTheme
import com.example.filrouge.viewModels.AlarmViewModel


class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //plein écran
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        val alarmId = intent.getStringExtra("alarm_id") ?: ""
        val alarmName = intent.getStringExtra("alarm_name") ?: "Alarme"
        val soundName = intent.getStringExtra("alarm_sound") ?: "aura_tone"
        val shouldVibrate = intent.getBooleanExtra("alarm_vibrate", false)

        val snoozeCount = SnoozeManager.getSnoozeCount(this, alarmId)

        setContent {
            FilRougeTheme(darkTheme = true, dynamicColor = false) {
                AlarmRingScreen(
                    alarmName = alarmName,
                    snoozeCount = snoozeCount,
                    onDismiss = {
                        AlarmReceiver.stopAlarm()
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.cancel(AlarmReceiver.NOTIFICATION_ID)
                        AlarmViewModel.cancelSnooze(this, alarmId)
                        SnoozeManager.resetSnoozeCount(this, alarmId)
                        finish()
                    },
                    onSnooze = {
                        AlarmReceiver.stopAlarm()
                        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                        nm.cancel(AlarmReceiver.NOTIFICATION_ID)

                        val newCount = SnoozeManager.incrementSnoozeCount(this, alarmId)
                        val retardMinutes = newCount * 10

                        // Envoyer la notification Discord
                        val userName = SnoozeManager.getUserName(this)
                        SnoozeManager.sendDiscordSnoozeNotification(userName, alarmName, newCount)

                        AlarmViewModel.scheduleSnooze(
                            context = this,
                            alarmId = alarmId,
                            name = alarmName,
                            soundName = soundName,
                            vibrate = shouldVibrate,
                            minutesLater = 10
                        )

                        Toast.makeText(
                            this,
                            "Alarme reportée de 10 min (Retard estimé : $retardMinutes min)",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun AlarmRingScreen(
    alarmName: String,
    snoozeCount: Int = 0,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val estimatedDelay = snoozeCount * 10

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B2A), Color(0xFF1B2838), Color(0xFF0A1628))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            //icone alarme
            Text("⏰", fontSize = 72.sp)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = alarmName,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Il est l'heure !",
                fontSize = 18.sp,
                color = Color(0xFFB0C4DE)
            )

            //retard estimé si l'alarme a déjà été snoozée
            if (snoozeCount > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = Color(0x33FFA726),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x88FFA726))
                ) {
                    Text(
                        text = "⏱️ Retard estimé : $estimatedDelay min (Snooze x$snoozeCount)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFCC80),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            //bouton éteindre
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0055A4)
                )
            ) {
                Text(
                    text = "Éteindre",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            //bouton snooze
            Button(
                onClick = onSnooze,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                border = BorderStroke(1.5.dp, Color(0x88FFFFFF)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x22FFFFFF)
                )
            ) {
                Text(
                    text = "Snooze (+10 min)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

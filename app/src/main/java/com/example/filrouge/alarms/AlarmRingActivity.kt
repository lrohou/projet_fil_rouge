package com.example.filrouge.alarms

import android.app.KeyguardManager
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.filrouge.R
import com.example.filrouge.ui.theme.FilRougeTheme

/**
 * Activité plein écran qui s'affiche au-dessus du lockscreen lorsque l'alarme sonne.
 * Configure la fenêtre pour déverrouiller l'écran et propose les boutons d'arrêt et de snooze.
 * Supporte le template spécial "John Pork" lorsque ce son est sélectionné.
 */
class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Plein écran
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        // Autorisation d'affichage par-dessus l'écran de verrouillage et allumage de l'écran
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
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
        val alarmName = intent.getStringExtra("alarm_name") ?: getString(R.string.default_alarm_name)
        val soundName = intent.getStringExtra("alarm_sound") ?: "aura_tone"
        val shouldVibrate = intent.getBooleanExtra("alarm_vibrate", false)
        val alarmWebhookUrl = intent.getStringExtra("alarm_webhook_url") ?: ""

        val snoozeCount = SnoozeManager.getSnoozeCount(this, alarmId)

        val onDismissAction: () -> Unit = {
            //arrêt du réveil
            AlarmReceiver.stopAlarm()
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AlarmReceiver.NOTIFICATION_ID)
            AlarmViewModel.cancelSnooze(this, alarmId)
            SnoozeManager.resetSnoozeCount(this, alarmId)
            finish()
        }

        val onSnoozeAction: () -> Unit = {
            //report d'alarme
            AlarmReceiver.stopAlarm()
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(AlarmReceiver.NOTIFICATION_ID)

            val newCount = SnoozeManager.incrementSnoozeCount(this, alarmId)
            val retardMinutes = newCount * 10

            //envoyer la notification Discord
            val userName = SnoozeManager.getUserName(this)
            SnoozeManager.sendDiscordSnoozeNotification(userName, alarmName, newCount, alarmWebhookUrl)

            //programmation du snooze
            AlarmViewModel.scheduleSnooze(
                context = this,
                alarmId = alarmId,
                name = alarmName,
                soundName = soundName,
                vibrate = shouldVibrate,
                minutesLater = 10,
                discordWebhookUrl = alarmWebhookUrl
            )

            Toast.makeText(
                this,
                getString(R.string.toast_alarm_snoozed, retardMinutes),
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }

        setContent {
            FilRougeTheme(darkTheme = true, dynamicColor = false) {
                if (soundName == "john_pork") {
                    JohnPorkRingScreen(
                        onDismiss = onDismissAction,
                        onSnooze = onSnoozeAction
                    )
                } else {
                    AlarmRingScreen(
                        alarmName = alarmName,
                        snoozeCount = snoozeCount,
                        onDismiss = onDismissAction,
                        onSnooze = onSnoozeAction
                    )
                }
            }
        }
    }
}


@Composable
fun JohnPorkRingScreen(
    onDismiss: () -> Unit,
    onSnooze: () -> Unit
) {
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.6f),
        offset = Offset(2f, 2f),
        blurRadius = 8f
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        //image John Pork
        Image(
            painter = painterResource(id = R.drawable.john_pork_template),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        //conteneur principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(vertical = 48.dp, horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // haut "John Pork is calling..."
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.john_pork_title),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = TextStyle(shadow = textShadow),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.john_pork_is_calling),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f),
                    style = TextStyle(shadow = textShadow),
                    textAlign = TextAlign.Center
                )
            }

            //boutons "Decline" (Snooze) et "Accept" (Éteindre)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                //bouton rouge Decline (Snooze)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .clickable(onClick = onSnooze),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_call_end),
                            contentDescription = stringResource(R.string.call_decline),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.call_decline),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(shadow = textShadow)
                    )
                }

                //bouton vert : Accept (Stop / Éteindre)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_call),
                            contentDescription = stringResource(R.string.call_accept),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.call_accept),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        style = TextStyle(shadow = textShadow)
                    )
                }
            }
        }
    }
}

/**
 * Interface Compose affichée lorsque le réveil sonne (template classique).
 * Affiche le nom de l'alarme, le compteur de snooze/retard et les boutons Éteindre / Snooze.
 */
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
                text = stringResource(R.string.alarm_time_is_up),
                fontSize = 18.sp,
                color = Color(0xFFB0C4DE)
            )

            if (snoozeCount > 0) {
                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    color = Color(0x33FFA726),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0x88FFA726))
                ) {
                    Text(
                        text = stringResource(R.string.alarm_delay_estimate, estimatedDelay, snoozeCount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFCC80),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

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
                    text = stringResource(R.string.btn_turn_off),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    text = stringResource(R.string.btn_snooze_10m),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

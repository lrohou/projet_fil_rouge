package com.example.filrouge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.filrouge.data.AlarmRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("BootReceiver", "Boot complété, reprogrammation des alarmes...")

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = db.collection("alarms")
                    .whereArrayContains("participantIds", uid)
                    .get()
                    .await()

                val alarms = snapshot.toObjects(com.example.filrouge.data.Alarm::class.java)
                alarms.filter { it.enabled }.forEach { alarm ->
                    com.example.filrouge.viewModels.AlarmViewModel.scheduleAlarm(
                        context, alarm.id, alarm.hour, alarm.minute,
                        alarm.name, alarm.soundName, alarm.vibrate, alarm.days, true
                    )
                }
                Log.d("BootReceiver", "${alarms.count { it.enabled }} alarmes reprogrammées")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Erreur reprogrammation", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

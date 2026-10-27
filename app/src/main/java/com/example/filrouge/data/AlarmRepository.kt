package com.example.filrouge.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AlarmRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val alarms get() = db.collection("alarms")

    fun alarmsFlow(): Flow<List<Alarm>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = alarms
            .whereArrayContains("participantIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AlarmRepository", "Erreur Firestore snapshot listener: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(Alarm::class.java).orEmpty()
                trySend(list.sortedWith(compareBy({ it.hour }, { it.minute })))
            }
        awaitClose { registration.remove() }
    }

    suspend fun createAlarm(
        hour: Int,
        minute: Int,
        name: String,
        days: List<Int>,
        soundName: String,
        vibrate: Boolean,
        contacts: List<String>
    ): Result<String> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Utilisateur non connecté")
        val allParticipants = (listOf(uid) + contacts).distinct()
        val docRef = alarms.document()
        docRef.set(
            mapOf(
                "ownerId" to uid,
                "hour" to hour,
                "minute" to minute,
                "enabled" to true,
                "name" to name,
                "days" to days,
                "soundName" to soundName,
                "vibrate" to vibrate,
                "participantIds" to allParticipants,
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        docRef.id
    }

    suspend fun updateAlarm(
        alarmId: String,
        hour: Int,
        minute: Int,
        name: String,
        days: List<Int>,
        soundName: String,
        vibrate: Boolean,
        contacts: List<String>
    ): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Utilisateur non connecté")

        val allParticipants = (listOf(uid) + contacts).distinct()
        alarms.document(alarmId).update(
            mapOf(
                "hour" to hour,
                "minute" to minute,
                "name" to name,
                "days" to days,
                "soundName" to soundName,
                "vibrate" to vibrate,
                "participantIds" to allParticipants
            )
        ).await()
        Unit
    }

    suspend fun setEnabled(alarmId: String, enabled: Boolean): Result<Unit> = runCatching {
        alarms.document(alarmId).update("enabled", enabled).await()
        Unit
    }

    suspend fun deleteAlarm(alarmId: String): Result<Unit> = runCatching {
        alarms.document(alarmId).delete().await()
        Unit
    }
}
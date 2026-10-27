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
    private val invitations get() = db.collection("alarm_invitations")

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

    fun alarmInvitationsFlow(): Flow<List<AlarmInvitation>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = invitations
            .whereEqualTo("receiverId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("AlarmRepository", "Erreur snapshot invitations: ${error.message}", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.toObjects(AlarmInvitation::class.java).orEmpty()
                    .filter { it.status == "pending" }
                Log.d("AlarmRepository", "Invitations recues pour $uid: ${list.size}")
                trySend(list)
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
        val currentUser = auth.currentUser
        val senderName = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")
            ?: "Un ami"

        Log.d("AlarmRepository", "createAlarm appelée avec ${contacts.size} contacts: $contacts")

        // Seul le créateur est directement dans participantIds
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
                "participantIds" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()

        // Envoi des invitations pour chaque contact sélectionné
        contacts.filter { it != uid }.forEach { contactUid ->
            try {
                sendAlarmInvitation(
                    alarmId = docRef.id,
                    alarmName = name,
                    alarmHour = hour,
                    alarmMinute = minute,
                    senderId = uid,
                    senderName = senderName,
                    receiverId = contactUid
                )
            } catch (e: Exception) {
                Log.e("AlarmRepository", "Erreur d'invitation (vérifier règles Firestore): ${e.message}", e)
            }
        }

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
        val currentUser = auth.currentUser
        val senderName = currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")
            ?: "Un ami"

        @Suppress("UNCHECKED_CAST")
        val doc = alarms.document(alarmId).get().await()
        val currentParticipants = (doc.get("participantIds") as? List<String>).orEmpty()

        alarms.document(alarmId).update(
            mapOf(
                "hour" to hour,
                "minute" to minute,
                "name" to name,
                "days" to days,
                "soundName" to soundName,
                "vibrate" to vibrate
            )
        ).await()

        contacts.filter { it != uid && !currentParticipants.contains(it) }.forEach { contactUid ->
            try {
                sendAlarmInvitation(
                    alarmId = alarmId,
                    alarmName = name,
                    alarmHour = hour,
                    alarmMinute = minute,
                    senderId = uid,
                    senderName = senderName,
                    receiverId = contactUid
                )
            } catch (e: Exception) {
                Log.e("AlarmRepository", "Erreur d'invitation lors de updateAlarm: ${e.message}", e)
            }
        }
        Unit
    }

    private suspend fun sendAlarmInvitation(
        alarmId: String,
        alarmName: String,
        alarmHour: Int,
        alarmMinute: Int,
        senderId: String,
        senderName: String,
        receiverId: String
    ) {
        val invitationId = "${alarmId}_$receiverId"
        Log.d("AlarmRepository", "Tentative d'envoi d'invitation: id=$invitationId à receiverId=$receiverId pour alarme=$alarmName")
        invitations.document(invitationId).set(
            mapOf(
                "alarmId" to alarmId,
                "alarmName" to alarmName,
                "alarmHour" to alarmHour,
                "alarmMinute" to alarmMinute,
                "senderId" to senderId,
                "senderName" to senderName,
                "receiverId" to receiverId,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Log.d("AlarmRepository", "Invitation enregistrée dans Firestore: id=$invitationId")
    }

    suspend fun acceptAlarmInvitation(invitation: AlarmInvitation): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Utilisateur non connecté")
        alarms.document(invitation.alarmId).update(
            "participantIds", FieldValue.arrayUnion(uid)
        ).await()
        invitations.document(invitation.id).update("status", "accepted").await()
        Unit
    }

    suspend fun declineAlarmInvitation(invitationId: String): Result<Unit> = runCatching {
        invitations.document(invitationId).update("status", "declined").await()
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
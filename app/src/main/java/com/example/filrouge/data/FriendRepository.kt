package com.example.filrouge.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class Friendship(
    @DocumentId val id: String = "",
    val userIds: List<String> = emptyList(),
    val requesterId: String = "",
    val status: String = "pending"
) {
    fun otherUid(me: String): String = userIds.first { it != me }
}

class FriendRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val col get() = db.collection("friendships")

    private fun pairId(a: String, b: String) = listOf(a, b).sorted().joinToString("_")

    fun friendshipsFlow(): Flow<List<Friendship>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = col
            .whereArrayContains("userIds", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Friendship::class.java).orEmpty())
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendRequest(otherUid: String): Result<Unit> = runCatching {
        val me = auth.currentUser?.uid ?: error("Utilisateur non connecté")
        require(me != otherUid) { "Impossible de s'ajouter soi-même" }
        col.document(pairId(me, otherUid)).set(
            mapOf(
                "userIds" to listOf(me, otherUid),
                "requesterId" to me,
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }

    suspend fun accept(friendshipId: String): Result<Unit> = runCatching {
        col.document(friendshipId).update("status", "accepted").await()
        Unit
    }

    suspend fun remove(friendshipId: String): Result<Unit> = runCatching {
        col.document(friendshipId).delete().await()
        Unit
    }
}
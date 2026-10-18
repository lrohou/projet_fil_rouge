package com.example.filrouge.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val isLoggedIn: Boolean get() = auth.currentUser != null

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    suspend fun signUp(displayName: String, email: String, password: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        db.collection("users").document(uid).set(
            mapOf("displayName" to displayName, "email" to email)
        ).await()
        Unit
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user!!

        if (result.additionalUserInfo?.isNewUser == true) {
            db.collection("users").document(user.uid).set(
                mapOf(
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: "")
                )
            ).await()
        }
        Unit
    }

    fun signOut() = auth.signOut()
}

fun Throwable.toAuthMessage(): String = when (this) {
    is FirebaseAuthWeakPasswordException -> "Mot de passe trop faible (6 caractères minimum)"
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> "E-mail ou mot de passe incorrect"
    is FirebaseAuthUserCollisionException -> "Un compte existe déjà avec cet e-mail"
    is FirebaseNetworkException -> "Pas de connexion internet"
    else -> "Une erreur est survenue, réessaie"
}
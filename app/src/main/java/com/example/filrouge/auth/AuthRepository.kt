package com.example.filrouge.auth

import android.content.Context
import com.example.filrouge.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repository gérant l'authentification des utilisateurs via Firebase Auth
 * et la création initiale de la fiche utilisateur dans Firestore.
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Indique si un utilisateur est actuellement authentifié */
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /** Authentification e-mail / mot de passe */
    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
        Unit
    }

    /** Inscription par e-mail et création de la fiche utilisateur dans Firestore */
    suspend fun signUp(displayName: String, email: String, password: String): Result<Unit> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        db.collection("users").document(uid).set(
            mapOf("displayName" to displayName, "email" to email)
        ).await()
        Unit
    }

    /** Envoie un e-mail de réinitialisation de mot de passe */
    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    /** Connexion via Google */
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

    /** Déconnecte l'utilisateur courant */
    fun signOut() = auth.signOut()
}

/** Extension traduisant les exceptions Firebase Auth en identifiant de ressource string */
fun Throwable.toAuthMessageResId(): Int = when (this) {
    is FirebaseAuthWeakPasswordException -> R.string.error_auth_weak_password
    is FirebaseAuthInvalidUserException,
    is FirebaseAuthInvalidCredentialsException -> R.string.error_auth_invalid_credentials
    is FirebaseAuthUserCollisionException -> R.string.error_auth_user_collision
    is FirebaseNetworkException -> R.string.error_auth_network
    else -> R.string.error_auth_unknown
}

fun Throwable.toAuthMessage(context: Context): String = context.getString(toAuthMessageResId())

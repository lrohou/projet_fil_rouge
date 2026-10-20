package com.example.filrouge.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = ""
)

data class ContactsUiState(
    val users: List<AppUser> = emptyList(),
    val filteredUsers: List<AppUser> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val friendRequestsSent: Set<String> = emptySet(),
    val error: String? = null
)

class ContactsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState(isLoading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private var usersListener: ListenerRegistration? = null

    init {
        listenToUsers()
    }

    override fun onCleared() {
        super.onCleared()
        usersListener?.remove()
    }

    fun fetchUsers() {
        listenToUsers()
    }

    fun listenToUsers() {
        val currentUid = auth.currentUser?.uid
        Log.d("ContactsVM", "listenToUsers() called. currentUid: $currentUid")

        if (currentUid == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Utilisateur non connecté"
            )
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        usersListener?.remove()
        usersListener = db.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ContactsVM", "Erreur Firestore lors de la récupération des users: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.localizedMessage ?: error.message ?: "Erreur de permissions Firestore"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("ContactsVM", "Documents reçus: ${snapshot.size()}")
                    val allUsers = snapshot.documents.mapNotNull { doc ->
                        val uid = doc.id
                        val displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("email")?.substringBefore("@")
                            ?: "Utilisateur"
                        val email = doc.getString("email") ?: ""
                        Log.d("ContactsVM", "User trouvé -> id: $uid, name: $displayName, email: $email")
                        AppUser(uid = uid, displayName = displayName, email = email)
                    }.filter { user ->
                        //tous les users sauf le user connecté
                        val isCurrentUser = (user.uid == currentUid)
                        if (isCurrentUser) {
                            Log.d("ContactsVM", "Exclusion de l'utilisateur connecté: ${user.uid}")
                        }
                        !isCurrentUser
                    }.sortedBy { it.displayName.lowercase() }

                    Log.d("ContactsVM", "Total users affichables (hors utilisateur courant): ${allUsers.size}")

                    val currentQuery = _uiState.value.searchQuery
                    val filtered = applyFilter(allUsers, currentQuery)

                    _uiState.value = _uiState.value.copy(
                        users = allUsers,
                        filteredUsers = filtered,
                        isLoading = false,
                        error = null
                    )
                } else {
                    Log.w("ContactsVM", "Snapshot est null")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = applyFilter(_uiState.value.users, query)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredUsers = filtered
        )
    }

    fun sendFriendRequest(userId: String) {
        //action fictive de demande d'ami
        val updatedSet = _uiState.value.friendRequestsSent + userId
        _uiState.value = _uiState.value.copy(friendRequestsSent = updatedSet)
    }

    private fun applyFilter(users: List<AppUser>, query: String): List<AppUser> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return users
        return users.filter { user ->
            user.displayName.lowercase().contains(q) || user.email.lowercase().contains(q)
        }
    }
}

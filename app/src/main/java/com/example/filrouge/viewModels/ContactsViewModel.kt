package com.example.filrouge.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.filrouge.data.FriendRepository
import com.example.filrouge.data.Friendship
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUser(
    val uid: String = "",
    val displayName: String = "",
    val email: String = ""
)

data class FriendRequest(
    val friendshipId: String,
    val from: AppUser
)

data class ContactsUiState(
    val users: List<AppUser> = emptyList(),
    val filteredUsers: List<AppUser> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val friendRequestsSent: Set<String> = emptySet(),
    val friendIds: Set<String> = emptySet(),
    val incomingRequests: List<FriendRequest> = emptyList(),
    val error: String? = null
)

class ContactsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val friendRepo: FriendRepository = FriendRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState(isLoading = true))
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private var usersListener: ListenerRegistration? = null
    private var friendships: List<Friendship> = emptyList()

    init {
        listenToUsers()
        viewModelScope.launch {
            friendRepo.friendshipsFlow()
                .catch { Log.e("ContactsVM", "Erreur friendships", it) }
                .collect { list ->
                    friendships = list
                    updateFriendState()
                }
        }
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
                    Log.e("ContactsVM", "Erreur Firestore: ${error.message}", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.localizedMessage ?: error.message ?: "Erreur de permissions Firestore"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allUsers = snapshot.documents.mapNotNull { doc ->
                        val displayName = doc.getString("displayName")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("email")?.substringBefore("@")
                            ?: "Utilisateur"
                        AppUser(
                            uid = doc.id,
                            displayName = displayName,
                            email = doc.getString("email") ?: ""
                        )
                    }
                        .filter { it.uid != currentUid }   // tous les users sauf le user connecté
                        .sortedBy { it.displayName.lowercase() }

                    Log.d("ContactsVM", "Total users affichables: ${allUsers.size}")

                    val filtered = applyFilter(allUsers, _uiState.value.searchQuery)

                    _uiState.value = _uiState.value.copy(
                        users = allUsers,
                        filteredUsers = filtered,
                        isLoading = false,
                        error = null
                    )
                    // Les noms des demandeurs se résolvent à partir de cette liste
                    updateFriendState()
                } else {
                    Log.w("ContactsVM", "Snapshot est null")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }
    }

    private fun updateFriendState() {
        val me = auth.currentUser?.uid ?: return
        _uiState.update { state ->
            val sent = friendships
                .filter { it.status == "pending" && it.requesterId == me }
                .map { it.otherUid(me) }.toSet()
            val friends = friendships
                .filter { it.status == "accepted" }
                .map { it.otherUid(me) }.toSet()
            val incoming = friendships
                .filter { it.status == "pending" && it.requesterId != me }
                .map { f ->
                    FriendRequest(
                        friendshipId = f.id,
                        from = state.users.firstOrNull { it.uid == f.requesterId }
                            ?: AppUser(uid = f.requesterId, displayName = "Utilisateur")
                    )
                }
            state.copy(friendRequestsSent = sent, friendIds = friends, incomingRequests = incoming)
        }
    }

    fun onSearchQueryChange(query: String) {
        val filtered = applyFilter(_uiState.value.users, query)
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredUsers = filtered
        )
    }

    fun sendFriendRequest(userId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = friendRepo.sendRequest(userId)
            result.onFailure { Log.e("ContactsVM", "Demande échouée", it) }
            onResult(result.isSuccess)
        }
    }

    fun acceptRequest(friendshipId: String) {
        viewModelScope.launch { friendRepo.accept(friendshipId) }
    }

    fun refuseRequest(friendshipId: String) {
        viewModelScope.launch { friendRepo.remove(friendshipId) }
    }

    private fun applyFilter(users: List<AppUser>, query: String): List<AppUser> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return users
        return users.filter { user ->
            user.displayName.lowercase().contains(q) || user.email.lowercase().contains(q)
        }
    }
}
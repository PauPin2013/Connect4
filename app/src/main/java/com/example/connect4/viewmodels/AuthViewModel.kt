package com.example.connect4.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect4.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ViewModel for handling user authentication operations.
class AuthViewModel : ViewModel() {
    // Firebase Authentication instance.
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    // Firebase Firestore instance.
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // MutableStateFlow to hold the current authenticated user.
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    // Public StateFlow for observing the current user.
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // MutableStateFlow to hold any error messages.
    private val _error = MutableStateFlow<String?>(null)
    // Public StateFlow for observing error messages.
    val error: StateFlow<String?> = _error.asStateFlow()

    // MutableStateFlow to indicate loading state.
    private val _isLoading = MutableStateFlow(false)
    // Public StateFlow for observing loading state.
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Initializes the ViewModel, setting up an authentication state listener.
    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    /**
     * Registers a new user with email and password.
     * Creates a new user document in Firestore upon successful registration.
     * @param email User's email address.
     * @param password User's password.
     */
    fun register(email: String, password: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Create user with email and password.
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = result.user
                firebaseUser?.let { user ->
                    // Create a new User object and save to Firestore.
                    val newUser = User(uid = user.uid, email = user.email ?: "", username = user.email?.split("@")?.get(0) ?: "Player")
                    db.collection("users").document(user.uid).set(newUser).await()
                }
                _currentUser.value = auth.currentUser
            } catch (e: Exception) {
                // Set error message if registration fails.
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Logs in a user with email and password.
     * @param email User's email address.
     * @param password User's password.
     */
    fun login(email: String, password: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                // Sign in user with email and password.
                auth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = auth.currentUser
            } catch (e: Exception) {
                // Set error message if login fails.
                _error.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Logs out the current user.
    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            _currentUser.value = null
        }
    }

    // Clears any current error messages.
    fun clearError() {
        _error.value = null
    }
}
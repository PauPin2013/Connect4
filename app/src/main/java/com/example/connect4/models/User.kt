package com.example.connect4.models

import com.google.firebase.firestore.DocumentId

// Represents a user in the application.
data class User(
    @DocumentId
    val uid: String = "", // Unique user ID from Firebase Authentication.
    val email: String = "",    // User's email address.
    val username: String = "", // User's chosen username.
    val score: Int = 0         // User's score in the game.
)
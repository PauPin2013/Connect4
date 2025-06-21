package com.example.connect4.models

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val score: Int = 0
)
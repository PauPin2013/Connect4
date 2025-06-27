package com.example.connect4.models

import com.google.firebase.firestore.DocumentId

data class VocabularyWord(
    @DocumentId
    val id: String = "",
    val word: String = "",
    val translation: String = ""
)
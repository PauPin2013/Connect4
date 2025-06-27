package com.example.connect4.models

import com.google.firebase.firestore.DocumentId

// Represents a vocabulary word for the game.
data class VocabularyWord(
    @DocumentId
    val id: String = "",        // Unique ID for the vocabulary word.
    val word: String = "",      // The word in the original language.
    val translation: String = "" // The translation of the word.
)
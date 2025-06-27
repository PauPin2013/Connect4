package com.example.connect4.models

import org.junit.Assert.assertEquals
import org.junit.Test

// This is the unit test class for your 'VocabularyWord' data class.
class VocabularyWordTest {

    /**
     * Test to verify that the properties of a 'VocabularyWord' object are correctly assigned
     * when a constructor with specific values is provided.
     */
    @Test
    fun testVocabularyWordCreation() {
        // 1. Create an instance of VocabularyWord with example values.
        val word = VocabularyWord(id = "1", word = "Hello", translation = "Hola")

        // 2. Assert that the properties are correctly assigned.
        assertEquals("1", word.id)
        assertEquals("Hello", word.word)
        assertEquals("Hola", word.translation)
    }

    /**
     * Test to verify that the properties of a 'VocabularyWord' object take their default values
     * when an instance is created without providing constructor arguments.
     */
    @Test
    fun testVocabularyWordDefaultValues() {
        // 1. Create an instance of VocabularyWord using the no-argument constructor,
        //    which will assign the default values defined in the data class.
        val word = VocabularyWord()

        // 2. Assert that the default values are correctly assigned.
        assertEquals("", word.id)
        assertEquals("", word.word)
        assertEquals("", word.translation)
    }
}
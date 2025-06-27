package com.example.connect4 // In the root package

// Import necessary JUnit classes
import org.junit.Assert.assertEquals
import org.junit.Test

// Import the VocabularyWord class from its actual location
import com.example.connect4.models.VocabularyWord

// New test class: WordDataTest
class WordDataTest {

    /**
     * Test to verify that the properties of a 'VocabularyWord' object are correctly assigned
     * when a constructor with specific values is provided.
     */
    @Test
    fun verifyWordProperties() { // New name for the test method
        // Create an instance of VocabularyWord.
        val myWord = VocabularyWord(id = "alpha", word = "Árbol", translation = "Tree")

        // Verify that the values are correctly assigned.
        assertEquals("alpha", myWord.id)
        assertEquals("Árbol", myWord.word)
        assertEquals("Tree", myWord.translation)
    }

    /**
     * Test to verify that the properties of a 'VocabularyWord' object take their default values
     * when an instance is created without providing constructor arguments.
     */
    @Test
    fun verifyDefaultWordValues() { // New name for the test method
        // Create an instance of VocabularyWord without passing values.
        val defaultWord = VocabularyWord()

        // Verify that the default values are correct.
        assertEquals("", defaultWord.id)
        assertEquals("", defaultWord.word)
        assertEquals("", defaultWord.translation)
    }
}
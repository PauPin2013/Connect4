package com.example.connect4 // In the root package, as requested

// Import necessary JUnit classes for assertions and test handling.
import org.junit.Assert.assertEquals
import org.junit.Test

// Import the 'User' class from its actual location in your application's 'models' module.
import com.example.connect4.models.User

// This is the unit test class for your 'User' data class.
// The name 'UserDataTest' is the one previously agreed upon.
class UserDataTest {

    /**
     * Test to verify that the properties of a 'User' object are correctly assigned
     * when a constructor with specific values is provided.
     */
    @Test
    fun verifyUserProperties() { // Test method name revised for clarity
        // 1. Create an instance of 'User' with example values.
        val newUser = User(uid = "beta", email = "user@domain.com", username = "Player2", score = 250)

        // 2. Use assertions to confirm that each property of 'newUser'
        //    matches the value passed during its creation.
        assertEquals("beta", newUser.uid)
        assertEquals("user@domain.com", newUser.email)
        assertEquals("Player2", newUser.username)
        assertEquals(250, newUser.score)
    }

    /**
     * Test to verify that the properties of a 'User' object take their default values
     * when an instance is created without providing constructor arguments.
     */
    @Test
    fun verifyDefaultUserValues() { // Test method name revised
        // 1. Create an instance of 'User' using the no-argument constructor,
        //    which will assign the default values defined in the data class.
        val defaultUser = User()

        // 2. Use assertions to confirm that the properties of 'defaultUser'
        //    have the expected default values (empty strings for 'uid', 'email', 'username' and 0 for 'score').
        assertEquals("", defaultUser.uid)
        assertEquals("", defaultUser.email)
        assertEquals("", defaultUser.username)
        assertEquals(0, defaultUser.score)
    }
}
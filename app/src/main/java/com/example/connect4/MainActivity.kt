package com.example.connect4

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.connect4.views.Connect4GameScreen // Import the new game screen composable

// MainActivity sets up the app's UI and starts the game
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The Connect4GameScreen will now handle its own ViewModel creation
            Connect4GameScreen()
        }
    }
}

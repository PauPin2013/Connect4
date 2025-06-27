package com.example.connect4.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect4.viewmodels.OnlineConnect4ViewModel
import com.example.connect4.viewmodels.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLobbyScreen(
    onStartOfflineGame: () -> Unit, // Callback to start an offline game.
    onStartOnlineGame: (String) -> Unit, // Callback to start an online game, with gameId.
    onLogout: () -> Unit,
    onlineConnect4ViewModel: OnlineConnect4ViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    // State for the game ID input field.
    var gameIdInput by remember { mutableStateOf("") }
    // Collect loading state from the OnlineConnect4ViewModel.
    val isLoading by onlineConnect4ViewModel.isLoading.collectAsState()
    // Collect message state from the OnlineConnect4ViewModel.
    val message by onlineConnect4ViewModel.message.collectAsState()
    // Collect online game state from the OnlineConnect4ViewModel.
    val onlineGame by onlineConnect4ViewModel.onlineGame.collectAsState()

    // SnackbarHostState for displaying messages.
    val snackbarHostState = remember { SnackbarHostState() }
    // Coroutine scope for launching snackbar operations.
    val scope = rememberCoroutineScope()

    // Effect to show messages in a snackbar.
    LaunchedEffect(message) {
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Close",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Effect to navigate to the online game screen when a game starts.
    LaunchedEffect(onlineGame) {
        onlineGame?.let {
            if (it.status == "playing" && it.gameId.isNotBlank()) {
                onStartOnlineGame(it.gameId) // Pass the gameId when the game starts.
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF2C3E50)) // Dark blue background.
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // General lobby title.
            Text(
                text = "Connect 4 Lobby",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Offline Play Section.
            Button(
                onClick = {
                    onStartOfflineGame()
                },
                enabled = !isLoading, // Disable buttons while an online operation is loading.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)), // Green button. [Image of a green button]
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Play Offline", fontSize = 18.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(24.dp)) // Space between offline and online sections.

            // Online Game Section Title.
            Text(
                text = "Online Game",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Button to Create Online Game.
            Button(
                onClick = { onlineConnect4ViewModel.createOnlineGame() },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)), // Blue button. [Image of a blue button]
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading && onlineGame == null) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Create Game", fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Field and button to Join Game.
            OutlinedTextField(
                value = gameIdInput,
                onValueChange = { gameIdInput = it },
                label = { Text("Game ID", color = Color.White) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF9B59B6), // Purple for focused border.
                    unfocusedBorderColor = Color.LightGray,
                    cursorColor = Color(0xFF9B59B6),
                    focusedLabelColor = Color(0xFF9B59B6),
                    unfocusedLabelColor = Color.LightGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF34495E),
                    unfocusedContainerColor = Color(0xFF34495E)
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Button(
                onClick = { onlineConnect4ViewModel.joinOnlineGame(gameIdInput) },
                enabled = !isLoading && gameIdInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9B59B6)), // Purple button. [Image of a purple button]
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading && onlineGame != null) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Join Game", fontSize = 18.sp, color = Color.White)
                }
            }

            // Display game ID if a game is waiting for an opponent.
            onlineGame?.let { game ->
                if (game.status == "waiting") {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Your Game ID: ${game.gameId}",
                        color = Color(0xFFF1C40F), // Yellow color.
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Share this ID with your opponent.",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Logout Button.
            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)), // Red button. [Image of a red button]
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Log Out", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
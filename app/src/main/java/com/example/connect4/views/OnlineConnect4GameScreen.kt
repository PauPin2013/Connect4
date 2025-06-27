package com.example.connect4.views

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect4.models.GameState
import com.example.connect4.viewmodels.OnlineConnect4ViewModel
import com.example.connect4.views.shared.BoardView // Import the shared BoardView.
import com.example.connect4.views.shared.PlayerInfo // Import the shared PlayerInfo.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineConnect4GameScreen(
    gameId: String?,
    onNavigateBackToLobby: () -> Unit,
    onlineConnect4ViewModel: OnlineConnect4ViewModel = viewModel()
) {
    // Collect states from the OnlineConnect4ViewModel.
    val board by onlineConnect4ViewModel.board.collectAsState()
    val gameState by onlineConnect4ViewModel.gameState.collectAsState()
    val currentUserId by onlineConnect4ViewModel.currentUserId.collectAsState()
    val onlineGame by onlineConnect4ViewModel.onlineGame.collectAsState()
    val isMyTurn by onlineConnect4ViewModel.isMyTurn.collectAsState()
    val isLoading by onlineConnect4ViewModel.isLoading.collectAsState()
    val answerAttempt by onlineConnect4ViewModel.answerAttempt.collectAsState()

    // Start listening for game updates when the gameId is available.
    LaunchedEffect(gameId) {
        if (!gameId.isNullOrBlank()) {
            onlineConnect4ViewModel.startListeningForGameUpdates(gameId)
        } else {
            Log.e("OnlineConnect4GameScreen", "Game ID is null or blank. Cannot start listener.")
            // Consider navigating back to the lobby or showing an error here.
        }
    }

    // Stop listening for game updates when the composable leaves the composition.
    DisposableEffect(Unit) {
        onDispose {
            onlineConnect4ViewModel.stopListeningForGameUpdates()
        }
    }

    // Determine the message to display based on the game state.
    val displayMessage = when (gameState) {
        is GameState.Winner -> {
            val winnerNum = (gameState as GameState.Winner).player
            val winnerName = if (winnerNum == 1) onlineGame?.player1Name else onlineGame?.player2Name
            "${winnerName ?: "Player $winnerNum"} has won!"
        }
        GameState.Draw -> "It's a draw!"
        is GameState.AskingQuestion -> {
            // Check if the current user is the one asked the question.
            val isQuestionForMe = onlineGame?.questionAskedToPlayerId == currentUserId
            if (isQuestionForMe) {
                "It's your turn! Translate: \"${(gameState as GameState.AskingQuestion).word}\""
            } else {
                "Turn of ${
                    if (onlineGame?.player1Id == currentUserId) onlineGame?.player2Name else onlineGame?.player1Name
                }. Waiting for answer..."
            }
        }
        GameState.Playing -> {
            if (isMyTurn) "It's your turn! Make your move."
            else "Turn of ${
                if (onlineGame?.player1Id == currentUserId) onlineGame?.player2Name else onlineGame?.player1Name
            }..."
        }
        GameState.WaitingToStart -> {
            onlineGame?.let { game ->
                if (game.player1Id == currentUserId) "Waiting for opponent for game ${game.gameId}..."
                else "Waiting for Player 1 to start the game..."
            } ?: "Preparing game..."
        }
        else -> "..."
    }

    // Determine the text color for the message.
    val textColor = when (gameState) {
        is GameState.Winner -> if ((gameState as GameState.Winner).player == 1) Color.Red else Color.Yellow
        GameState.Draw -> Color.Gray
        else -> Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect 4 (Online)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToLobby) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Lobby", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2C3E50)) // Dark blue top app bar.
            )
        },
        containerColor = Color(0xFF2C3E50) // Dark blue screen background.
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(48.dp))
                Text("Loading...", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Display game status message.
                    Text(
                        text = displayMessage,
                        color = textColor,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    onlineGame?.let { game ->
                        // Player information.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlayerInfo(
                                playerName = game.player1Name ?: "Player 1",
                                isCurrentPlayer = game.currentPlayerId == game.player1Id,
                                isMe = currentUserId == game.player1Id,
                                playerColor = Color.Red
                            )
                            Text("vs", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                            PlayerInfo(
                                playerName = game.player2Name ?: "Player 2",
                                isCurrentPlayer = game.currentPlayerId == game.player2Id,
                                isMe = currentUserId == game.player2Id,
                                playerColor = Color.Yellow
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Game ID: ${game.gameId}",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Question/Answer Section.
                // This section should only show if it's MY turn AND I'm the one being asked the question.
                if (gameState is GameState.AskingQuestion && onlineGame?.questionAskedToPlayerId == currentUserId) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Translate: \"${(gameState as GameState.AskingQuestion).word}\"",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = answerAttempt,
                            onValueChange = { onlineConnect4ViewModel.setAnswerAttempt(it) },
                            label = { Text("Your answer", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF3498DB),
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = Color(0xFF3498DB),
                                focusedLabelColor = Color(0xFF3498DB),
                                unfocusedLabelColor = Color.LightGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF34495E),
                                unfocusedContainerColor = Color(0xFF34495E)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onlineConnect4ViewModel.submitAnswer() },
                            enabled = answerAttempt.isNotBlank() && !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)), // Green button.
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Submit Answer", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Connect4 game board.
                BoardView(
                    board = board.cells,
                    onColumnClick = { col ->
                        val isOnlineGameWaiting = (onlineGame != null && onlineGame?.status == "waiting")
                        // Only allow dropping a piece if it's my turn AND the question was answered correctly (or no active question).
                        val canDropPiece = isMyTurn &&
                                (onlineGame?.currentQuestionWord == null || onlineGame?.questionAnsweredCorrectly == true) &&
                                onlineGame?.status == "playing"

                        when {
                            onlineGame != null && canDropPiece && !isOnlineGameWaiting -> {
                                Log.d("OnlineConnect4GameScreen", "Attempting to drop piece in column $col. Current User: $currentUserId, My Turn: $isMyTurn, Question Answered Correctly: ${onlineGame?.questionAnsweredCorrectly}")
                                onlineConnect4ViewModel.dropPiece(col)
                            }
                            onlineGame != null && !isMyTurn -> {
                                Log.d("OnlineConnect4GameScreen", "It's not your turn. Attempted move in column $col. Current User: $currentUserId, My Turn: $isMyTurn")
                                // Optional: show a message to the user that it's not their turn.
                            }
                            onlineGame != null && isOnlineGameWaiting -> {
                                Log.d("OnlineConnect4GameScreen", "Waiting for the second player. Attempted move in column $col.")
                                // Optional: show a message that the game is waiting.
                            }
                            onlineGame != null && onlineGame?.currentQuestionWord != null && onlineGame?.questionAnsweredCorrectly != true && isMyTurn -> {
                                Log.d("OnlineConnect4GameScreen", "You must answer the question first. Attempted move in column $col. Current User: $currentUserId, My Turn: $isMyTurn, Question Answered Correctly: ${onlineGame?.questionAnsweredCorrectly}")
                                // Optional: show a message that they must answer the question.
                            }
                            else -> {
                                Log.d("OnlineConnect4GameScreen", "Cannot make a move at this time. Current state: $gameState. Attempt in column $col.")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                onlineGame?.let { game ->
                    val isCreator = currentUserId == game.player1Id
                    when (gameState) {
                        GameState.WaitingToStart -> {
                            if (isCreator) { // Only the creator can cancel the online game.
                                Button(
                                    onClick = { onlineConnect4ViewModel.deleteOnlineGame() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)), // Red button.
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancel Game", fontSize = 18.sp, color = Color.White)
                                }
                            } else {
                                // Player 2 who joined, just waits or leaves.
                                Button(
                                    onClick = { onlineConnect4ViewModel.deleteOnlineGame() }, // Allow the second player to leave.
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Leave Game", fontSize = 18.sp, color = Color.White)
                                }
                            }
                        }
                        is GameState.Winner, GameState.Draw -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (isCreator) { // Only the creator can reset the online game.
                                    Button(
                                        onClick = { onlineConnect4ViewModel.resetOnlineGame() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .padding(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)), // Green button.
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Play Again", fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
                                    }
                                }
                                Button(
                                    onClick = onNavigateBackToLobby,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)), // Blue button.
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Back to Lobby", fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        else -> { // GameState.Playing or GameState.AskingQuestion.
                            Button(
                                onClick = { onlineConnect4ViewModel.deleteOnlineGame() }, // Allow leaving mid-game.
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Leave Game", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
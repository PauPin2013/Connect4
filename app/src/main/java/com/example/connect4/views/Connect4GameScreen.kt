package com.example.connect4.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect4.models.GameState
import com.example.connect4.viewmodels.Connect4ViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Connect4GameScreen(
    onLogout: () -> Unit,
    connect4ViewModel: Connect4ViewModel = viewModel()
) {
    val board by connect4ViewModel.board.collectAsState()
    val currentPlayer by connect4ViewModel.currentPlayer.collectAsState()
    val gameState by connect4ViewModel.gameState.collectAsState()
    val currentUserId by connect4ViewModel.currentUserId.collectAsState()
    val onlineGame by connect4ViewModel.onlineGame.collectAsState()
    val isMyTurn by connect4ViewModel.isMyTurn.collectAsState()
    val message by connect4ViewModel.message.collectAsState()
    val questionAnsweredCorrectly by connect4ViewModel.questionAnsweredCorrectly.collectAsState()

    var userAnswer by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val displayMessage = when (gameState) {
        is GameState.Winner -> {
            val winnerNum = (gameState as GameState.Winner).player
            if (onlineGame != null) {
                val winnerName = if (winnerNum == 1) onlineGame?.player1Name else onlineGame?.player2Name
                "${winnerName ?: "Jugador $winnerNum"} ha ganado!"
            } else {
                "¡El Jugador $winnerNum ha ganado!"
            }
        }
        GameState.Draw -> "¡Es un empate!"
        is GameState.AskingQuestion -> {
            val word = (gameState as GameState.AskingQuestion).word
            if (onlineGame != null && onlineGame?.currentPlayerId != currentUserId) {
                "Esperando la respuesta de ${if (onlineGame?.player1Id == onlineGame?.currentPlayerId) onlineGame?.player1Name else onlineGame?.player2Name}..."
            } else {
                "¿Cuál es la traducción de '${word.word}'?"
            }
        }
        GameState.Playing -> {
            if (onlineGame != null) {
                if (isMyTurn) "¡Es tu turno!" else "Turno de ${if (onlineGame?.player1Id == currentUserId) onlineGame?.player2Name else onlineGame?.player1Name}..."
            } else {
                "Turno del Jugador $currentPlayer"
            }
        }
        GameState.WaitingToStart -> {
            onlineGame?.let { game ->
                if (game.player1Id == currentUserId) "Esperando oponente para la partida ${game.gameId}..."
                else "Esperando que el jugador 1 inicie la partida..."
            } ?: "Preparando partida..."
        }
        GameState.Paused -> "Juego pausado"
    }

    val textColor = when (gameState) {
        is GameState.Winner -> if ((gameState as GameState.Winner).player == 1) Color.Red else Color.Yellow
        GameState.Draw -> Color.Gray
        else -> Color.White
    }

    LaunchedEffect(message) {
        message?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    actionLabel = "Cerrar",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Conecta 4", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2C3E50)),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF2C3E50))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayMessage,
                    color = textColor,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                onlineGame?.let { game ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerInfo(
                            playerName = game.player1Name ?: "Jugador 1",
                            isCurrentPlayer = game.currentPlayerId == game.player1Id,
                            isMe = currentUserId == game.player1Id,
                            playerColor = Color.Red
                        )
                        Text("vs", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                        PlayerInfo(
                            playerName = game.player2Name ?: "Jugador 2",
                            isCurrentPlayer = game.currentPlayerId == game.player2Id,
                            isMe = currentUserId == game.player2Id,
                            playerColor = Color.Yellow
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "ID Partida: ${game.gameId}",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            BoardView(
                board = board.cells,
                onColumnClick = { col ->
                    val canDropPieceAfterQuestion = (questionAnsweredCorrectly == true)
                    val isMyOnlineTurn = (onlineGame != null && isMyTurn)
                    val isOnlineGameWaiting = (onlineGame != null && onlineGame?.status == "waiting")

                    when {
                        gameState is GameState.AskingQuestion && isMyOnlineTurn -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Por favor, responde la pregunta de vocabulario primero.",
                                    actionLabel = "Cerrar"
                                )
                            }
                        }
                        isMyOnlineTurn && canDropPieceAfterQuestion -> {
                            connect4ViewModel.proceedWithDropPiece(col)
                        }
                        isOnlineGameWaiting -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Esperando al segundo jugador para iniciar la partida...",
                                    actionLabel = "Cerrar"
                                )
                            }
                        }
                        onlineGame != null && !isMyOnlineTurn -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "No es tu turno.",
                                    actionLabel = "Cerrar"
                                )
                            }
                        }
                        onlineGame == null && gameState == GameState.Playing -> {
                            connect4ViewModel.dropPiece(col)
                        }
                        onlineGame != null && questionAnsweredCorrectly == false -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Respondiste incorrectamente. Tu turno fue pasado.",
                                    actionLabel = "Cerrar"
                                )
                            }
                        }
                        else -> {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "No se puede hacer un movimiento en este momento.",
                                    actionLabel = "Cerrar"
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = gameState is GameState.AskingQuestion && (onlineGame == null || (onlineGame != null && isMyTurn)),
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(durationMillis = 500)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(durationMillis = 500))
            ) {
                val currentQuestion = (gameState as? GameState.AskingQuestion)?.word
                if (currentQuestion != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF34495E))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Traduce: '${currentQuestion.word}'",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = userAnswer,
                            onValueChange = { userAnswer = it },
                            label = { Text("Tu respuesta", color = Color.LightGray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF2ECC71),
                                unfocusedBorderColor = Color.Gray,
                                cursorColor = Color(0xFF2ECC71),
                                focusedLabelColor = Color(0xFF2ECC71),
                                unfocusedLabelColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF4A6572),
                                unfocusedContainerColor = Color(0xFF4A6572)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                connect4ViewModel.checkQuestionAnswer(userAnswer)
                                userAnswer = ""
                            },
                            enabled = userAnswer.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71))
                        ) {
                            Text("Enviar respuesta", color = Color.White)
                        }
                        questionAnsweredCorrectly?.let { isCorrect ->
                            Text(
                                text = if (isCorrect) "¡Correcto!" else "Incorrecto.",
                                color = if (isCorrect) Color.Green else Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (gameState is GameState.Winner || gameState == GameState.Draw || onlineGame == null) {
                    Button(
                        onClick = { connect4ViewModel.resetGame() },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reiniciar", color = Color.White)
                    }
                } else if (onlineGame != null && onlineGame?.player1Id == currentUserId && onlineGame?.status != "playing") {
                    Button(
                        onClick = { connect4ViewModel.resetOnlineGame() },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reiniciar Partida Online", textAlign = TextAlign.Center, color = Color.White)
                    }
                }

                if (onlineGame != null && onlineGame?.player1Id == currentUserId) {
                    Button(
                        onClick = { connect4ViewModel.deleteOnlineGame() },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC0392B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Eliminar Partida", textAlign = TextAlign.Center, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerInfo(playerName: String, isCurrentPlayer: Boolean, isMe: Boolean, playerColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = playerName + if (isMe) " (Tú)" else "",
            color = if (isCurrentPlayer) playerColor else Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            fontWeight = if (isCurrentPlayer) FontWeight.Bold else FontWeight.Normal
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(playerColor, CircleShape)
                .border(2.dp, if (isCurrentPlayer) Color.White else Color.Transparent, CircleShape)
        )
    }
}

@Composable
fun BoardView(board: List<List<Int>>, onColumnClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF3498DB))
            .padding(8.dp)
    ) {
        board.forEachIndexed { rowIdx, row ->
            Row {
                row.forEachIndexed { colIdx, cell ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(48.dp)
                            .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
                            .clickable {
                                onColumnClick(colIdx)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    when (cell) {
                                        1 -> Color.Red
                                        2 -> Color.Yellow
                                        else -> Color.White
                                    },
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}

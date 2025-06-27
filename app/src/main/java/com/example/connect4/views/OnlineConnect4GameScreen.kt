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
import com.example.connect4.views.shared.BoardView // Importar el BoardView compartido
import com.example.connect4.views.shared.PlayerInfo // Importar el PlayerInfo compartido

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineConnect4GameScreen(
    gameId: String?,
    onNavigateBackToLobby: () -> Unit,
    onlineConnect4ViewModel: OnlineConnect4ViewModel = viewModel()
) {
    val board by onlineConnect4ViewModel.board.collectAsState()
    val gameState by onlineConnect4ViewModel.gameState.collectAsState()
    val currentUserId by onlineConnect4ViewModel.currentUserId.collectAsState()
    val onlineGame by onlineConnect4ViewModel.onlineGame.collectAsState()
    val isMyTurn by onlineConnect4ViewModel.isMyTurn.collectAsState()
    val isLoading by onlineConnect4ViewModel.isLoading.collectAsState()
    val answerAttempt by onlineConnect4ViewModel.answerAttempt.collectAsState()

    LaunchedEffect(gameId) {
        // Asegurarse de que gameId no sea nulo o vacío antes de iniciar el listener
        if (!gameId.isNullOrBlank()) {
            onlineConnect4ViewModel.startListeningForGameUpdates(gameId)
        } else {
            Log.e("OnlineConnect4GameScreen", "Game ID is null or blank. Cannot start listener.")
            // Aquí puedes considerar navegar de vuelta al lobby o mostrar un error
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onlineConnect4ViewModel.stopListeningForGameUpdates()
        }
    }

    val displayMessage = when (gameState) {
        is GameState.Winner -> {
            val winnerNum = (gameState as GameState.Winner).player
            val winnerName = if (winnerNum == 1) onlineGame?.player1Name else onlineGame?.player2Name
            "${winnerName ?: "Jugador $winnerNum"} ha ganado!"
        }
        GameState.Draw -> "¡Es un empate!"
        is GameState.AskingQuestion -> {
            // Check if the current user is the one asked the question
            val isQuestionForMe = onlineGame?.questionAskedToPlayerId == currentUserId
            if (isQuestionForMe) {
                "¡Es tu turno! Traduce: \"${(gameState as GameState.AskingQuestion).word}\""
            } else {
                "Turno de ${
                    if (onlineGame?.player1Id == currentUserId) onlineGame?.player2Name else onlineGame?.player1Name
                }. Esperando respuesta..."
            }
        }
        GameState.Playing -> {
            if (isMyTurn) "¡Es tu turno! Haz tu movimiento."
            else "Turno de ${
                if (onlineGame?.player1Id == currentUserId) onlineGame?.player2Name else onlineGame?.player1Name
            }..."
        }
        GameState.WaitingToStart -> {
            onlineGame?.let { game ->
                if (game.player1Id == currentUserId) "Esperando oponente para la partida ${game.gameId}..."
                else "Esperando que el jugador 1 inicie la partida..."
            } ?: "Preparando partida..."
        }
        else -> "..."
    }

    val textColor = when (gameState) {
        is GameState.Winner -> if ((gameState as GameState.Winner).player == 1) Color.Red else Color.Yellow
        GameState.Draw -> Color.Gray
        else -> Color.White
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conecta 4 (Online)", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToLobby) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver al Lobby", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2C3E50))
            )
        },
        containerColor = Color(0xFF2C3E50) // Fondo de la pantalla
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
                Text("Cargando...", color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp))
            } else {
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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Sección de Pregunta/Respuesta
                // This section should only show if it's MY turn AND I'm the one being asked the question
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
                            text = "Traduce: \"${(gameState as GameState.AskingQuestion).word}\"",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value = answerAttempt,
                            onValueChange = { onlineConnect4ViewModel.setAnswerAttempt(it) },
                            label = { Text("Tu respuesta", color = Color.White.copy(alpha = 0.7f)) },
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1ABC9C)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Enviar Respuesta", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                BoardView(
                    board = board.cells,
                    onColumnClick = { col ->
                        val isOnlineGameWaiting = (onlineGame != null && onlineGame?.status == "waiting")
                        // Solo permite soltar una pieza si es mi turno Y la pregunta fue respondida correctamente (o no hay pregunta activa)
                        val canDropPiece = isMyTurn &&
                                (onlineGame?.currentQuestionWord == null || onlineGame?.questionAnsweredCorrectly == true) &&
                                onlineGame?.status == "playing"

                        when {
                            onlineGame != null && canDropPiece && !isOnlineGameWaiting -> {
                                Log.d("OnlineConnect4GameScreen", "Intentando soltar ficha en columna $col. Usuario Actual: $currentUserId, Es Mi Turno: $isMyTurn, Pregunta Respondida Correctamente: ${onlineGame?.questionAnsweredCorrectly}")
                                onlineConnect4ViewModel.dropPiece(col)
                            }
                            onlineGame != null && !isMyTurn -> {
                                Log.d("OnlineConnect4GameScreen", "No es tu turno. Intento de movimiento en columna $col. Usuario Actual: $currentUserId, Es Mi Turno: $isMyTurn")
                                // Opcional: mostrar un mensaje al usuario de que no es su turno
                            }
                            onlineGame != null && isOnlineGameWaiting -> {
                                Log.d("OnlineConnect4GameScreen", "Esperando al segundo jugador. Intento de movimiento en columna $col.")
                                // Opcional: mostrar un mensaje de que la partida está esperando
                            }
                            onlineGame != null && onlineGame?.currentQuestionWord != null && onlineGame?.questionAnsweredCorrectly != true && isMyTurn -> {
                                Log.d("OnlineConnect4GameScreen", "Debes responder la pregunta primero. Intento de movimiento en columna $col. Usuario Actual: $currentUserId, Es Mi Turno: $isMyTurn, Pregunta Respondida Correctamente: ${onlineGame?.questionAnsweredCorrectly}")
                                // Opcional: mostrar un mensaje de que deben responder la pregunta
                            }
                            else -> {
                                Log.d("OnlineConnect4GameScreen", "No se puede hacer un movimiento en este momento. Estado actual: $gameState. Intento en columna $col.")
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                onlineGame?.let { game ->
                    val isCreator = currentUserId == game.player1Id
                    when (gameState) {
                        GameState.WaitingToStart -> {
                            if (isCreator) {
                                Button(
                                    onClick = { onlineConnect4ViewModel.deleteOnlineGame() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Cancelar Partida", fontSize = 18.sp, color = Color.White)
                                }
                            } else {
                                // Jugador 2 que se unió, solo espera o sale
                                Button(
                                    onClick = { onlineConnect4ViewModel.deleteOnlineGame() }, // Para que el segundo jugador pueda salir
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Salir de la Partida", fontSize = 18.sp, color = Color.White)
                                }
                            }
                        }
                        is GameState.Winner, GameState.Draw -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                if (isCreator) { // Solo el creador puede reiniciar la partida online
                                    Button(
                                        onClick = { onlineConnect4ViewModel.resetOnlineGame() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .padding(horizontal = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Jugar de Nuevo", fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
                                    }
                                }
                                Button(
                                    onClick = onNavigateBackToLobby,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3498DB)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Volver al Lobby", fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        else -> { // GameState.Playing or GameState.AskingQuestion
                            Button(
                                onClick = { onlineConnect4ViewModel.deleteOnlineGame() }, // Permite salir en medio del juego
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Salir de la Partida", fontSize = 18.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

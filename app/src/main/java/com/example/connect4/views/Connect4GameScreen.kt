package com.example.connect4.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // Importar el ícono de flecha hacia atrás
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
import android.util.Log // Importar para logs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Connect4GameScreen(
    onNavigateBackToLobby: () -> Unit, // CAMBIO: Nueva función para navegar de vuelta al lobby
    connect4ViewModel: Connect4ViewModel = viewModel()
) {
    val board by connect4ViewModel.board.collectAsState()
    val currentPlayer by connect4ViewModel.currentPlayer.collectAsState()
    val gameState by connect4ViewModel.gameState.collectAsState()
    val currentUserId by connect4ViewModel.currentUserId.collectAsState()
    val onlineGame by connect4ViewModel.onlineGame.collectAsState()
    val isMyTurn by connect4ViewModel.isMyTurn.collectAsState()
    val message by connect4ViewModel.message.collectAsState()

    val scope = rememberCoroutineScope()

    DisposableEffect(onlineGame, gameState, isMyTurn) {
        Log.d("Connect4GameScreen", "UI Update - onlineGame: ${onlineGame?.gameId}, gameState: $gameState, isMyTurn: $isMyTurn")
        onDispose { }
    }

    LaunchedEffect(Unit) {
        if (onlineGame == null) {
            Log.d("Connect4GameScreen", "Inicializando modo offline si onlineGame es null.")
            connect4ViewModel.resetGame()
        }
    }

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
            "Juego en modo pregunta (temporalmente deshabilitado)."
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conecta 4", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2C3E50)),
                navigationIcon = { // Usamos navigationIcon para el botón de "atrás"
                    IconButton(onClick = onNavigateBackToLobby) { // CAMBIO: Llama a onNavigateBackToLobby
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver al Lobby", tint = Color.White)
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
                    val isMyOnlineTurn = (onlineGame != null && isMyTurn)
                    val isOnlineGameWaiting = (onlineGame != null && onlineGame?.status == "waiting")

                    when {
                        onlineGame == null && gameState == GameState.Playing -> {
                            connect4ViewModel.dropPiece(col)
                        }
                        onlineGame != null && isMyOnlineTurn && !isOnlineGameWaiting -> {
                            connect4ViewModel.proceedWithDropPiece(col)
                        }
                        onlineGame != null && !isMyOnlineTurn -> {
                            Log.d("Connect4GameScreen", "No es tu turno. Intento de movimiento en columna $col.")
                        }
                        onlineGame != null && isOnlineGameWaiting -> {
                            Log.d("Connect4GameScreen", "Esperando al segundo jugador. Intento de movimiento en columna $col.")
                        }
                        else -> {
                            Log.d("Connect4GameScreen", "No se puede hacer un movimiento en este momento. Estado actual: $gameState. Intento en columna $col.")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        Log.d("Connect4GameScreen", "Botón 'Reiniciar Juego Local' clickeado.")
                        connect4ViewModel.resetGame()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reiniciar Local", color = Color.White, textAlign = TextAlign.Center)
                }

                if (onlineGame != null) {
                    if (onlineGame?.player1Id == currentUserId && onlineGame?.status != "playing") {
                        Button(
                            onClick = {
                                Log.d("Connect4GameScreen", "Botón 'Reiniciar Partida Online' clickeado.")
                                connect4ViewModel.resetOnlineGame()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reiniciar Online", textAlign = TextAlign.Center, color = Color.White)
                        }
                    }
                    if (onlineGame?.player1Id == currentUserId) {
                        Button(
                            onClick = {
                                Log.d("Connect4GameScreen", "Botón 'Eliminar Partida' clickeado.")
                                connect4ViewModel.deleteOnlineGame()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
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
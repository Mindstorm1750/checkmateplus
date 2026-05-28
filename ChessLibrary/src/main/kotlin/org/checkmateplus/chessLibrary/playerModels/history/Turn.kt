package org.checkmateplus.chessLibrary.playerModels.history

import kotlinx.serialization.Serializable
import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.playerModels.Player
@Serializable
data class Turn (
    val player: Player,
    val move: Move,
    val board: Board,
    val fen: String,
    val timeTaken: Int
)

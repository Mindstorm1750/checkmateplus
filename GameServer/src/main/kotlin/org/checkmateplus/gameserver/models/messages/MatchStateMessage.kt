package org.checkmateplus.gameserver.models.messages

import org.checkmateplus.chessLibrary.chessEngine.Colour
import org.checkmateplus.chessLibrary.playerModels.Player

data class MatchStateMessage(
    val matchId: String,
    val fen: String,
    val whitePlayerTime: Int,
    val blackPlayerTime: Int,
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val isDraw: Boolean = false,
    val lastMove: MoveNotification? = null,
    val whitePlayer: Player? = null,
    val blackPlayer: Player? = null,
    val moveHistory: List<MoveHistoryEntry> = emptyList()
)

data class MoveHistoryEntry(
    val san: String,
    val from: String,
    val to: String,
    val fenAfter: String,
    val colour: Colour,
    val playerId: String
)
package org.chessmateplus.gameserver.models.messages

import org.chessmateplus.chessLibrary.playerModels.Player

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
    val blackPlayer: Player? = null
)
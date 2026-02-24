package org.checkmateplus.gameserver.models.messages

import org.checkmateplus.chessLibrary.chessEngine.PieceType

data class MoveNotification(
    val matchId: String,
    val from: String,
    val to: String,
    val promotion: PieceType?,
    val movedBy: String,
)
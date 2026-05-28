package org.checkmateplus.gameserver.models.messages

import org.checkmateplus.chessLibrary.chessEngine.PieceType


/* not sure what's gonna happen on the chess engine end
so imma just roll with using the algebraic notation for
the moment as I suspect I will have to change this later...*/
data class MoveMessage(
    val matchId: String,
    val playerId: String,
    val from: String,  // e.g., "e2"
    val to: String,    // e.g., "e4"
    val promotion: PieceType? // e.g., "q", "r", "b", "n"
)

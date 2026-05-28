package org.checkmateplus.chessLibrary.chessEngine

import kotlinx.serialization.Serializable

@Serializable
enum class PieceType{
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

package org.chessmateplus.chessLibrary.chessEngine

data class Move(
    val from: Square, // Where piece starts
    val to: Square, // Where piece ends up
    val promotion: PieceType? = null
)
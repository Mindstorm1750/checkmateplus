package uk.ac.kent.chessLibrary.chessEngine

//uk.ac.kent.Move representation
data class Move(
    val from: Square, // Where piece starts
    val to: Square, // Where piece ends up
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
)
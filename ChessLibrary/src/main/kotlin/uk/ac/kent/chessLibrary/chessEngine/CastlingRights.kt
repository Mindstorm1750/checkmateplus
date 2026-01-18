package uk.ac.kent.chessLibrary.chessEngine

data class CastlingRights(
    var whiteKingSide: Boolean = true,
    var whiteQueenSide: Boolean = true,
    var blackKingSide: Boolean = true,
    var blackQueenSide: Boolean = true
)
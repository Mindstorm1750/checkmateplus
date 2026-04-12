package org.checkmateplus.chessLibrary.chessEngine

import kotlinx.serialization.Serializable

@Serializable
data class Piece(val type: PieceType, val color: Colour)
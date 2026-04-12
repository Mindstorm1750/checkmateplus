package org.checkmateplus.chessLibrary.chessEngine

import kotlinx.serialization.Serializable

@Serializable
enum class Colour {
    WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else WHITE
}

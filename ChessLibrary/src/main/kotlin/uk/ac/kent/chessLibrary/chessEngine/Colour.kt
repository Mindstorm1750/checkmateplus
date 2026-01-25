package org.chessmateplus.chessLibrary.chessEngine

enum class Colour {
    WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else Colour.WHITE
}

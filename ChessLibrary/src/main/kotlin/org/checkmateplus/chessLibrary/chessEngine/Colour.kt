package org.checkmateplus.chessLibrary.chessEngine

enum class Colour {
    WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else WHITE
}

package org.checkmateplus.chessLibrary.chessEngine

data class TranspositionTableEntry(val score: Int, val depth: Int, val flag: TTFlag, val bestMove: Move? = null)

enum class TTFlag { EXACT, LOWER_BOUND, UPPER_BOUND }
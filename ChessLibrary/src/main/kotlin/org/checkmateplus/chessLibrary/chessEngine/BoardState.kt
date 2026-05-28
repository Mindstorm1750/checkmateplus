package org.checkmateplus.chessLibrary.chessEngine

import kotlinx.serialization.Serializable

@Serializable
enum class BoardState{
    IN_PROGRESS,
    CHECKMATE,
    STALEMATE
}
package org.checkmateplus.chessLibrary.playerModels

import kotlinx.serialization.Serializable

@Serializable
enum class BotMoveTimeModelType {
    NODELAY, // Return move as soon as its calculated
    MEDIAN, // Return move after the median human move time of 2.24 seconds
    REALISTIC, // Return move after a variable move time depending on MoveTimeFeatures
}
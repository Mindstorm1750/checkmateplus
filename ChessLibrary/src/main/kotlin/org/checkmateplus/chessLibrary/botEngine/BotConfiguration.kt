package org.checkmateplus.chessLibrary.botEngine

import kotlinx.serialization.Serializable

@Serializable
data class BotConfiguration(
    val botModelType: BotModelType,
    val botMoveTimeModelType: BotMoveTimeModelType
)
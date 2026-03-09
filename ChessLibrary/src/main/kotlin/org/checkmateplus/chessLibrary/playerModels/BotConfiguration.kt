package org.checkmateplus.chessLibrary.playerModels

import kotlinx.serialization.Serializable

@Serializable
data class BotConfiguration(
    val botModelType: BotModelType,
    val botMoveTimeModelType: BotMoveTimeModelType
)
package org.chessmateplus.gameserver.models.messages

data class TimeUpdateMessage(
    val matchId: String,
    val whitePlayerTime: Int,
    val blackPlayerTime: Int
)
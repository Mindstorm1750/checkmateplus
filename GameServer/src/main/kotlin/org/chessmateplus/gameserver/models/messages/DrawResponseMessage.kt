package org.chessmateplus.gameserver.models.messages

data class DrawResponseMessage(
    val matchId: String,
    val playerId: String,
    val accepted: Boolean
)
package org.chessmateplus.gameserver.models.messages

data class JoinMatchMessage(
    val matchId: String,
    val playerId: String,
    val playerName: String,
)


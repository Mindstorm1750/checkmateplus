package org.checkmateplus.gameserver.models.messages

data class ResignMessage(
    val matchId: String,
    val playerId: String
)
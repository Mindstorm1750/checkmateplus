package org.chessmateplus.gameserver.models.messages

data class DrawOfferMessage(
    val matchId: String,
    val offeredBy: String
)
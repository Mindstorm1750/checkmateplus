package uk.ac.kent.gameserver.models.messages

data class DrawOfferMessage(
    val matchId: String,
    val offeredBy: String
)
package uk.ac.kent.gameserver.models.messages

data class DrawResponseMessage(
    val matchId: String,
    val playerId: String,
    val accepted: Boolean
)
package uk.ac.kent.gameserver.models.messages

data class ResignMessage(
    val matchId: String,
    val playerId: String
)
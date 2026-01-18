package uk.ac.kent.gameserver.models.messages

data class TimeUpdateMessage(
    val matchId: String,
    val whitePlayerTime: Int,
    val blackPlayerTime: Int
)
package uk.ac.kent.gameserver.models.messages

data class MoveNotification(
    val matchId: String,
    val from: String,
    val to: String,
    val promotion: String? = null,
    val movedBy: String,
)
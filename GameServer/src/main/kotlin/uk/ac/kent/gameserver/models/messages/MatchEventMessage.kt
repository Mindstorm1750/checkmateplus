package uk.ac.kent.gameserver.models.messages

data class MatchEventMessage(
    val matchId: String,
    val eventType: MatchEventType,
    val message: String,
    val data: Map<String, Any>? = null,
    val timestamp: Long = System.currentTimeMillis()
)
package org.chessmateplus.matchmakingservice.models

data class MatchFoundEvent(
    val matchId: String,
    val websocketUrl: String,
    val opponentName: String,
)
package org.checkmateplus.playerservice.models

data class MatchAnalysisGraphQLView(
    val matchId: String,
    val status: String,
    val moves: List<MoveAnalysis>,
)
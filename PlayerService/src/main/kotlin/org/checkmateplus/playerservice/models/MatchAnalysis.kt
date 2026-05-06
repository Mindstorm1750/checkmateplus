package org.checkmateplus.playerservice.models

import kotlinx.serialization.Serializable

@Serializable
data class MatchAnalysis(
    val matchId: String,
    val moves: List<MoveAnalysis>,
)


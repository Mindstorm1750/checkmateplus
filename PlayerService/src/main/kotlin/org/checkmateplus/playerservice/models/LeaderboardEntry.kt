package org.checkmateplus.playerservice.models

import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val rank: Int,
    val playerId: String,
    val displayName: String,
    val skillRating: Int
)

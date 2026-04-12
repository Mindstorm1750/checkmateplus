package org.checkmateplus.playerservice.models

data class PlayerProfile(
    val playerId: String,
    val displayName: String,
    val username: String,
    val skillRating: Int?,
    val totpEnabled: Boolean
)
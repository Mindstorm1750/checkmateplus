package org.checkmateplus.playerservice.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val jwt: String,
    val refreshToken: String?,  // null for guests
    val playerId: String,
    val displayName: String,
    val isGuest: Boolean
)
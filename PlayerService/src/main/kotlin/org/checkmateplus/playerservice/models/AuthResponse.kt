package org.checkmateplus.playerservice.models

class AuthResponse(
    val jwt: String,
    val playerId: String,
    val displayName: String,
    val isGuest: Boolean,
)
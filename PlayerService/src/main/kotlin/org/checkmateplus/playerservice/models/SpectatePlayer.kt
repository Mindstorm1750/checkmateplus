package org.checkmateplus.playerservice.models

data class SpectatePlayer(
    val playerId: String,
    val displayName: String,
    val rating: Int,
    val type: String, // REGISTERED | BOT | ANONYMOUS
)
package org.checkmateplus.playerservice.models

data class RegisterResponse(
    val qrCodeUri: String,
    val secret: String,
    val playerId: String,
)
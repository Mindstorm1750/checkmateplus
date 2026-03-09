package org.checkmateplus.chessLibrary.playerModels

import kotlinx.serialization.Serializable

@Serializable
data class PlayerPreferences (
    val boardColour: String,
    val funAnimation: Boolean,
)

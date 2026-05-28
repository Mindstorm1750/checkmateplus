package org.checkmateplus.gameserver.models.messages

import org.checkmateplus.chessLibrary.playerModels.PlayerPreferences

data class JoinMatchMessage(
    val matchId: String,
    val playerId: String,
    val playerName: String,
    val playerPreferences: PlayerPreferences
)

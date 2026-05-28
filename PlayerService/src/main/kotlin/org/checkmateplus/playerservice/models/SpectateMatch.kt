package org.checkmateplus.playerservice.models

import org.checkmateplus.chessLibrary.playerModels.TimeControl

data class SpectateMatch(
    val matchId: String,
    val status: String,
    val serverId: String?,
    val websocketUrl: String?,
    val whitePlayer: SpectatePlayer?,
    val blackPlayer: SpectatePlayer?,
    val whiteTimeSeconds: Int,
    val blackTimeSeconds: Int,
    val lastFen: String?,
    val activeColor: String?,
    val moveNumber: Int,
    val winnerId: String?,
    val reason: String?,
    val updatedAt: Long,
    val botConfiguration: SpectateBotConfiguration?,
    val matchType: String?,
    val timeControl: TimeControl?,
)


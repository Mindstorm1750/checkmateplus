package org.checkmateplus.gameservermanager.models

import kotlinx.serialization.Serializable
import org.checkmateplus.chessLibrary.playerModels.Player

@Serializable
data class TrackedMatch(
    val matchId: String,
    val serverId: String?,
    val status: String,
    val whitePlayer: Player?,
    val blackPlayer: Player?,
    val lastFen: String?,
    val whiteTimeSeconds: Int?,
    val blackTimeSeconds: Int?,
    val activeColor: String?,
    val moveNumber: Int?,
    val winnerId: String?,
    val reason: String?,
    val updatedAt: Long,
)

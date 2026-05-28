package org.checkmateplus.playerservice.models

import kotlinx.serialization.Serializable
import org.checkmateplus.chessLibrary.playerModels.TimeControl

@Serializable
data class PlayerStats(
    val playerId: String,
    val displayName: String,
    val skillRating: Int?,
    val ratingDeviation: Double?,
    val wins: Int,
    val losses: Int,
    val draws: Int,
    val totalGames: Int,
    val winRate: Int,
    val winsAsWhite: Int,
    val winsAsBlack: Int,
    val gamesAsWhite: Int,
    val gamesAsBlack: Int,
    val currentStreak: Int,
    val longestWinStreak: Int,
    val averageGameDurationMs: Long,
    val favouriteTimeControl: TimeControl?,
    val memberSinceEpochMs: Long
)


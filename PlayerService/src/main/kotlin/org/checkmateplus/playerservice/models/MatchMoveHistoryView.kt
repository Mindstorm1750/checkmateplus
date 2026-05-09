package org.checkmateplus.playerservice.models

import org.checkmateplus.chessLibrary.playerModels.TimeControl

data class MatchMoveHistoryView(
    val matchId: String,
    val whitePlayer: SpectatePlayer?,
    val blackPlayer: SpectatePlayer?,
    val timeControl: TimeControl,
    val matchType: String,
    val outcome: String,
    val finalFen: String,
    val moves: List<MoveHistoryView>,
    val createdAtEpochMs: Long,
    val gameDurationMs: Long,
)

data class MoveHistoryView(
    val san: String,
    val from: String,
    val to: String,
    val fenAfter: String,
    val colour: String,
    val playerId: String,
)

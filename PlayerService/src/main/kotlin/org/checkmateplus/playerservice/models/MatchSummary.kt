package org.checkmateplus.playerservice.models

import org.checkmateplus.chessLibrary.chessEngine.Colour
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl

data class MatchSummary(
    val matchId: String,
    val opponentId: String?,
    val result: MatchResult,
    val colour: Colour,
    val matchType: MatchType,
    val timeControl: TimeControl,
    val gameDurationMs: Long?,
    val createdAtEpochMs: Long
)
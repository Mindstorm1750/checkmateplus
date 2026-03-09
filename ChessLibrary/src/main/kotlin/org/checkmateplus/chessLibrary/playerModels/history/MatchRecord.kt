package org.checkmateplus.chessLibrary.playerModels.history

import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.playerModels.MatchStatus
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl

data class MatchRecord(
    val board: Board, //Final Board
    val players: List<Player>,
    val matchId: String,
    val timeControl: TimeControl,
    val matchType: MatchType,
    val botConfiguration: BotConfiguration?,
    val matchOutcome: MatchOutcome,
    val whitePlayer: Player,
    val blackPlayer: Player,
    val remainingWhitePlayerTime: Int,
    val remainingBlackPlayerTime: Int,
    val matchStatus: MatchStatus,
    val history: List<Turn>
)

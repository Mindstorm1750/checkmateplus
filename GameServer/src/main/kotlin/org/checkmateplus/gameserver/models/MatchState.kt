package org.checkmateplus.gameserver.models

import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Colour
import org.checkmateplus.chessLibrary.chessEngine.FenToBoard
import org.checkmateplus.chessLibrary.chessEngine.STARTING_FEN
import org.checkmateplus.chessLibrary.playerModels.BotConfiguration
import org.checkmateplus.chessLibrary.playerModels.BotModelType
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class MatchState(
    var players: List<Player>, //Player White is List position 0 and player black is position 1
    val matchId: String,
    val timeControl: TimeControl,
    val matchType: MatchType,
    val botConfiguration: BotConfiguration? = null,
) {
    var board: Board? = null
    var whitePlayerTime: Int = timeControl.minutes * 60 //set up timers
    var blackPlayerTime: Int = timeControl.minutes * 60
    var activeTurn: Colour = Colour.WHITE
    var whitePlayer: Player? = null
    var blackPlayer: Player? = null
    var connectedPlayers: HashSet<String>
    var matchStarted: AtomicBoolean



    init {
        whitePlayer = players[0]
        blackPlayer = players[1] //assign the players to vars just to clarify which side is which!
        board = FenToBoard(STARTING_FEN)
        connectedPlayers = hashSetOf()
        matchStarted = AtomicBoolean(false)
    }
}

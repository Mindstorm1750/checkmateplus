package org.checkmateplus.gameserver.models

import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Colour
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl
class MatchState(
    var players: List<Player>,
    val matchId: String,
    val timeControl: TimeControl = TimeControl.CLASSICAL_30_0 //setting to 30 min matches by default
) {
    var board: Board? = Board()
    var whitePlayerTime: Int = timeControl.minutes * 60 //set up timers
    var blackPlayerTime: Int = timeControl.minutes * 60
    var activeTurn: Colour = Colour.WHITE

    fun addPlayer(player: Player): Boolean {
        if (players.size == 1 || players.isEmpty() && !players.contains(player)) {
            players += player
            return true
        }
        else return false //kinda generic no here... needs looking at later



    }
}

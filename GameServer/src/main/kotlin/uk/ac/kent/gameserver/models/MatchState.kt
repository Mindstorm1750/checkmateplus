package uk.ac.kent.gameserver.models

import uk.ac.kent.chessLibrary.chessEngine.Board
import uk.ac.kent.chessLibrary.chessEngine.Colour
import uk.ac.kent.chessLibrary.playerModels.Player
import uk.ac.kent.playerModels.TimeControl

class MatchState(
    val players: List<Player>,
    val matchId: String,
    val timeControl: TimeControl = TimeControl.CLASSICAL_30_0 //setting to 30 min matches by default
) {
    var board: Board? = Board()
    var whitePlayerTime: Int = timeControl.minutes * 60 //set up timers
    var blackPlayerTime: Int = timeControl.minutes * 60
    var activeTurn: Colour = Colour.WHITE
}

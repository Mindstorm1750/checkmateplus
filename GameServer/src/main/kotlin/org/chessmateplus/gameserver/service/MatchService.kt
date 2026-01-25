package org.chessmateplus.gameserver.service

import org.chessmateplus.chessLibrary.chessEngine.Board
import org.chessmateplus.chessLibrary.chessEngine.Colour.*
import org.chessmateplus.chessLibrary.chessEngine.Move
import org.chessmateplus.chessLibrary.chessEngine.PieceType
import org.chessmateplus.chessLibrary.chessEngine.toSquare
import org.chessmateplus.chessLibrary.playerModels.Player
import org.chessmateplus.chessLibrary.playerModels.TimeControl
import org.chessmateplus.gameserver.models.MatchState
import org.chessmateplus.gameserver.models.messages.MatchStateMessage
import org.chessmateplus.gameserver.models.messages.MoveNotification
import org.chessmateplus.gameserver.models.messages.TimeUpdateMessage
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Service
class MatchService {
    private val matches = ConcurrentHashMap<String, MatchState>()
    private val timers = ConcurrentHashMap<String, Timer>()


    fun createMatch(
        matchId: String,
        players: List<Player>,
        timeControl: TimeControl
    ): MatchState {
        val matchState =
            MatchState(players, matchId, timeControl)
        matches[matchId] = matchState
        return matchState
    }

    fun getMatch(matchId: String): MatchState? {
        return matches[matchId]
    }

    /**
     * Process a move with the engine
     * Need proper engine implementation
     */
    fun processMove(
        matchId: String,
        moveMessage: org.chessmateplus.gameserver.models.messages.MoveMessage
    ): MatchStateMessage {
        val match = matches[matchId]
            ?: throw IllegalArgumentException("Match not found: $matchId")
        val board = match.board
            ?: throw IllegalStateException("Board not initialized")
        /*
        TODO: Implement validation from chess engine to detect illicit moves. Waiting for chess engine implementation first though
        */

        // execute the move on the local board
        board.applyMove(
            Move(
                from = moveMessage.from.toSquare() ?: throw IllegalArgumentException("Invalid from square"),
                to = moveMessage.to.toSquare() ?: throw IllegalArgumentException("Invalid to square"),
                promotion = moveMessage.promotion as PieceType?
            )

        )
        //needs to be re-enabled when move execution is enabled
        //if (!moveSuccess) {
        //   throw IllegalArgumentException("Illegal move")
        //}


        // Update time with increment
        updateTimeAfterMove(match, board)

        //flip the active turn
        if (match.activeTurn == WHITE) match.activeTurn = BLACK
        if (match.activeTurn == BLACK) match.activeTurn = WHITE

        // Create move notification
        val moveNotification = MoveNotification(
            matchId = matchId,
            from = moveMessage.from,
            to = moveMessage.to,
            promotion = moveMessage.promotion,
            movedBy = moveMessage.playerId,
        )

        return createMatchStateMessage(match, moveNotification)
    }

    private fun updateTimeAfterMove(
        match: MatchState,
        board: Board
    ) {
        val increment = match.timeControl.increment

        // Add increment to the player who just moved
        // Current turn is now the OTHER player after the move
        when (board.sideToMove) {
            BLACK -> {
                // White just moved
                match.whitePlayerTime += increment
            }

            WHITE -> {
                // Black just moved
                match.blackPlayerTime += increment
            }
        }
    }

    fun startTimer(
        matchId: String,
        onTimeUpdate: (TimeUpdateMessage) -> Unit,
        onTimeExpired: (String) -> Unit
    ) {
        val match = matches[matchId] ?: return

        val timer = Timer()
        timers[matchId] = timer

        timer.scheduleAtFixedRate(object : TimerTask() {
            //timer runs at fixed rate, it is possible to lose ms here
            override fun run() {
                val board = match.board ?: return

                when (board.sideToMove) {
                    WHITE -> {
                        match.whitePlayerTime--
                        if (match.whitePlayerTime <= 0) {
                            match.whitePlayerTime = 0
                            onTimeExpired("WHITE")
                            stopTimer(matchId)
                            return
                        }
                    }

                    BLACK -> {
                        match.blackPlayerTime--
                        if (match.blackPlayerTime <= 0) {
                            match.blackPlayerTime = 0
                            onTimeExpired("BLACK")
                            stopTimer(matchId)
                            return
                        }
                    }
                }

                onTimeUpdate(
                    TimeUpdateMessage(
                        matchId = matchId,
                        whitePlayerTime = match.whitePlayerTime,
                        blackPlayerTime = match.blackPlayerTime
                    )
                )
            }
        }, 1000, 1000)
    }

    fun stopTimer(matchId: String) {
        timers[matchId]?.cancel()
        timers.remove(matchId)
    }

    fun pauseTimer(matchId: String) {
        timers[matchId]?.cancel()
    }

    fun resumeTimer(
        matchId: String,
        onTimeUpdate: (TimeUpdateMessage) -> Unit,
        onTimeExpired: (String) -> Unit
    ) {
        startTimer(matchId, onTimeUpdate, onTimeExpired)
    }

    fun resign(matchId: String, playerId: String): String {
        val match = matches[matchId]
            ?: throw IllegalArgumentException("Match not found")
        stopTimer(matchId)

        val resigningPlayer = match.players.find { it.playerId == playerId }
        return if (resigningPlayer == match.players[0]) {
            match.players[1].displayName

        } else {
            match.players[0].displayName
        }

    }

    fun acceptDraw(matchId: String): Boolean {
        matches[matchId] ?: return false
        stopTimer(matchId)
        return true
    }

    fun removeMatch(matchId: String) {
        stopTimer(matchId)
        matches.remove(matchId)
    }

    /**
     * Send the MatchState
     */
    fun createMatchStateMessage(
        match: MatchState,
        lastMove: MoveNotification? = null
    ): MatchStateMessage {
        match.board
            ?: throw IllegalStateException("Board not initialized")

        return MatchStateMessage(
            matchId = match.matchId,
            fen = TODO("waiting for engine implementation"),
            whitePlayerTime = match.whitePlayerTime,
            blackPlayerTime = match.blackPlayerTime,
            isCheck = TODO("waiting for engine implementation"),
            isCheckmate = TODO("waiting for engine implementation"),
            isStalemate = TODO("waiting for engine implementation"),
            isDraw = TODO("waiting for engine implementation"),
            lastMove = lastMove,
            whitePlayer = match.players.getOrNull(0),
            blackPlayer = match.players.getOrNull(1)
        )
    }

    fun getActiveMatchCount(): Int {
        return matches.size;
    }
    fun getActivePlayerCount(): Int {
        var count = 0
        matches.forEach { (_, matchstate) -> matchstate.players.forEach { _ -> count++ } }
        return count
    }
}


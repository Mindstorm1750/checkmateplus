package org.checkmateplus.gameserver.service

import org.checkmateplus.chessLibrary.chessEngine.BoardState
import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Colour.*
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.chessEngine.PieceType
import org.checkmateplus.chessLibrary.chessEngine.toSquare
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.gameserver.models.MatchState
import org.checkmateplus.gameserver.models.messages.MatchStateMessage
import org.checkmateplus.gameserver.models.messages.MoveNotification
import org.checkmateplus.gameserver.models.messages.TimeUpdateMessage
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for managing chess matches, including state, timers, and game logic.
 */
@Service
class MatchService {
    private val matches = ConcurrentHashMap<String, MatchState>()
    private val timers = ConcurrentHashMap<String, Timer>()


    /**
     * Creates a new match with the given players and time control.
     *
     * @param matchId Unique identifier for the match.
     * @param players List of players in the match.
     * @param timeControl The time control settings for the match.
     * @return The created [MatchState].
     */
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
     * Processes a move message and updates the match state.
     *
     * @param matchId The identifier of the match.
     * @param moveMessage The move details.
     * @return The updated [MatchStateMessage].
     */
    fun processMove(
        matchId: String,
        moveMessage: org.checkmateplus.gameserver.models.messages.MoveMessage
    ): MatchStateMessage {
        val match = matches[matchId]
            ?: throw IllegalArgumentException("Match not found: $matchId")
        var board = match.board
            ?: throw IllegalStateException("Board not initialized")
        /*
        TODO: Implement validation from chess engine to detect illicit moves. Waiting for chess engine implementation first though
        */

        // execute the move on the local board
        match.board = board.applyMove(
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
        updateTimeAfterMove(match, match.board)

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
        board: Board?
    ) {
        val increment = match.timeControl.increment

        // Add increment to the player who just moved
        // Current turn is now the OTHER player after the move
        when (board?.sideToMove) {
            BLACK -> {
                // White just moved
                match.whitePlayerTime += increment
            }

            WHITE -> {
                // Black just moved
                match.blackPlayerTime += increment
            }

            else -> {}
        }
    }

    /**
     * Starts the timer for a match.
     *
     * @param matchId The identifier of the match.
     * @param onTimeUpdate Callback for time updates.
     * @param onTimeExpired Callback when a player's time expires.
     */
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
        match: MatchState?,
        lastMove: MoveNotification? = null
    ): MatchStateMessage {
        match?.board
            ?: throw IllegalStateException("Board not initialized")
        val boardState = match.board!!.getBoardState()
        var isCheckmate = false
        var isCheck = false
        var isStalemate = false
        var isDraw = false
        if (boardState == BoardState.CHECKMATE) {
            isCheckmate = true
            isCheck = true
            isStalemate = false
            isDraw = false
        }
        if (boardState == BoardState.STALEMATE) {
            isCheckmate = false
            isCheck = false
            isStalemate = true
            isDraw = true
        }
        return MatchStateMessage(
            matchId = match.matchId,
            fen = match.board!!.toFen(),
            whitePlayerTime = match.whitePlayerTime,
            blackPlayerTime = match.blackPlayerTime,
            isCheck = isCheck,
            isCheckmate = isCheckmate,
            isStalemate = isStalemate,
            isDraw = isDraw,
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


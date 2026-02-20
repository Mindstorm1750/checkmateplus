package org.checkmateplus.gameserver.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.checkmateplus.chessLibrary.botEngine.BotEngine
import org.checkmateplus.chessLibrary.chessEngine.*
import org.checkmateplus.chessLibrary.chessEngine.Colour.BLACK
import org.checkmateplus.chessLibrary.chessEngine.Colour.WHITE
import org.checkmateplus.chessLibrary.playerModels.BotModelType
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.gameserver.models.MatchState
import org.checkmateplus.gameserver.models.messages.MatchStateMessage
import org.checkmateplus.gameserver.models.messages.MoveMessage
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
        timeControl: TimeControl,
        matchType: MatchType,
        botModelType: BotModelType?
    ): MatchState {
        val matchState = MatchState(
            players, matchId, timeControl, matchType, botModelType
        )

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
    suspend fun processMove(
        matchId: String, moveMessage: MoveMessage
    ): MatchStateMessage {
        val match = matches[matchId] ?: throw IllegalArgumentException("Match not found: $matchId")
        val board = match.board ?: throw IllegalStateException("Board not initialized")


        // execute the move on the local board
        match.board = board.applyMove(
            Move(
                from = moveMessage.from.toSquare() ?: throw IllegalArgumentException("Invalid from square"),
                to = moveMessage.to.toSquare() ?: throw IllegalArgumentException("Invalid to square"),
                promotion = moveMessage.promotion
            )

        )
        // Update time with increment
        updateTimeAfterMove(match, match.board)

        //flip the active turn
        flipTurn(match)


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

    fun updateTimeAfterMove(
        match: MatchState, board: Board?
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
        matchId: String, onTimeUpdate: (TimeUpdateMessage) -> Unit, onTimeExpired: (String) -> Unit
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

    fun flipTurn(match: MatchState) {
        match.activeTurn = if (match.activeTurn == WHITE) BLACK else WHITE
    }

    fun stopTimer(matchId: String) {
        timers[matchId]?.cancel()
        timers.remove(matchId)
    }

    fun pauseTimer(matchId: String) {
        timers[matchId]?.cancel()
    }

    fun resumeTimer(
        matchId: String, onTimeUpdate: (TimeUpdateMessage) -> Unit, onTimeExpired: (String) -> Unit
    ) {
        startTimer(matchId, onTimeUpdate, onTimeExpired)
    }

    fun resign(matchId: String, playerId: String): String {
        val match = matches[matchId] ?: throw IllegalArgumentException("Match not found")
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
        match: MatchState?, lastMove: MoveNotification? = null
    ): MatchStateMessage {
        match?.board ?: throw IllegalStateException("Board not initialized")
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
        return matches.size
    }

    fun getActivePlayerCount(): Int {
        var count = 0
        matches.forEach { (_, matchstate) -> matchstate.players.forEach { _ -> count++ } }
        return count
    }

    suspend fun calculateMove(board: Board, botModelType: BotModelType): Move {
        return withContext(Dispatchers.Default) {
            BotEngine.create(botModelType).getMove(board)
        }
    }
}


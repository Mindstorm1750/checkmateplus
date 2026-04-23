package org.checkmateplus.gameserver.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.checkmateplus.chessLibrary.botEngine.calculateMoveTimeDelayMs
import org.checkmateplus.chessLibrary.chessEngine.Colour.BLACK
import org.checkmateplus.chessLibrary.chessEngine.Colour.WHITE
import org.checkmateplus.chessLibrary.chessEngine.toAlgebraic
import org.checkmateplus.chessLibrary.chessEngine.toSquare
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.botEngine.MoveTimeModelFeatures
import org.checkmateplus.chessLibrary.playerModels.MatchStatus
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.history.MatchOutcome
import org.checkmateplus.chessLibrary.playerModels.history.MatchRecord
import org.checkmateplus.gameserver.models.MatchState
import org.checkmateplus.gameserver.models.messages.*
import org.checkmateplus.gameserver.service.GameServerService
import org.checkmateplus.gameserver.service.MatchService
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Controller
import java.security.Principal
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds

/**
 * WebSocket controller for handling real-time chess match interactions.
 *
 * This controller manages moves, game state updates, and match-related events
 * such as joining, starting, draw offers, and resignations.
 *
 * @property messagingTemplate Template for sending messages to WebSocket topics.
 * @property matchService Service for processing match logic and state.
 */
@RegisterReflectionForBinding(
    DrawOfferMessage::class,
    DrawResponseMessage::class,
    JoinMatchMessage::class,
    MatchEventMessage::class,
    MatchEventType::class,
    MatchStateMessage::class,
    MoveMessage::class,
    MoveNotification::class,
    ResignMessage::class,
    TimeUpdateMessage::class,
    MatchState::class,
    Player::class,
    Player.Anonymous::class,
    Player.Bot::class,
)

@Controller
class MatchWebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val matchService: MatchService,
    private val gameServerService: GameServerService
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    //TODO: Need to add disconnect/reconnect support incase of connection drop

    /**
     * Handles a move made by a player in a specific match.
     *
     * @param matchId The unique identifier of the match.
     * @param principal The authenticated user making the move.
     * @param moveMessage The details of the move being made.
     * @throws IllegalAccessException if the player attempts to move for someone else.
     */
    @MessageMapping("/match/{matchId}/move")
    fun makeMove(
        @DestinationVariable matchId: String,
        principal: Principal,
        moveMessage: MoveMessage
    ) {
        try {
            val match = matchService.getMatch(matchId)
            val securePlayerId = (principal as JwtAuthenticationToken).token.subject
            if (moveMessage.playerId != securePlayerId) throw IllegalAccessException("Player mismatch")
            // these are for if the match is PVE (a bot match) and the bot is using the realistic move time feature
            val timePlayerTook = match!!.activePlayerTimeAtTurnStart - (if(match.activeTurn == WHITE) match.whitePlayerTime else match.blackPlayerTime)
            val wasCapture = match.board?.squares?.get(moveMessage.to.toSquare() ?: throw IllegalArgumentException("moveMessage has no 'to' square")) != null

            // Process the move
            val matchState = matchService.processMove(matchId, moveMessage)

            // Broadcast updated state to all players
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/state",
                matchState
            )

            // Send the specific move notification
            matchState.lastMove?.let { lastMove ->
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/moves",
                    lastMove
                )
            }
            // Handle game end conditions
            if (handleGameEndConditions(matchId, matchState)) return

            if (match.matchType == MatchType.PVE && match.botConfiguration?.botModelType != null) {
                val botColour = if (match.players[0] is Player.Bot) WHITE else BLACK

                if (botColour == match.activeTurn) {
                    CoroutineScope(Dispatchers.Default).launch {
                        val botMove = coroutineScope {
                            val moveTimeModelFeatures = MoveTimeModelFeatures(
                                wasCapture = wasCapture,
                                moveNumber = match.board?.fullMoveNumber ?: 0,
                                initialTime = match.timeControl.minutes * 60,
                                increment = match.timeControl.increment,
                                timeRemaining = (if (match.activeTurn == WHITE) match.whitePlayerTime else match.blackPlayerTime).toDouble(),
                                timeOpponentTook = timePlayerTook.toDouble(),
                                numberOfLegalMoves = match.board?.generateLegalMoves()?.size ?: 0,
                                inCheck = match.board?.isInCheck(match.activeTurn) ?: false,
                            )
                            val moveDeferred = async(Dispatchers.Default) {
                                matchService.calculateMove(match.board!!, match.botConfiguration.botModelType)
                            }
                            val delayDeferred = async {
                                delay(calculateMoveTimeDelayMs(match.botConfiguration.botMoveTimeModelType, moveTimeModelFeatures).milliseconds)
                            }
                            awaitAll(moveDeferred, delayDeferred)
                            moveDeferred.await()
                        }

                        val botClock = if(botColour == WHITE) match.whitePlayerTime else match.blackPlayerTime
                        if (botClock <= 0) return@launch
                        if (matchService.getMatch(matchId) === null) return@launch

                        val botMoveMessage = MoveMessage(
                            matchId,
                            playerId = match.players.find { it is Player.Bot }?.playerId ?: "BOT",
                            from = botMove.from.toAlgebraic(),
                            to = botMove.to.toAlgebraic(),
                            promotion = botMove.promotion,
                        )
                        val botMatchStateMessage = matchService.processMove(matchId, botMoveMessage)

                        // Broadcast updated state to all players
                        messagingTemplate.convertAndSend(
                            "/topic/match/$matchId/state",
                            botMatchStateMessage
                        )

                        // Send the specific move notification
                        matchState.lastMove?.let { lastMove ->
                            messagingTemplate.convertAndSend(
                                "/topic/match/$matchId/moves",
                                botMatchStateMessage
                            )
                        }
                        handleGameEndConditions(matchId, botMatchStateMessage)
                    }
                }
            }
        } catch (e: IllegalArgumentException) {
            // Send error to the player who made the invalid move
            messagingTemplate.convertAndSendToUser(
                principal.name,
                "/queue/errors",
                mapOf(
                    "error" to (e.message ?: "Invalid move"),
                    "matchId" to matchId
                )
            )

            // Also send an event to all players
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/events",
                MatchEventMessage(
                    matchId = matchId,
                    eventType = MatchEventType.ILLEGAL_MOVE,
                    message = "Illegal move attempted",
                    data = mapOf("playerId" to moveMessage.playerId)
                )
            )
        }
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun notifyMatchCompleted(match: MatchState, winnerId: String, reason: String) {
        val durationSeconds = (kotlin.time.Clock.System.now() - match.createdAt).inWholeSeconds.toInt()
        gameServerService.reportMatchCompleted(
            matchId = match.matchId,
            winnerId = winnerId,
            reason = reason,
            totalMoves = match.turnHistory.size,
            durationSeconds = durationSeconds
        )
        matchService.removeMatch(match.matchId)
    }

    /**
     * Handles game end conditions such as checkmate, stalemate, or other draws.
     *
     * @param matchId The unique identifier of the match.
     * @param matchState The current state of the match.
     */
    private fun handleGameEndConditions(matchId: String, matchState: MatchStateMessage): Boolean {
        when {
            matchState.isCheckmate -> {
                val match = matchService.getMatch(matchId)
                val losingPlayer = match?.board?.sideToMove
                val winner = if (losingPlayer == WHITE) match.blackPlayer else match?.whitePlayer
                val outcome = if (losingPlayer == WHITE) MatchOutcome.BLACK_WINS else MatchOutcome.WHITE_WINS
                val winnerName = winner!!.displayName
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/events",
                    MatchEventMessage(
                        matchId = matchId,
                        eventType = MatchEventType.MATCH_ENDED,
                        message = "Checkmate! $winnerName wins!",
                        data = mapOf(
                            "result" to "checkmate",
                            "winner" to winnerName
                        )
                    )
                )
                matchService.stopTimer(matchId)
                matchService.addMatchRecord(match!!.toMatchRecord(outcome))
                notifyMatchCompleted(match, winner.playerId, "checkmate")
                return true
            }

            matchState.isStalemate -> {
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/events",
                    MatchEventMessage(
                        matchId = matchId,
                        eventType = MatchEventType.MATCH_ENDED,
                        message = "Stalemate! The game is a draw.",
                        data = mapOf("result" to "stalemate")
                    )
                )
                matchService.stopTimer(matchId)
                val match = matchService.getMatch(matchId)
                matchService.addMatchRecord(match!!.toMatchRecord(MatchOutcome.DRAW))
                notifyMatchCompleted(match, winnerId = "", reason = "stalemate")
                return true
            }

            matchState.isDraw -> {
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/events",
                    MatchEventMessage(
                        matchId = matchId,
                        eventType = MatchEventType.MATCH_ENDED,
                        message = "Draw by repetition or insufficient material.",
                        data = mapOf("result" to "draw")
                    )
                )
                matchService.stopTimer(matchId)
                val match = matchService.getMatch(matchId)
                matchService.addMatchRecord(match!!.toMatchRecord(MatchOutcome.DRAW))
                notifyMatchCompleted(match, winnerId = "", reason = "draw")
                return true
            }
        }
        return false
    }

    /**
     * Handles a player joining a match.
     *
     * @param matchId The unique identifier of the match.
     * @param joinMessage Message containing player details joining the match.
     */
    @OptIn(ExperimentalAtomicApi::class)
    @MessageMapping("/match/{matchId}/join")
    fun joinMatch(
        @DestinationVariable matchId: String,
        principal: Principal,
        joinMessage: JoinMatchMessage
    ) {
        logger.info(joinMessage.toString())
        val securePlayerId = (principal as JwtAuthenticationToken).token.subject
        if (joinMessage.playerId != securePlayerId) throw IllegalAccessException("Player mismatch")
        val playerPreferences = joinMessage.playerPreferences

        val match = matchService.getMatch(matchId) ?: run {
            logger.warn("Join attempt for unknown match: $matchId")
            return
        }
        if (match.matchType == MatchType.PVE) {
            match.players
                .filterIsInstance<Player.Bot>()
                .forEach { match.connectedPlayers.add(it.playerId) }
        }
        messagingTemplate.convertAndSend(
            "/topic/match/$matchId/events",
            MatchEventMessage(
                matchId = matchId,
                eventType = MatchEventType.PLAYER_JOINED,
                message = "${joinMessage.playerName} joined the match"
            )
        )

        if (match.players.any { player -> player.playerId == securePlayerId }) {
            match.connectedPlayers.add(joinMessage.playerId) // add the player as having connect to the match
        }
        val index = match.players.indexOfFirst { it.playerId == securePlayerId }
        if (index != -1) { //-1 indicates not found!
            match.players[index] = when (val player = match.players[index]) {
                is Player.Anonymous -> player.copy(playerPreferences = playerPreferences)
                is Player.Bot -> player
                is Player.Registered -> player.copy(playerPreferences = playerPreferences)
            }
        }
        val matchState = matchService.createMatchStateMessage(match)
        messagingTemplate.convertAndSendToUser(
            joinMessage.playerId,
            "/queue/match/$matchId/state",
            matchState
        )

        //when both players are connected start the match
        if (match.connectedPlayers.size == 2 && match.matchStarted.compareAndSet(expectedValue = false, newValue = true)) {
            startMatch(matchId)
        }
    }

    /**
     * Starts the match timer and broadcasts the match start event.
     *
     * @param matchId The unique identifier of the match.
     */
    //@MessageMapping("/match/{matchId}/start") disabled not used!!!
    fun startMatch(@DestinationVariable matchId: String) {
        matchService.startTimer(
            matchId,
            onTimeUpdate = { timeUpdate ->
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/time",
                    timeUpdate
                )
            },
            onTimeExpired = { losingColor ->
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/events",
                    MatchEventMessage(
                        matchId = matchId,
                        eventType = MatchEventType.TIME_EXPIRED,
                        message = "$losingColor ran out of time!",
                        data = mapOf(
                            "loser" to losingColor,
                            "winner" to if (losingColor == "WHITE") "BLACK" else "WHITE"
                        )
                    )
                )
                val winner = if (losingColor == "WHITE") MatchOutcome.BLACK_WINS else MatchOutcome.WHITE_WINS
                val match = matchService.getMatch(matchId)
                matchService.addMatchRecord(match!!.toMatchRecord(winner))
                val winnerPlayer = if (losingColor == "WHITE") match.blackPlayer else match.whitePlayer
                notifyMatchCompleted(match, winnerPlayer?.playerId ?: "", "timeout")
            }
        )

        messagingTemplate.convertAndSend(
            "/topic/match/$matchId/events",
            MatchEventMessage(
                matchId = matchId,
                eventType = MatchEventType.MATCH_STARTED,
                message = "Match started!"
            )
        )
        val match = matchService.getMatch(matchId)!!
        val activeTurn = match.activeTurn
        val botSide = if(match.whitePlayer is Player.Bot) WHITE else BLACK
        if (activeTurn == WHITE && botSide == WHITE ) {
            val botMove = matchService.calculateMove(match.board!!, match.botConfiguration?.botModelType!!)
            val botMoveMessage = MoveMessage(
                matchId,
                playerId = match.players.find { it is Player.Bot }?.playerId ?: "BOT",
                from = botMove.from.toAlgebraic(),
                to = botMove.to.toAlgebraic(),
                promotion = botMove.promotion,
            )
            val botMatchStateMessage = matchService.processMove(matchId, botMoveMessage)
            val botMoveNotification = MoveNotification(
                matchId,
                botMoveMessage.from,
                botMoveMessage.to,
                botMoveMessage.promotion,
                botMoveMessage.playerId
            )

            // Broadcast updated state to all players
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/state",
                botMatchStateMessage
            )

            // Send the specific move notification
            botMatchStateMessage.lastMove?.let { lastMove ->
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/moves",
                    botMoveNotification
                )
            }
        } else {
            val matchState = matchService.createMatchStateMessage(match)
            messagingTemplate.convertAndSend("/topic/match/$matchId/state", matchState)
        }
    }

    /**
     * Broadcasts a draw offer to the other player.
     *
     * @param matchId The unique identifier of the match.
     * @param drawOffer The draw offer message.
     */
    @MessageMapping("/match/{matchId}/draw/offer")
    fun offerDraw(
        @DestinationVariable matchId: String,
        drawOffer: DrawOfferMessage,
        principal: Principal
    ) {
        val securePlayerId = (principal as JwtAuthenticationToken).token.subject
        if (drawOffer.offeredBy != securePlayerId) throw IllegalAccessException("Player mismatch")
        messagingTemplate.convertAndSend(
            "/topic/match/$matchId/draw-offer",
            drawOffer
        )
    }

    /**
     * Responds to a draw offer.
     *
     * @param matchId The unique identifier of the match.
     * @param response The response to the draw offer.
     */
    @MessageMapping("/match/{matchId}/draw/respond")
    fun respondToDraw(
        @DestinationVariable matchId: String,
        response: DrawResponseMessage,
        principal: Principal
    ) {
        val securePlayerId = (principal as JwtAuthenticationToken).token.subject
        if (response.playerId != securePlayerId) throw IllegalAccessException("Player mismatch")
        if (response.accepted) {
            matchService.acceptDraw(matchId)
            val match = matchService.getMatch(matchId)
            matchService.addMatchRecord(match!!.toMatchRecord(MatchOutcome.DRAW))
            notifyMatchCompleted(match, winnerId = "", reason = "draw agreed")
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/events",
                MatchEventMessage(
                    matchId = matchId,
                    eventType = MatchEventType.DRAW_ACCEPTED,
                    message = "Draw accepted. Match ended in a draw.",
                    data = mapOf("result" to "draw_agreement")
                )
            )
        } else {
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/events",
                MatchEventMessage(
                    matchId = matchId,
                    eventType = MatchEventType.DRAW_DECLINED,
                    message = "Draw offer declined"
                )
            )
        }
    }

    /**
     * Handles a player resignation.
     *
     * @param matchId The unique identifier of the match.
     * @param resignMessage Message containing details about the resignation.
     */
    @MessageMapping("/match/{matchId}/resign")
    fun resign(
        @DestinationVariable matchId: String,
        resignMessage: ResignMessage,
        principal: Principal
    ) {
        val match = matchService.getMatch(matchId)
        val securePlayerId = (principal as JwtAuthenticationToken).token.subject
        if (resignMessage.playerId != securePlayerId) throw IllegalAccessException("Player mismatch")
        val winningPlayer = matchService.resign(matchId, resignMessage.playerId)
        val resigningPlayerName = (match?.players?.find { it.playerId == resignMessage.playerId }?.displayName) ?: "Player"

        messagingTemplate.convertAndSend(
            "/topic/match/$matchId/events",
            MatchEventMessage(
                matchId = matchId,
                eventType = MatchEventType.PLAYER_RESIGNED,
                message = "$resigningPlayerName resigned. ${winningPlayer.displayName} wins!",
                data = mapOf(
                    "winner" to winningPlayer.displayName,
                    "resignedPlayer" to resignMessage.playerId
                )
            )
        )

        val outcome = if(match?.whitePlayer?.playerId == resignMessage.playerId) MatchOutcome.BLACK_WINS else MatchOutcome.WHITE_WINS

        matchService.addMatchRecord(match!!.toMatchRecord(outcome))
        notifyMatchCompleted(match, winningPlayer.playerId, "resignation")
    }

}
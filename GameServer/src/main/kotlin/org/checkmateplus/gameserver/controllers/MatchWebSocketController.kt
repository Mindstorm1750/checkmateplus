package org.checkmateplus.gameserver.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.checkmateplus.chessLibrary.botEngine.calculateMoveTimeDelayMs
import org.checkmateplus.chessLibrary.chessEngine.Colour
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
import org.checkmateplus.gameserver.service.MatchService
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal
import kotlin.concurrent.atomics.ExperimentalAtomicApi

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
    private val matchService: MatchService
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
            handleGameEndConditions(matchId, matchState)

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
                                timeRemaining = (if (match.activeTurn == Colour.WHITE) match.whitePlayerTime else match.blackPlayerTime).toDouble(),
                                timeOpponentTook = timePlayerTook.toDouble(),
                                numberOfLegalMoves = match.board?.generateLegalMoves()?.size ?: 0,
                                inCheck = match.board?.isInCheck(match.activeTurn) ?: false,
                            )
                            val moveDeferred = async(Dispatchers.Default) {
                                matchService.calculateMove(match.board!!, match.botConfiguration.botModelType)
                            }
                            val delayDeferred = async {
                                delay(calculateMoveTimeDelayMs(match.botConfiguration.botMoveTimeModelType, moveTimeModelFeatures))
                            }
                            awaitAll(moveDeferred, delayDeferred)
                            moveDeferred.await()
                        }

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
                        val botStateMessage = matchService.createMatchStateMessage(match, botMoveNotification)

                        // Broadcast updated state to all players
                        messagingTemplate.convertAndSend(
                            "/topic/match/$matchId/state",
                            botStateMessage
                        )

                        // Send the specific move notification
                        matchState.lastMove?.let { lastMove ->
                            messagingTemplate.convertAndSend(
                                "/topic/match/$matchId/moves",
                                botMoveNotification
                            )
                        }
                        handleGameEndConditions(matchId, botStateMessage)
                    }
                }
            }

            // Handle game end conditions
            handleGameEndConditions(matchId, matchState)

        } catch (e: IllegalArgumentException) {
            // Send error to the player who made the invalid move
            messagingTemplate.convertAndSendToUser(
                moveMessage.playerId,
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

    /**
     * Handles game end conditions such as checkmate, stalemate, or other draws.
     *
     * @param matchId The unique identifier of the match.
     * @param matchState The current state of the match.
     */
    private fun handleGameEndConditions(matchId: String, matchState: MatchStateMessage) {
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
                matchService.addMatchRecord(MatchRecord(
                    board = match?.board!!,
                    players = match.players,
                    matchId = match.matchId,
                    timeControl = match.timeControl,
                    matchType = match.matchType,
                    botConfiguration = match.botConfiguration,
                    matchOutcome = outcome,
                    whitePlayer = match.whitePlayer!!,
                    blackPlayer = match.blackPlayer!!,
                    remainingWhitePlayerTime = match.whitePlayerTime,
                    remainingBlackPlayerTime = match.blackPlayerTime,
                    matchStatus = MatchStatus.COMPLETED,
                    history = match.turnHistory
                ))
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
                matchService.addMatchRecord(MatchRecord(
                    board = match?.board!!,
                    players = match.players,
                    matchId = match.matchId,
                    timeControl = match.timeControl,
                    matchType = match.matchType,
                    botConfiguration = match.botConfiguration,
                    matchOutcome = MatchOutcome.DRAW,
                    whitePlayer = match.whitePlayer!!,
                    blackPlayer = match.blackPlayer!!,
                    remainingWhitePlayerTime = match.whitePlayerTime,
                    remainingBlackPlayerTime = match.blackPlayerTime,
                    matchStatus = MatchStatus.COMPLETED,
                    history = match.turnHistory
                ))
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
                matchService.addMatchRecord(MatchRecord(
                    board = match?.board!!,
                    players = match.players,
                    matchId = match.matchId,
                    timeControl = match.timeControl,
                    matchType = match.matchType,
                    botConfiguration = match.botConfiguration,
                    matchOutcome = MatchOutcome.DRAW,
                    whitePlayer = match.whitePlayer!!,
                    blackPlayer = match.blackPlayer!!,
                    remainingWhitePlayerTime = match.whitePlayerTime,
                    remainingBlackPlayerTime = match.blackPlayerTime,
                    matchStatus = MatchStatus.COMPLETED,
                    history = match.turnHistory
                ))
            }
        }
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
        joinMessage: JoinMatchMessage
    ) {
        logger.info(joinMessage.toString())

        val playerId = joinMessage.playerId
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

        if (match.players.any { player -> player.playerId == playerId }) {
            match.connectedPlayers.add(joinMessage.playerId) // add the player as having connect to the match
        }
        val index = match.players.indexOfFirst { it.playerId == playerId }
        if (index != -1) { //-1 indicates not found!
            match.players[index] = when (val player = match.players[index]) {
                is Player.Anonymous -> player.copy(playerPreferences = playerPreferences)
                is Player.Bot -> player
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
            val matchState = matchService.createMatchStateMessage(match)
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/state",
                matchState
            )
        }

    }

    /**
     * Starts the match timer and broadcasts the match start event.
     *
     * @param matchId The unique identifier of the match.
     */
    @MessageMapping("/match/{matchId}/start")
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
            val botStateMessage = matchService.createMatchStateMessage(match, botMoveNotification)

            // Broadcast updated state to all players
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/state",
                botStateMessage
            )

            // Send the specific move notification
            botMatchStateMessage.lastMove?.let { lastMove ->
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/moves",
                    botMoveNotification
                )
            }
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
        drawOffer: DrawOfferMessage
    ) {
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
        response: DrawResponseMessage
    ) {
        if (response.accepted) {
            matchService.acceptDraw(matchId)
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
        resignMessage: ResignMessage
    ) {
        val winner = matchService.resign(matchId, resignMessage.playerId)

        messagingTemplate.convertAndSend(
            "/topic/match/$matchId/events",
            MatchEventMessage(
                matchId = matchId,
                eventType = MatchEventType.PLAYER_RESIGNED,
                message = "Player resigned. $winner wins!",
                data = mapOf(
                    "winner" to winner,
                    "resignedPlayer" to resignMessage.playerId
                )
            )
        )
    }

}
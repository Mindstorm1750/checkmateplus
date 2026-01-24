package org.chessmateplus.gameserver.controllers

import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.chessmateplus.gameserver.models.messages.DrawOfferMessage
import org.chessmateplus.gameserver.models.messages.DrawResponseMessage
import org.chessmateplus.gameserver.models.messages.JoinMatchMessage
import org.chessmateplus.gameserver.models.messages.MatchEventMessage
import org.chessmateplus.gameserver.models.messages.MatchEventType
import org.chessmateplus.gameserver.models.messages.MatchStateMessage
import org.chessmateplus.gameserver.models.messages.MoveMessage
import org.chessmateplus.gameserver.models.messages.ResignMessage
import org.chessmateplus.gameserver.service.MatchService
import java.security.Principal

@Controller
class MatchWebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val matchService: MatchService
) {
    //TODO: Need to add disconnect/reconnect support incase of connection drop




    @MessageMapping("/match/{matchId}/move")
    fun makeMove(
        @DestinationVariable matchId: String,
        principal: Principal,
        moveMessage: MoveMessage
    ) {
        try {
            if (principal.name != moveMessage.playerId) { //this implementation needs further work as the
                //principal is not really defined yet...
                throw IllegalAccessException("You cannot move for another player")
            }
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

    private fun handleGameEndConditions(matchId: String, matchState: MatchStateMessage) {
        when {
            matchState.isCheckmate -> {
                //if winner declared by engine
                val winner = "TEST WINNER" //TODO: Needs to be update with engine
                messagingTemplate.convertAndSend(
                    "/topic/match/$matchId/events",
                    MatchEventMessage(
                        matchId = matchId,
                        eventType = MatchEventType.MATCH_ENDED,
                        message = "Checkmate! $winner wins!",
                        data = mapOf(
                            "result" to "checkmate",
                            "winner" to winner
                        )
                    )
                )
                matchService.stopTimer(matchId)
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
            }
        }
    }

    @MessageMapping("/match/{matchId}/join")
    fun joinMatch(
        @DestinationVariable matchId: String,
        joinMessage: JoinMatchMessage
    ) {

        //this need to be a reffered join from the matchmake manager
        val success = true
        if (success) {
            messagingTemplate.convertAndSend(
                "/topic/match/$matchId/events",
                MatchEventMessage(
                    matchId = matchId,
                    eventType = MatchEventType.PLAYER_JOINED,
                    message = "${joinMessage.playerName} joined the match"
                )
            )

            val match = matchService.getMatch(matchId)
            if (match != null) {
                val matchState = matchService.createMatchStateMessage(match)
                messagingTemplate.convertAndSendToUser(
                    joinMessage.playerId,
                    "/queue/match/$matchId/state",
                    matchState
                )
            }
            //when both players are connected start the match
            if (matchService.getMatch(matchId) != null && matchService.getMatch(matchId)!!.players.size == 2) {
                startMatch(matchId)
            }
        }
    }
    //might depreciate this in favour of automated start system once players have joined.
    //Can use this for a pause incase of disconnect feature
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
    }

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
package org.checkmateplus.matchmakingservice.controllers

import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.matchmakingservice.models.messages.JoinRequest
import org.checkmateplus.matchmakingservice.models.Match
import org.checkmateplus.matchmakingservice.models.messages.MatchFoundEvent
import org.checkmateplus.matchmakingservice.models.messages.ServerAllocationResult
import org.checkmateplus.matchmakingservice.services.MatchmakingService
import org.slf4j.LoggerFactory
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import java.security.Principal
import kotlin.time.ExperimentalTime

/**
 * WebSocket controller for handling matchmaking requests.
 *
 * This controller manages the entry of players into the matchmaking queue
 * and notifies them when a match is found and a server is allocated.
 *
 * @property matchmakingService Service for queue and match management.
 * @property messagingTemplate Template for sending messages to specific users.
 */
@RegisterReflectionForBinding(
    MatchFoundEvent::class,
    JoinRequest::class,
    ServerAllocationResult::class
)
@Controller
class MatchmakingController(
    private val matchmakingService: MatchmakingService,
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)


    /**
     * Handles a request to join the matchmaking queue.
     * If a match is formed, it immediately requests a server allocation and notifies players.
     *
     * @param request Details of the join request, including display name.
     * @param principal The authenticated user joining the queue.
     */
    @OptIn(ExperimentalTime::class)
    @MessageMapping("/join")
    fun joinGame(@Payload request: JoinRequest, principal: Principal) {
        //  Use the ID from the principal
        val securePlayerId = principal.name

        // Create the player object using the ID
        val player = Player.Anonymous(
            displayName = request.displayName,
            playerId = securePlayerId
        )

        logger.info("Player $securePlayerId (${player.displayName}) joining queue")

        try {
            // Add to queue
            val match = matchmakingService.joinQueue(
                player,
                matchType = request.matchType,
                timeControl = request.timeControl,
                botModelType = request.botModelType
            )

            if (match == null) {
                // Waiting for opponent
                messagingTemplate.convertAndSendToUser(
                    securePlayerId, // Use secure ID for routing
                    "/queue/status",
                    mapOf("status" to "QUEUED", "message" to "Waiting for opponent...")
                )
            } else {
                // Match Found
                logger.info("Match found! ${match.matchId} between ${match.players[0].playerId} and ${match.players[1].playerId}")

                val allocation = matchmakingService.requestServerForMatch(
                    match.matchId,
                    match.players[0],
                    match.players[1],
                    match.timeControl,
                    match.matchType,
                    match.botModelType
                )

                if (allocation.success) {
                    notifyPlayers(match, allocation)
                } else {
                    logger.error("Failed to allocate server for match ${match.matchId}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error in joinGame for player $securePlayerId", e)
        }
    }

    /**
     * Notifies both players in a match about the match details and allocated server.
     *
     * @param match The match that was formed.
     * @param allocation The server allocation details.
     */
    private fun notifyPlayers(match: Match, allocation: ServerAllocationResult) {
        match.players.forEachIndexed { index, player ->
            val opponent = match.players[if (index == 0) 1 else 0]

            messagingTemplate.convertAndSendToUser(
                player.playerId,
                "/queue/match",
                MatchFoundEvent(
                    matchId = match.matchId,
                    websocketUrl = allocation.websocketUrl,
                    opponentName = opponent.displayName,
                    playerId = player.playerId,
                )
            )
        }
    }
}

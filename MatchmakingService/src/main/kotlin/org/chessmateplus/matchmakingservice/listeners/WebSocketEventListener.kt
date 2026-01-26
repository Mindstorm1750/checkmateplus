package org.chessmateplus.matchmakingservice.listeners

import org.chessmateplus.matchmakingservice.services.MatchmakingService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

/**
 * Listener for WebSocket lifecycle events.
 *
 * This component ensures that players are removed from matchmaking queues if their
 * WebSocket connection is terminated.
 */
@Component
class WebSocketEventListener(private val matchmakingService: MatchmakingService) {

    /**
     * Handles WebSocket disconnection events by removing the disconnected user from the queue.
     *
     * @param event The session disconnect event containing user details.
     */
    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val user = event.user
        if (user != null) {
            // Remove them from Redis if they disconnect
            matchmakingService.leaveQueue(user.name)
        }
    }
}
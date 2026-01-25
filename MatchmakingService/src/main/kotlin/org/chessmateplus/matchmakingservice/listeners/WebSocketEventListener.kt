package org.chessmateplus.matchmakingservice.listeners

import org.chessmateplus.matchmakingservice.services.MatchmakingService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

@Component
class WebSocketEventListener(private val matchmakingService: MatchmakingService) {

    @EventListener
    fun handleWebSocketDisconnectListener(event: SessionDisconnectEvent) {
        val user = event.user
        if (user != null) {
            // Remove them from Redis if they disconnect
            matchmakingService.leaveQueue(user.name)
        }
    }
}
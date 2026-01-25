package org.chessmateplus.matchmakingservice.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(private val customHandshakeHandler: CustomHandshakeHandler) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        // Memory-based message broker to send messages back to the client
        config.enableSimpleBroker("/topic", "/queue")
        config.setApplicationDestinationPrefixes("/app")

        // 1-1 notifications
        config.setUserDestinationPrefix("/user")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // The endpoint clients connect to: ws://localhost:8080/matchmaking
        registry.addEndpoint("/matchmaking")
            .setAllowedOriginPatterns("*") // Allow all origins for dev; restrict in prod
            .setHandshakeHandler(customHandshakeHandler)
            .withSockJS() // Fallback options for older browsers
    }
}

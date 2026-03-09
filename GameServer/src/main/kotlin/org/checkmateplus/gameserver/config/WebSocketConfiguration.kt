package org.checkmateplus.gameserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration(private val customHandshakeHandler: CustomHandshakeHandler, @Value($$"${allowed-origins}") private val allowedOrigins: String) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/match")
            .setAllowedOriginPatterns(*allowedOrigins.split(",").toTypedArray())
            .setHandshakeHandler(customHandshakeHandler)
            .withSockJS()
    }

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.setApplicationDestinationPrefixes("/app")
        config.enableSimpleBroker("/topic", "/queue")
        config.setUserDestinationPrefix("/user")
    }
}

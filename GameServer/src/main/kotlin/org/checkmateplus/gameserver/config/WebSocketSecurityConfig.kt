package org.checkmateplus.gameserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager
import org.springframework.messaging.Message
@Configuration
@EnableWebSocketSecurity
class WebSocketSecurityConfig {

    @Bean
    fun messageAuthorizationManager(
        messages: MessageMatcherDelegatingAuthorizationManager.Builder
    ): AuthorizationManager<Message<*>> {
        return messages
            // Allow connection handshake
            .nullDestMatcher().permitAll()

            // Allow subscription to topics and queues
            .simpSubscribeDestMatchers("/topic/**").permitAll()
            .simpSubscribeDestMatchers("/queue/**").permitAll()
            .simpSubscribeDestMatchers("/user/queue/**").permitAll()

            // Allow sending messages to /app/match/** endpoints
            .simpDestMatchers("/app/match/**").permitAll()

            // Deny everything else
            .anyMessage().denyAll()
            .build()
    }

    @Bean("csrfChannelInterceptor")
    fun csrfChannelInterceptor(): ChannelInterceptor {
        return object : ChannelInterceptor {}
    }
}
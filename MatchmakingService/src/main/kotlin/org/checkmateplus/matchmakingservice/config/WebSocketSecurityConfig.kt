package org.checkmateplus.matchmakingservice.config

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
            .nullDestMatcher().authenticated()          // CONNECT frame
            .simpDestMatchers("/app/join").authenticated()
            .simpSubscribeDestMatchers("/user/queue/**", "/topic/**").authenticated()
            .anyMessage().denyAll()
            .build()
    }

    @Bean("csrfChannelInterceptor")
    fun csrfChannelInterceptor(): ChannelInterceptor {
        return object : ChannelInterceptor {}
    }
}

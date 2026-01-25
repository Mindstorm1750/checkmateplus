package org.chessmateplus.matchmakingservice.config

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
            .nullDestMatcher().permitAll()
            .simpDestMatchers("/app/join").permitAll()

            .simpSubscribeDestMatchers("/user/queue/**", "/topic/**").permitAll() //this needs to be "authenticated"
            .anyMessage().denyAll()
            .build()
    }

    // this disables csrf!
    @Bean("csrfChannelInterceptor")
    fun csrfChannelInterceptor(): ChannelInterceptor {
        return object : ChannelInterceptor {}
    }
}
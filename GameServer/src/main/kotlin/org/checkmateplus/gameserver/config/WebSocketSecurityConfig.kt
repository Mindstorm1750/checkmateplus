package org.checkmateplus.gameserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager
import org.springframework.messaging.Message
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.security.messaging.access.intercept.AuthorizationChannelInterceptor
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
class WebSocketSecurityConfig(
    private val jwtChannelInterceptor: JwtChannelInterceptor
) : WebSocketMessageBrokerConfigurer {

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        val authManager = MessageMatcherDelegatingAuthorizationManager.builder()
            .nullDestMatcher().authenticated()
            .simpSubscribeDestMatchers("/topic/**").authenticated()
            .simpSubscribeDestMatchers("/user/queue/**").authenticated()
            .simpDestMatchers("/app/match/**").authenticated()
            .anyMessage().denyAll()
            .build()

        registration.interceptors(
            jwtChannelInterceptor,
            AuthorizationChannelInterceptor(authManager)
        )
    }

    @Bean("csrfChannelInterceptor")
    fun csrfChannelInterceptor(): ChannelInterceptor = object : ChannelInterceptor {}
}
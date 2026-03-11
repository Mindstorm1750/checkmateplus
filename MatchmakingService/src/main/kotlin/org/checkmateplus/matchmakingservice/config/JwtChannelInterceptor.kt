package org.checkmateplus.matchmakingservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageDeliveryException
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import javax.crypto.spec.SecretKeySpec

@Component
class JwtChannelInterceptor(
    @Value("\${jwt.secret}") private val jwtSecret: String
) : ChannelInterceptor {

    private val decoder = NimbusJwtDecoder
        .withSecretKey(SecretKeySpec(jwtSecret.toByteArray(), "HmacSHA256"))
        .build()

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)

        if (accessor?.command == StompCommand.CONNECT) {
            val token = accessor.getFirstNativeHeader("Authorization")
                ?.removePrefix("Bearer ")
                ?: throw MessageDeliveryException("Missing token")

            val jwt = decoder.decode(token)
            val auth = JwtAuthenticationToken(jwt)
            accessor.user = auth
        }

        return message
    }
}
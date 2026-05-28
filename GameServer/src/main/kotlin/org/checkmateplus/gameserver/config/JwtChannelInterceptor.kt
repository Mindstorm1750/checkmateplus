package org.checkmateplus.gameserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
class JwtChannelInterceptor(
    @Value("\${jwt.secret}") private val jwtSecret: String
) : ChannelInterceptor {

    private val decoder = NimbusJwtDecoder
        .withSecretKey(SecretKeySpec(Base64.getDecoder().decode(jwtSecret), "HmacSHA256"))
        .build()

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        val command = accessor.command
        if (command != StompCommand.CONNECT &&
            command != StompCommand.SEND &&
            command != StompCommand.SUBSCRIBE &&
            command != StompCommand.DISCONNECT) {
            return message
        }

        val token = accessor.getFirstNativeHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        if (token == null) {
            // SUBSCRIBE/SEND frames don't carry the header — reuse the session user from CONNECT
            val existingUser = accessor.user
            if (existingUser is JwtAuthenticationToken && existingUser.isAuthenticated) {
                val context = SecurityContextHolder.createEmptyContext()
                context.authentication = existingUser
                SecurityContextHolder.setContext(context)
            }
            return message
        }

        val jwt = decoder.decode(token)
        val auth = JwtAuthenticationToken(
            jwt,
            listOf(SimpleGrantedAuthority("ROLE_USER")),
        ).also { it.isAuthenticated = true }

        accessor.user = auth

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = auth
        SecurityContextHolder.setContext(context)

        return message
    }

    override fun afterSendCompletion(
        message: Message<*>,
        channel: MessageChannel,
        sent: Boolean,
        ex: Exception?
    ) {
        SecurityContextHolder.clearContext()
    }
}
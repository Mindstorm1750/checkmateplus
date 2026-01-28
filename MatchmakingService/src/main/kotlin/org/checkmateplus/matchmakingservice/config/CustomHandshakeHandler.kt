package org.checkmateplus.matchmakingservice.config

import org.springframework.http.server.ServerHttpRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import java.security.Principal
import java.util.UUID
@Component
class CustomHandshakeHandler : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Principal {
        val query = request.uri.query
        val playerId = query?.split("&")
            ?.find { it.startsWith("playerId=") }
            ?.substringAfter("playerId=") ?: "anon-${UUID.randomUUID()}"

        // This creates a spring security user
        return AnonymousAuthenticationToken(
            "key",
            playerId,
            AuthorityUtils.createAuthorityList("ROLE_PLAYER")
        )
    }
}
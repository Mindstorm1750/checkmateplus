package org.checkmateplus.playerservice.controllers

import jakarta.servlet.http.HttpServletRequest
import org.checkmateplus.playerservice.models.AuthResponse
import org.checkmateplus.playerservice.models.PlayerProfile
import org.checkmateplus.playerservice.models.RegisterResponse
import org.checkmateplus.playerservice.services.AuthenticationService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AuthenticationGraphQLController(
    private val authService: AuthenticationService,
    private val request: HttpServletRequest
) {

    @MutationMapping
    fun register(@Argument username: String): RegisterResponse {
        val response = authService.register(username)
        // store playerId in session for confirmTotp step
        request.session.setAttribute("pending_player_id", response.playerId)
        return response
    }

    @MutationMapping
    fun confirmTotp(@Argument code: String): AuthResponse {
        val playerId = request.session.getAttribute("pending_player_id") as? String
            ?: throw IllegalStateException("No pending registration found")
        val response = authService.confirmTotp(playerId, code)
        request.session.removeAttribute("pending_player_id")
        return response
    }

    @MutationMapping
    fun loginWithTotp(
        @Argument username: String,
        @Argument code: String
    ): AuthResponse = authService.loginWithTotp(username, code)

    @MutationMapping
    fun loginAsGuest(): AuthResponse = authService.loginAsGuest()

    @QueryMapping
    fun me(): PlayerProfile {
        val playerId = request.session.getAttribute("playerId") as? String
            ?: throw IllegalStateException("Not authenticated")
        return authService.getProfile(playerId)
    }
}
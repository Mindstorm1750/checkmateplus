package org.checkmateplus.playerservice.configs

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.checkmateplus.playerservice.services.AuthenticationService
import org.checkmateplus.playerservice.services.JwtService
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class GoogleOAuth2SuccessHandler(
    private val authService: AuthenticationService,
    private val jwtService: JwtService
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User
        val googleId = oauth2User.getAttribute<String>("sub")!!

        val authResponse = authService.loginWithGoogle(googleId)

        // redirect to frontend with JWT as query param
        // in production use a short-lived code instead
        response.sendRedirect("http://localhost:5173/auth/callback?token=${authResponse.jwt}")
    }
}
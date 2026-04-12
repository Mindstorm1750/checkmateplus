package org.checkmateplus.playerservice.configs

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.checkmateplus.playerservice.services.AuthenticationService
import org.checkmateplus.playerservice.services.OAuthCodeService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class GoogleOAuth2SuccessHandler(
    private val authService: AuthenticationService,
    private val oAuthCodeService: OAuthCodeService,
    @Value("\${frontend.url}") private val frontendUrl: String
) : AuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest, response: HttpServletResponse, authentication: Authentication
    ) {
        val oauth2User = authentication.principal as OAuth2User
        val googleId = oauth2User.getAttribute<String>("sub")!!

        val authResponse = authService.loginWithGoogle(googleId)
        val code = oAuthCodeService.storeCode(authResponse)

        response.sendRedirect("$frontendUrl/auth/callback?code=$code")
    }
}
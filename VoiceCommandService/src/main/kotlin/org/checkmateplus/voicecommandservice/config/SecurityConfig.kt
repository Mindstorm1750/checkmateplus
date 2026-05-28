package org.checkmateplus.voicecommandservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class SecurityConfig(@Value("\${jwt.secret}") private val jwtSecret: String) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        val decoded = Base64.getDecoder().decode(jwtSecret)
        val secretKey = SecretKeySpec(decoded, "HmacSHA256")
        http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                // allow api/voice
                auth
                    .requestMatchers("/api/voice/**").permitAll()
                    .requestMatchers("/actuator/health/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(NimbusJwtDecoder.withSecretKey(secretKey).build())
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .cors { }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(
        @Value("\${allowed-origins}") allowedOrigins: String
    ): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = allowedOrigins.split(",")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}


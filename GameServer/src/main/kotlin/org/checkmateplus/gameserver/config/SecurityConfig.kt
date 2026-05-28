package org.checkmateplus.gameserver.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class SecurityConfig(@Value("\${jwt.secret}") private val jwtSecret: String) {

    @Bean
    fun filterChain(http: HttpSecurity, ): SecurityFilterChain {
        val decoded = Base64.getDecoder().decode(jwtSecret)
        val secretKey = SecretKeySpec(decoded, "HmacSHA256")
        http
            .csrf { it.disable() } // Disable CSRF for game APIs
            .authorizeHttpRequests { auth ->
                auth.requestMatchers("/actuator/health/**").permitAll()
                auth.requestMatchers("/match/**").permitAll() // Handled by WS security
                auth.requestMatchers("/api/**").permitAll()
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.decoder(NimbusJwtDecoder.withSecretKey(secretKey).build())
                }
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        return http.build()
    }
}

package org.checkmateplus.playerservice.services

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiry-ms:86400000}") private val expiryMs: Long,
    @Value("\${jwt.guest-expiry-ms:3600000}") private val guestExpiryMs: Long
) {
    private val secretKey: SecretKey by lazy {
        val decoded = Base64.getDecoder().decode(secret)
        SecretKeySpec(decoded, "HmacSHA256")
    }

    val encoder: JwtEncoder by lazy {
        NimbusJwtEncoder(ImmutableSecret(secretKey))
    }

    val decoder: JwtDecoder by lazy {
        NimbusJwtDecoder.withSecretKey(secretKey).build()
    }

    fun generate(playerId: String, displayName: String, isGuest: Boolean = false): String {
        val expiry = if (isGuest) guestExpiryMs else expiryMs
        val now = Clock.System.now()
        val claims = JwtClaimsSet.builder()
            .subject(playerId)
            .issuedAt(now.toJavaInstant())
            .expiresAt((now + expiry.milliseconds).toJavaInstant())
            .issuer("checkmateplus")
            .claim("displayName", displayName)
            .claim("guest", isGuest)
            .build()

        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }

    fun validate(token: String): Jwt = decoder.decode(token)
}
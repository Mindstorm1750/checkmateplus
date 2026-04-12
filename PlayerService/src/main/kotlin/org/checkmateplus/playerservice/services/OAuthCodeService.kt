package org.checkmateplus.playerservice.services

import kotlinx.serialization.json.Json
import org.checkmateplus.playerservice.models.AuthResponse
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class OAuthCodeService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val json: Json
) {
    companion object {
        private const val KEY_PREFIX = "oauth_code:"
        private val TTL = Duration.ofSeconds(60)
    }

    fun storeCode(authResponse: AuthResponse): String {
        val code = UUID.randomUUID().toString()
        redisTemplate.opsForValue().set(KEY_PREFIX + code, json.encodeToString(authResponse), TTL)
        return code
    }

    fun exchangeCode(code: String): AuthResponse? {
        val key = KEY_PREFIX + code
        val serialized = redisTemplate.opsForValue().getAndDelete(key) ?: return null
        return json.decodeFromString(serialized)
    }
}
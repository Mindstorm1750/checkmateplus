package org.checkmateplus.playerservice.configs

import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(redisConnectionFactory: RedisConnectionFactory, objectMapper: ObjectMapper): RedisCacheManager {
        val valueSerializer = SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer(objectMapper))

        val base = RedisCacheConfiguration.defaultCacheConfig()
            .serializeValuesWith(valueSerializer)
            .disableCachingNullValues()

        return RedisCacheManager.builder(redisConnectionFactory)
            .withCacheConfiguration("playerStats", base.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("leaderboard", base.entryTtl(Duration.ofSeconds(60)))
            .build()
    }
}
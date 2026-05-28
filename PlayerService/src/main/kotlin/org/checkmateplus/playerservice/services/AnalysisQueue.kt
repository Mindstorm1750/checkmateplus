package org.checkmateplus.playerservice.services

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
//we are using redis BLPOP see https://redis.io/docs/latest/commands/blpop/
@Service
class AnalysisQueue(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${analysis.queue.key:analysis:queue}") private val queueKey: String,
) {
    fun enqueue(matchId: String) {
        redisTemplate.opsForList().rightPush(queueKey, matchId)
    }

    fun take(timeout: Duration): String? =
        redisTemplate.opsForList().leftPop(queueKey, timeout)

    fun depth(): Long = redisTemplate.opsForList().size(queueKey) ?: 0
}

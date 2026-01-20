package uk.ac.kent.matchmakingservice

import kotlinx.serialization.json.Json
import org.springframework.data.redis.core.RedisTemplate
import uk.ac.kent.chessLibrary.playerModels.Player
import uk.ac.kent.matchmakingservice.models.Match
import uk.ac.kent.chessLibrary.playerModels.MatchStatus
import uk.ac.kent.chessLibrary.playerModels.TimeControl
import kotlin.time.ExperimentalTime

class MatchmakingService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val QUEUE_PREFIX = "matchmaking:queue:"
        private const val PLAYER_QUEUE_MAP = "matchmaking:player_queue_map"
        private const val ACTIVE_MATCHES = "matchmaking:active_matches"
    }

    /**
     * Add a player to the matchmaking queue
     */
    fun joinQueue(player: Player): Match? {
        val timeControl: TimeControl = TimeControl.CLASSICAL_30_0

        // Check if player already in queue
        val existingQueue = redisTemplate.opsForHash<String, String>()
            .get(PLAYER_QUEUE_MAP, player.playerId)
        if (existingQueue != null) {
            throw IllegalStateException("Player ${player.playerId} is already in queue")
        }

        val queueKey = "$QUEUE_PREFIX${timeControl.name}"


        val opponentJson = redisTemplate.opsForList().rightPop(queueKey)

        return if (opponentJson != null) {
            val opponent = deserializePlayer(opponentJson)

            redisTemplate.opsForHash<String, String>()
                .delete(PLAYER_QUEUE_MAP, opponent.playerId)

            createMatch(listOf(opponent, player), timeControl)
        } else {

            val playerJson = serializePlayer(player)
            redisTemplate.opsForList().leftPush(queueKey, playerJson)


            redisTemplate.opsForHash<String, String>()
                .put(PLAYER_QUEUE_MAP, player.playerId, timeControl.name)

            null
        }
    }

    /**
     * Remove a player from queue
     */
    fun leaveQueue(playerId: String): Boolean {
        val timeControlName = redisTemplate.opsForHash<String, String>()
            .get(PLAYER_QUEUE_MAP, playerId) ?: return false

        val queueKey = "$QUEUE_PREFIX$timeControlName"


        val queueSize = redisTemplate.opsForList().size(queueKey) ?: 0
        val players = redisTemplate.opsForList().range(queueKey, 0, queueSize)

        var removed = false
        players?.forEach { playerJson ->
            val player = deserializePlayer(playerJson)
            if (player.playerId == playerId) {
                redisTemplate.opsForList().remove(queueKey, 1, playerJson)
                removed = true
            }
        }

        if (removed) {
            redisTemplate.opsForHash<String, String>()
                .delete(PLAYER_QUEUE_MAP, playerId)
        }

        return removed
    }

    /**
     * Get queue sizes for all time controls
     */
    fun getQueueSizes(): Map<TimeControl, Int> {
        return TimeControl.entries.associateWith { timeControl ->
            val queueKey = "$QUEUE_PREFIX${timeControl.name}"
            (redisTemplate.opsForList().size(queueKey) ?: 0).toInt()
        }
    }

    /**
     * Check if player is in queue
     */
    fun isInQueue(playerId: String): Boolean {
        return redisTemplate.opsForHash<String, String>()
            .hasKey(PLAYER_QUEUE_MAP, playerId)
    }

    /**
     * Get active match
     */
    fun getMatch(matchId: String): Match? {
        val matchJson = redisTemplate.opsForHash<String, String>()
            .get(ACTIVE_MATCHES, matchId) ?: return null
        return deserializeMatch(matchJson)
    }

    /**
     * Update match status
     */
    @OptIn(ExperimentalTime::class)
    fun updateMatchStatus(matchId: String, status: MatchStatus): Match? {
        val matchJson = redisTemplate.opsForHash<String, String>()
            .get(ACTIVE_MATCHES, matchId) ?: return null

        val match = deserializeMatch(matchJson)
        val updatedMatch = match.copy(status = status)

        if (status in listOf(MatchStatus.COMPLETED, MatchStatus.CANCELLED, MatchStatus.ABANDONED)) {
            redisTemplate.opsForHash<String, String>()
                .delete(ACTIVE_MATCHES, matchId)
        } else {
            redisTemplate.opsForHash<String, String>()
                .put(ACTIVE_MATCHES, matchId, serializeMatch(updatedMatch))
        }

        return updatedMatch
    }

    @OptIn(ExperimentalTime::class)
    private fun createMatch(players: List<Player>, timeControl: TimeControl): Match {
        val match = Match(
            players = players,
            timeControl = timeControl,
            status = MatchStatus.PENDING
        )

        // Store match in Redis
        redisTemplate.opsForHash<String, String>()
            .put(ACTIVE_MATCHES, match.matchId, serializeMatch(match))

        return match
    }

    private fun serializePlayer(player: Player): String {
        return Json.encodeToString(player)
    }

    private fun deserializePlayer(jsonString: String): Player {
        return Json.decodeFromString<Player>(jsonString)
    }

    private fun serializeMatch(match: Match): String {
        return Json.encodeToString(match)
    }

    private fun deserializeMatch(jsonString: String): Match {
        return Json.decodeFromString<Match>(jsonString)
    }
}
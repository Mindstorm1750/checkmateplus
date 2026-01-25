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

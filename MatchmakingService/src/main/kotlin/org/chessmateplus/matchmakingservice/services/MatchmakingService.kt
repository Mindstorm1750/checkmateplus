package org.chessmateplus.matchmakingservice.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.serialization.json.Json
import org.chessmateplus.chessLibrary.playerModels.MatchStatus
import org.chessmateplus.chessLibrary.playerModels.Player
import org.chessmateplus.chessLibrary.playerModels.TimeControl
import org.chessmateplus.gameserver.proto.GameManagerGrpc
import org.chessmateplus.gameserver.proto.GameManagerGrpc.newBlockingStub
import org.chessmateplus.gameserver.proto.GameServerProto

import org.chessmateplus.matchmakingservice.models.Match
import org.chessmateplus.matchmakingservice.models.ServerAllocationResult
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

@Service
class MatchmakingService(
    private val redisTemplate: RedisTemplate<String, String>
) {

    companion object {
        private const val QUEUE_PREFIX = "matchmaking:queue:"
        private const val PLAYER_QUEUE_MAP = "matchmaking:player_queue_map"
        private const val ACTIVE_MATCHES = "matchmaking:active_matches"
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var channel: ManagedChannel
    private lateinit var stub: GameManagerGrpc.GameManagerBlockingStub

    @PostConstruct
    fun initialize() {
        channel = ManagedChannelBuilder.forAddress("game-manager-service", 9090) //this need to match a k8s instance
            .usePlaintext().build()

        stub = newBlockingStub(channel)

        logger.info("Connected to Game Manager at localhost:9090")
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    /**
     * Add a player to the matchmaking queue
     */
    fun joinQueue(player: Player): Match? {
        val timeControl: TimeControl = TimeControl.CLASSICAL_30_0

        // Check if player already in queue
        val existingQueue = redisTemplate.opsForHash<String, String>().get(PLAYER_QUEUE_MAP, player.playerId)
        if (existingQueue != null) {
            throw IllegalStateException("Player ${player.playerId} is already in queue")
        }

        val queueKey = "$QUEUE_PREFIX${timeControl.name}"


        val opponentJson = redisTemplate.opsForList().rightPop(queueKey)

        return if (opponentJson != null) {
            val opponent = deserializePlayer(opponentJson)

            redisTemplate.opsForHash<String, String>().delete(PLAYER_QUEUE_MAP, opponent.playerId)

            createMatch(listOf(opponent, player), timeControl)
        } else {

            val playerJson = serializePlayer(player)
            redisTemplate.opsForList().leftPush(queueKey, playerJson)


            redisTemplate.opsForHash<String, String>().put(PLAYER_QUEUE_MAP, player.playerId, timeControl.name)

            null
        }
    }

    /**
     * Remove a player from queue
     */
    fun leaveQueue(playerId: String): Boolean {
        val timeControlName = redisTemplate.opsForHash<String, String>().get(PLAYER_QUEUE_MAP, playerId) ?: return false

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
            redisTemplate.opsForHash<String, String>().delete(PLAYER_QUEUE_MAP, playerId)
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
        return redisTemplate.opsForHash<String, String>().hasKey(PLAYER_QUEUE_MAP, playerId)
    }

    /**
     * Get active match
     */
    fun getMatch(matchId: String): Match? {
        val matchJson = redisTemplate.opsForHash<String, String>().get(ACTIVE_MATCHES, matchId) ?: return null
        return deserializeMatch(matchJson)
    }

    /**
     * Update match status
     */
    @OptIn(ExperimentalTime::class)
    fun updateMatchStatus(matchId: String, status: MatchStatus): Match? {
        val matchJson = redisTemplate.opsForHash<String, String>().get(ACTIVE_MATCHES, matchId) ?: return null

        val match = deserializeMatch(matchJson)
        val updatedMatch = match.copy(status = status)

        if (status in listOf(MatchStatus.COMPLETED, MatchStatus.CANCELLED, MatchStatus.ABANDONED)) {
            redisTemplate.opsForHash<String, String>().delete(ACTIVE_MATCHES, matchId)
        } else {
            redisTemplate.opsForHash<String, String>().put(ACTIVE_MATCHES, matchId, serializeMatch(updatedMatch))
        }

        return updatedMatch
    }

    @OptIn(ExperimentalTime::class)
    private fun createMatch(players: List<Player>, timeControl: TimeControl): Match {
        val match = Match(
            players = players, timeControl = timeControl, status = MatchStatus.PENDING
        )

        // Store match in Redis
        redisTemplate.opsForHash<String, String>().put(ACTIVE_MATCHES, match.matchId, serializeMatch(match))

        return match
    }

    private fun serializePlayer(player: Player): String {
        return Json.Default.encodeToString(player)
    }

    private fun deserializePlayer(jsonString: String): Player {
        return Json.Default.decodeFromString<Player>(jsonString)
    }

    private fun serializeMatch(match: Match): String {
        return Json.Default.encodeToString(match)
    }

    private fun deserializeMatch(jsonString: String): Match {
        return Json.Default.decodeFromString<Match>(jsonString)
    }

    /**
     * Request a game server for a match
     */
    fun requestServerForMatch(
        matchId: String, whitePlayer: Player, blackPlayer: Player, timeControl: TimeControl
    ): ServerAllocationResult {

        try {
            logger.info("Requesting server for match $matchId")
            logger.info("  White: ${whitePlayer.displayName} ")
            logger.info("  Black: ${blackPlayer.displayName} ")
            logger.info("  Time: ${timeControl.name} (${timeControl.minutes}+${timeControl.increment})")

            val request =
                GameServerProto.ServerRequest.newBuilder().setMatchId(matchId).setWhitePlayerId(whitePlayer.playerId)
                    .setWhitePlayerName(whitePlayer.displayName)
                    .setBlackPlayerId(blackPlayer.playerId).setBlackPlayerName(blackPlayer.displayName)
                    .setInitialTimeMinutes(timeControl.minutes)
                    .setIncrementSeconds(timeControl.increment).build()

            val response = stub.requestServer(request)

            if (response.success) {
                val server = response.server
                logger.info("Server allocated: ${server.serverId}")
                logger.info("  WebSocket URL: ${server.websocketUrl}")
                logger.info("  Estimated wait: ${server.estimatedWaitSeconds}s")

                return ServerAllocationResult(
                    success = true,
                    serverId = server.serverId,
                    matchId = server.matchId,
                    websocketUrl = server.websocketUrl,
                    estimatedWaitSeconds = server.estimatedWaitSeconds,
                    message = response.message
                )
            } else {
                logger.error("Server allocation failed: ${response.message}")
                return ServerAllocationResult(
                    success = false, message = response.message
                )
            }

        } catch (e: Exception) {
            logger.error("Failed to request server", e)
            return ServerAllocationResult(
                success = false, message = "Failed to communicate with manager: ${e.message}"
            )
        }
    }
}

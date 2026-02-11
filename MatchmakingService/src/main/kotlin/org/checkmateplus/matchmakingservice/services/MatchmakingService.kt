package org.checkmateplus.matchmakingservice.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.playerModels.MatchStatus
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl

import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.ServerAllocatorGrpc
import org.checkmateplus.gameserver.proto.ServerAllocatorGrpc.newBlockingStub

import org.checkmateplus.matchmakingservice.models.Match
import org.checkmateplus.matchmakingservice.models.ServerAllocationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

/**
 * Service responsible for player matchmaking and match state management.
 *
 * This service uses Redis to manage matchmaking queues and active match states,
 * and communicates with the Game Manager via gRPC to allocate server resources for matches.
 *
 * @property redisTemplate Template for Redis operations.
 */
@Service
class MatchmakingService(
    private val redisTemplate: RedisTemplate<String, String>,
    @Value("\${gameservermanager.host}") private val managerHost: String,
    @Value("\${gameservermanager.port}") private val managerPort: Int
) {

    companion object {
        private const val QUEUE_PREFIX = "matchmaking:queue:"
        private const val PLAYER_QUEUE_MAP = "matchmaking:player_queue_map"
        private const val ACTIVE_MATCHES = "matchmaking:active_matches"
    }

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var channel: ManagedChannel
    private lateinit var stub: ServerAllocatorGrpc.ServerAllocatorBlockingStub

    /**
     * Initialises the gRPC connection to the Game Manager.
     */
    @PostConstruct
    fun initialise() {
        channel = ManagedChannelBuilder.forAddress(managerHost, managerPort) //this need to match a k8s instance
            .usePlaintext().build()

        stub = newBlockingStub(channel)

        logger.info("Connected to Game Manager at $managerHost:$managerPort")
    }

    /**
     * Shuts down the gRPC channel.
     */
    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    /**
     * Adds a player to the matchmaking queue for a specific time control.
     * If an opponent is found, a match is created immediately.
     *
     * @param player The player joining the queue.
     * @return A [Match] if an opponent was found and a match created, null otherwise.
     * @throws IllegalStateException if the player is already in a queue.
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
     * Removes a player from their current matchmaking queue.
     *
     * @param playerId The unique identifier of the player.
     * @return true if the player was successfully removed, false if not found in any queue.
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
     * Retrieves the current number of players waiting in queue for each time control.
     *
     * @return A map of [TimeControl] to queue size.
     */
    fun getQueueSizes(): Map<TimeControl, Int> {
        return TimeControl.entries.associateWith { timeControl ->
            val queueKey = "$QUEUE_PREFIX${timeControl.name}"
            (redisTemplate.opsForList().size(queueKey) ?: 0).toInt()
        }
    }

    /**
     * Checks if a player is currently in any matchmaking queue.
     *
     * @param playerId The unique identifier of the player.
     * @return true if the player is in a queue, false otherwise.
     */
    fun isInQueue(playerId: String): Boolean {
        return redisTemplate.opsForHash<String, String>().hasKey(PLAYER_QUEUE_MAP, playerId)
    }

    /**
     * Retrieves an active match by its ID.
     *
     * @param matchId The unique identifier of the match.
     * @return The [Match] if found, null otherwise.
     */
    fun getMatch(matchId: String): Match? {
        val matchJson = redisTemplate.opsForHash<String, String>().get(ACTIVE_MATCHES, matchId) ?: return null
        return deserializeMatch(matchJson)
    }

    /**
     * Updates the status of an active match.
     * If the match is completed or cancelled, it is removed from active matches.
     *
     * @param matchId The unique identifier of the match.
     * @param status The new status to apply.
     * @return The updated [Match] object.
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

    /**
     * Internal helper to create a match and store it in Redis.
     */
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
     * Requests a game server allocation for a match from the Game Manager.
     *
     * @param matchId The ID of the match to allocate.
     * @param whitePlayer The player assigned to play white.
     * @param blackPlayer The player assigned to play black.
     * @param timeControl The match's time control.
     * @return A [ServerAllocationResult] indicating success or failure and server details.
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

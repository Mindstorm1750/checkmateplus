package org.checkmateplus.matchmakingservice.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.playerModels.*
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.ServerAllocatorGrpc
import org.checkmateplus.gameserver.proto.ServerAllocatorGrpc.newBlockingStub
import org.checkmateplus.matchmakingservice.models.Match
import org.checkmateplus.matchmakingservice.models.QueueEntry
import org.checkmateplus.matchmakingservice.models.messages.ServerAllocationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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
    @Value("\${gameservermanager.port}") private val managerPort: Int,
    private val json: Json
) {

    companion object {
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
    fun joinQueue(
        player: Player, matchType: MatchType, timeControl: TimeControl?, botConfiguration: BotConfiguration?
    ): Match? {


        // Check if player already in queue
        val existingQueue = redisTemplate.opsForHash<String, String>().get(PLAYER_QUEUE_MAP, player.playerId)
        if (existingQueue != null) {
            throw IllegalStateException("Player ${player.playerId} is already in queue")
        }

        val entry = QueueEntry(
            player,
            matchType = matchType,
            timeControl = timeControl,
            mmr = player.skillRating,
        )

        when (matchType) {
            MatchType.PVE -> {
                requireNotNull(timeControl) { "timeControl is required for PvE queues" }
                requireNotNull(botConfiguration) { "botConfiguration is required for PvE queues" }
                val bot = Player.Bot(
                    botConfiguration = botConfiguration,
                )

                return createMatch(listOf(player, bot), matchType, timeControl, botConfiguration)

            }

            MatchType.CASUAL_PVP -> {
                //set handler
                val hashOps = redisTemplate.opsForHash<String, String>()
                //get all entries from the redis
                val allEntries = hashOps.entries(PLAYER_QUEUE_MAP).values.map { deserializeQueueEntry(it) }
                //match first player where the players game mode and time control match
                val opponent = allEntries.firstOrNull { candidate ->
                    candidate.player.playerId != entry.player.playerId && candidate.matchType == entry.matchType && isTimeControlCompatible(
                        entry.timeControl, candidate.timeControl
                    )
                }

                return if (opponent != null) {
                    hashOps.delete(PLAYER_QUEUE_MAP, opponent.player.playerId)
                    //if both players did not pick a time control we pick a random one :)
                    // perhaps the default should be Classic?
                    val timeControl = entry.timeControl ?: opponent.timeControl ?: TimeControl.entries.random()
                    logger.info("Match found: ${entry.player.playerId} vs ${opponent.player.playerId} [$timeControl]")
                    createMatch(listOf(entry.player, opponent.player), entry.matchType, timeControl, null)
                } else {
                    hashOps.put(PLAYER_QUEUE_MAP, entry.player.playerId, serializeQueueEntry(entry))
                    logger.info("Player ${entry.player.playerId} added to queue, no match found yet")
                    null
                }
            }

            MatchType.COMPETITIVE_PVP -> {
                val hashOps = redisTemplate.opsForHash<String, String>()

                val allEntries = hashOps.entries(PLAYER_QUEUE_MAP).values.map { deserializeQueueEntry(it) }

                val playerMmr = entry.player.skillRating
                    ?: throw IllegalStateException("Player ${entry.player.playerId} has no MMR for competitive queue")

                val opponent = allEntries.firstOrNull { candidate ->
                    val candidateMmr = candidate.player.skillRating ?: return@firstOrNull false

                    // Use the wider of the two ranges so both players' wait times are respected
                    val playerRange = calculateMmrRange(entry.player.joinedQueueAt)
                    val candidateRange = calculateMmrRange(candidate.player.joinedQueueAt)
                    val effectiveRange = maxOf(playerRange, candidateRange)

                    candidate.player.playerId != entry.player.playerId && candidate.matchType == entry.matchType && isTimeControlCompatible(
                        entry.timeControl, candidate.timeControl
                    ) && abs(playerMmr - candidateMmr) <= effectiveRange
                }

                return if (opponent != null) {
                    hashOps.delete(PLAYER_QUEUE_MAP, opponent.player.playerId)
                    val timeControl = entry.timeControl ?: opponent.timeControl ?: TimeControl.entries.random()
                    logger.info("Match found: ${entry.player.playerId} (${playerMmr}) vs ${opponent.player.playerId} (${opponent.player.skillRating}) [$timeControl]")
                    createMatch(listOf(entry.player, opponent.player), entry.matchType, timeControl, null)
                } else {
                    hashOps.put(PLAYER_QUEUE_MAP, entry.player.playerId, serializeQueueEntry(entry))
                    logger.info("Player ${entry.player.playerId} added to competitive queue, no match found yet")
                    null
                }
            }


        }


    }

    /**
     * Removes a player from their current matchmaking queue.
     *
     * @param playerId The unique identifier of the player.
     * @return true if the player was successfully removed, false if not found in any queue.
     */
    fun leaveQueue(playerId: String): Boolean {
        val hashOps = redisTemplate.opsForHash<String, String>()
        val exists = hashOps.hasKey(PLAYER_QUEUE_MAP, playerId)
        if (!exists) return false
        hashOps.delete(PLAYER_QUEUE_MAP, playerId)
        return true
    }

    /**
     * Retrieves the current number of players waiting in queue for each time control.
     *
     * @return the current queue size.
     */
    fun getQueueSizes(): Map<MatchType, Int> {
        val allEntries = redisTemplate.opsForHash<String, String>()
            .entries(PLAYER_QUEUE_MAP).values.map { deserializeQueueEntry(it) }

        return MatchType.entries.associateWith { queueType ->
            allEntries.count { it.matchType == queueType }
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
    private fun createMatch(
        players: List<Player>, matchType: MatchType, timeControl: TimeControl, botConfiguration: BotConfiguration?
    ): Match {
        val match = Match(
            players = players,
            timeControl = timeControl,
            status = MatchStatus.PENDING,
            matchType = matchType,
            botConfiguration = botConfiguration
        )

        // Store match in Redis
        redisTemplate.opsForHash<String, String>().put(ACTIVE_MATCHES, match.matchId, serializeMatch(match))

        return match
    }

    private fun calculateMmrRange(joinedQueueAt: Instant): Int {
        val secondsWaiting = Clock.System.now().minus(joinedQueueAt).inWholeSeconds
        val expansions = secondsWaiting / 5 //how long for a range expansion
        //1 is the min range, 5 is how much it increases by and 50 is the max range we will match
        return minOf(10 + (expansions * 5), 50).toInt()
    }

    private fun isTimeControlCompatible(playerA: TimeControl?, playerB: TimeControl?): Boolean {
        if (playerA == null || playerB == null) return true  // either allows any
        return playerA == playerB
    }

    private fun serializeMatch(match: Match): String {
        return json.encodeToString(match)
    }

    private fun deserializeMatch(jsonString: String): Match {
        return json.decodeFromString<Match>(jsonString)
    }

    private fun serializeQueueEntry(entry: QueueEntry): String {
        return json.encodeToString(entry)
    }

    private fun deserializeQueueEntry(jsonString: String): QueueEntry {
        return json.decodeFromString<QueueEntry>(jsonString)
    }

    /**
     * Requests a game server allocation for a match from the Game Manager.
     *
     * @param matchId The ID of the match to allocate.
     * @param whitePlayer The player assigned to play white.
     * @param blackPlayer The player assigned to play black.
     * @param timeControl The match's time control.
     * @param matchType The match's type i.e. PvP/PvE, Competitive/Casual
     * @param botModelType The selected bot model affecting its move making algorithm
     * @return A [ServerAllocationResult] indicating success or failure and server details.
     */
    fun requestServerForMatch(
        matchId: String,
        whitePlayer: Player,
        blackPlayer: Player,
        timeControl: TimeControl,
        matchType: MatchType,
        botConfiguration: BotConfiguration?
    ): ServerAllocationResult {

        try {
            logger.info("Requesting server for match $matchId")
            logger.info("  White: ${whitePlayer.displayName}, MMR: ${whitePlayer.skillRating} ")
            logger.info("  Black: ${blackPlayer.displayName}, MMR: ${blackPlayer.skillRating} ")
            logger.info("  Time: ${timeControl.name} (${timeControl.minutes}+${timeControl.increment})")
            logger.info("  Game mode: $matchType ")

            if (botConfiguration != null) {
                logger.info("  BotConfiguration: $botConfiguration")
            }

            val request =
                GameServerProto.ServerRequest.newBuilder().setMatchId(matchId).setWhitePlayerId(whitePlayer.playerId)
                    .setWhitePlayerName(whitePlayer.displayName).setBlackPlayerId(blackPlayer.playerId)
                    .setBlackPlayerName(blackPlayer.displayName).setInitialTimeMinutes(timeControl.minutes)
                    .setIncrementSeconds(timeControl.increment).setMatchType(matchType.toString())
                    .apply {
                        botConfiguration?.let {
                            setBotConfiguration(
                                GameServerProto.BotConfiguration.newBuilder()
                                    .setBotModelType(GameServerProto.BotModelType.valueOf(it.botModelType.name))
                                    .setBotMoveTimeModelType(GameServerProto.BotMoveTimeModelType.valueOf(it.botMoveTimeModelType.name))
                                    .build()
                            )
                        }
                    }.build()

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

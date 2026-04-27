package org.checkmateplus.gameservermanager.services


import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.gameserver.proto.GameServerGrpc
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.MatchManagerGrpc
import org.checkmateplus.gameservermanager.models.GameServerInstance
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.grpc.server.service.GrpcService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Core service for managing the lifecycle and health of game server instances.
 *
 * This service handles server registration, heartbeat monitoring, and the allocation
 * of matches to available servers based on their current load.
 */
@Service
@GrpcService
class GameManagerService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val matchTrackingService: MatchTrackingService,
) : MatchManagerGrpc.MatchManagerImplBase() {
    private var registeredInstances: ConcurrentHashMap<String, GameServerInstance> = ConcurrentHashMap()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val serverChannels = ConcurrentHashMap<String, ManagedChannel>()
    private val serverStubs = ConcurrentHashMap<String, GameServerGrpc.GameServerBlockingStub>()

    companion object {
        private const val ACTIVE_MATCHES_HASH = "matchmaking:active_matches"
    }


    /**
     * Registers a new game server instance into the matchmaking pool.
     *
     * @param request Registration details including IP, port, and capacity.
     * @param responseObserver Observer for sending the registration response.
     */
    override fun registerServer(
        request: GameServerProto.RegisterServerRequest,
        responseObserver: StreamObserver<GameServerProto.RegisterServerResponse>
    ) {
        try {
            logger.info("Registering server: ${request.serverId} at ${request.ipAddress}:${request.port}")

            registeredInstances[request.serverId] = GameServerInstance(
                serverId = request.serverId,
                activeMatches = 0,
                activePlayers = 0,
                ipAddress = request.ipAddress,
                port = request.port,
                maxMatches = request.maxConcurrentMatches,
                lastheartbeatTime = System.currentTimeMillis(),
                websocketUrl = request.websocketUrl
            )

            val channel = ManagedChannelBuilder.forAddress(request.ipAddress,request.port)
                .usePlaintext().build()

            serverChannels[request.serverId] = channel
            serverStubs[request.serverId] = GameServerGrpc.newBlockingStub(channel)

            val response = GameServerProto.RegisterServerResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Server registered successfully")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

            logger.info("Server ${request.serverId} registered. Capacity: ${request.maxConcurrentMatches}")

        } catch (e: Exception) {
            logger.error("Failed to register server", e)
            val response = GameServerProto.RegisterServerResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Registration failed: ${e.message}")
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    /**
     * Processes heartbeats from registered game servers to monitor health and update load statistics.
     *
     * @param request Heartbeat details containing active matches and players.
     * @param responseObserver Observer for acknowledging the heartbeat.
     */
    override fun heartbeat(
        request: GameServerProto.HeartbeatRequest,
        responseObserver: StreamObserver<GameServerProto.HeartbeatResponse>
    ) {
        try {
            val server = registeredInstances[request.serverId]

            if (server != null) {
                server.activeMatches = request.activeMatchCount
                server.activePlayers = request.connectedPlayerCount
                server.lastheartbeatTime = System.currentTimeMillis()

                logger.debug("Heartbeat from ${request.serverId}: ${request.activeMatchCount}/${server.maxMatches} matches")

                val response = GameServerProto.HeartbeatResponse.newBuilder()
                    .setAcknowledged(true)
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } else {
                logger.warn("Heartbeat from unregistered server: ${request.serverId}")
                responseObserver.onNext(GameServerProto.HeartbeatResponse.newBuilder().setAcknowledged(false).build())
                responseObserver.onCompleted()
            }

        } catch (e: Exception) {
            logger.error("Heartbeat failed", e)
            responseObserver.onError(e)
        }
    }

    /**
     * Requests a game server for a new match.
     * Selects an available server from the pool and communicates with it to allocate the match.
     *
     * @param request Server request containing player and match configuration details.
     * @param responseObserver Observer for sending the allocated server details or error.
     */
    fun requestServer(
        request: GameServerProto.ServerRequest,
        responseObserver: StreamObserver<GameServerProto.ServerResponse>
    )  {
        try {
            logger.info("Server request for match ${request.matchId} ")
            logger.info("  White: ${request.whitePlayerName} ")
            logger.info("  Black: ${request.blackPlayerName} ")

            val availableInstances = registeredInstances.entries
                .filter { it.value.activeMatches < it.value.maxMatches }
                .sortedByDescending { it.value.activeMatches }




            if (availableInstances.isEmpty()) {
                logger.error("No available server for match ${request.matchId}")
                val response = GameServerProto.ServerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("No available servers. All servers at capacity.")
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
                return
            }
            val server = availableInstances[0]
            logger.info("Selected server ${server.value.serverId} (${server.value.activeMatches}/${server.value.maxMatches})")

            // Allocate match on selected server
            val allocateRequest = GameServerProto.AllocateMatchRequest.newBuilder()
                .setMatchId(request.matchId)
                .setWhitePlayerId(request.whitePlayerId)
                .setWhitePlayerName(request.whitePlayerName)
                .setBlackPlayerId(request.blackPlayerId)
                .setBlackPlayerName(request.blackPlayerName)
                .setInitialTimeSeconds(request.initialTimeMinutes * 60)
                .setIncrementSeconds(request.incrementSeconds)
                .setBotConfiguration(request.botConfiguration)
                .setMatchType(request.matchType)
                .setWhiteSkillRating(request.whiteSkillRating)
                .setBlackSkillRating(request.blackSkillRating)
                .build()

            val stub = serverStubs[server.value.serverId]
            if (stub == null) {
                logger.error("No gRPC stub for server ${server.value.serverId}")
                responseObserver.onNext(
                    GameServerProto.ServerResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Server communication error")
                        .build()
                )
                responseObserver.onCompleted()
                return
            }

            val allocateResponse = stub.allocateMatch(allocateRequest)

            if (allocateResponse.success) {
                server.value.activeMatches++

                logger.info("Match ${request.matchId} allocated to ${server.value.serverId}")
                logger.info("  Server now at ${server.value.activeMatches}/${server.value.maxMatches} )")

                val response = GameServerProto.ServerResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Server allocated successfully")
                    .setServer(
                        GameServerProto.AllocatedServer.newBuilder()
                            .setServerId(server.value.serverId)
                            .setMatchId(request.matchId)
                            .setWebsocketUrl(server.value.websocketUrl) //NOTE: when running for real the domain should be added
                            .setEstimatedWaitSeconds(0)
                            .build()
                    )
                    .build()

                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } else {
                logger.error("Server ${server.value.serverId} rejected allocation: ${allocateResponse.message}")
                responseObserver.onNext(
                    GameServerProto.ServerResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Server rejected match: ${allocateResponse.message}")
                        .build()
                )
                responseObserver.onCompleted()
            }

        } catch (e: Exception) {
            logger.error("Failed to allocate server", e)
            responseObserver.onNext(
                GameServerProto.ServerResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Allocation failed: ${e.message}")
                    .build()
            )
            responseObserver.onCompleted()
        }
    }

    /**
     * Handles a match completion report from a game server.
     *
     * Frees the server's capacity slot and removes the match from the shared
     * `matchmaking:active_matches` Redis hash owned by MatchmakingService.
     *
     * @param request The match completion report.
     * @param responseObserver Observer for acknowledging the report.
     */
    override fun reportMatchCompleted(
        request: GameServerProto.MatchCompletedReport,
        responseObserver: StreamObserver<GameServerProto.MatchCompletedResponse>
    ) {
        try {
            logger.info(
                "Match ${request.matchId} completed on ${request.serverId} " +
                        "(reason=${request.reason}, winner=${request.winnerId}, " +
                        "moves=${request.totalMoves}, duration=${request.durationSeconds}s)"
            )

            registeredInstances[request.serverId]?.let { server ->
                if (server.activeMatches > 0) server.activeMatches--
                logger.info(
                    "Server ${request.serverId} now at ${server.activeMatches}/${server.maxMatches}"
                )
            } ?: logger.warn("Completion report from unregistered server ${request.serverId}")

            val removed = redisTemplate.opsForHash<String, String>()
                .delete(ACTIVE_MATCHES_HASH, request.matchId)
            if (removed == 0L) {
                logger.warn("Match ${request.matchId} not present in $ACTIVE_MATCHES_HASH")
            }

            responseObserver.onNext(
                GameServerProto.MatchCompletedResponse.newBuilder().setAcknowledged(true).build()
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Failed to process match completion for ${request.matchId}", e)
            responseObserver.onNext(
                GameServerProto.MatchCompletedResponse.newBuilder().setAcknowledged(false).build()
            )
            responseObserver.onCompleted()
        }
    }

    /**
     * Records a per-match status / FEN / clock snapshot pushed from MatchmakingService
     * (PENDING/CANCELLED) or GameServer (ASSIGNED/ACTIVE/COMPLETED/ABANDONED).
     */
    override fun reportMatchUpdate(
        request: GameServerProto.MatchUpdate,
        responseObserver: StreamObserver<GameServerProto.MatchUpdateResponse>
    ) {
        try {
            matchTrackingService.recordUpdate(request)
            responseObserver.onNext(
                GameServerProto.MatchUpdateResponse.newBuilder().setAcknowledged(true).build()
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Failed to record match update for ${request.matchId}", e)
            responseObserver.onNext(
                GameServerProto.MatchUpdateResponse.newBuilder().setAcknowledged(false).build()
            )
            responseObserver.onCompleted()
        }
    }

    /**
     * Returns a snapshot of every match currently tracked in the GSM.
     * Includes PENDING/ASSIGNED/ACTIVE plus recently-ended entries within their TTL window.
     */
    override fun listLiveMatches(
        request: GameServerProto.ListLiveMatchesRequest,
        responseObserver: StreamObserver<GameServerProto.ListLiveMatchesResponse>
    ) {
        try {
            val tracked = matchTrackingService.listLiveMatches()
            val response = GameServerProto.ListLiveMatchesResponse.newBuilder()
                .apply { tracked.forEach { addMatches(toLiveMatch(it)) } }
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Failed to list live matches", e)
            responseObserver.onError(e)
        }
    }

    private fun toLiveMatch(match: org.checkmateplus.gameservermanager.models.TrackedMatch): GameServerProto.LiveMatch {
        val builder = GameServerProto.LiveMatch.newBuilder()
            .setMatchId(match.matchId)
            .setServerId(match.serverId ?: "")
            .setStatus(GameServerProto.MatchStatus.valueOf("MATCH_STATUS_${match.status}"))
            .setLastFen(match.lastFen ?: "")
            .setWhiteTimeSeconds(match.whiteTimeSeconds ?: 0)
            .setBlackTimeSeconds(match.blackTimeSeconds ?: 0)
            .setActiveColor(match.activeColor ?: "")
            .setMoveNumber(match.moveNumber ?: 0)
            .setWinnerId(match.winnerId ?: "")
            .setReason(match.reason ?: "")
            .setUpdatedAt(match.updatedAt)
        match.whitePlayer?.let {
            builder.whitePlayerId = it.playerId
            builder.whitePlayerName = it.displayName
            builder.whitePlayerRating = it.skillRating ?: 0
            builder.whitePlayerRegistered = it is Player.Registered
        }
        match.blackPlayer?.let {
            builder.blackPlayerId = it.playerId
            builder.blackPlayerName = it.displayName
            builder.blackPlayerRating = it.skillRating ?: 0
            builder.blackPlayerRegistered = it is Player.Registered
        }
        val bot = match.whitePlayer as? Player.Bot
            ?: match.blackPlayer as? Player.Bot
        bot?.let {
            builder.botConfiguration = GameServerProto.BotConfiguration.newBuilder()
                .setBotModelType(GameServerProto.BotModelType.valueOf(it.botConfiguration.botModelType.name))
                .setBotMoveTimeModelType(GameServerProto.BotMoveTimeModelType.valueOf(it.botConfiguration.botMoveTimeModelType.name))
                .build()
        }
        return builder.build()
    }

    /**
     * Removes a game server from the matchmaking pool.
     *
     * @param request The un-registration request.
     * @param responseObserver Observer for acknowledging the un-registration.
     */
    override fun unregisterServer(
        request: GameServerProto.UnRegisterServerRequest?,
        responseObserver: StreamObserver<GameServerProto.UnRegisterServerResponse?>?
    ) {
        try {
            request?.let {
                registeredInstances.remove(request.serverId) //remove the server from the pool
            }
            val response = GameServerProto.UnRegisterServerResponse.newBuilder().setSuccess(true).build()
            responseObserver?.onNext(response)
            responseObserver?.onCompleted()
            logger.info("Server ${request?.serverId} un-registered from the matchmaking pool.")
        } catch (e: Exception) {
            logger.error("Un-registering server error", e)
            responseObserver?.onError(e)
        }
    }
}

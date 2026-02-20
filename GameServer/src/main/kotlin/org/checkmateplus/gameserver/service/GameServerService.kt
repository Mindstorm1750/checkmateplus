package org.checkmateplus.gameserver.service

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.checkmateplus.chessLibrary.chessEngine.Colour
import org.checkmateplus.chessLibrary.playerModels.BotModelType
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.gameserver.proto.GameServerGrpc
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.MatchManagerGrpc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.grpc.server.service.GrpcService
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.time.ExperimentalTime

/**
 * gRPC service for managing the game server's lifecycle and match allocations.
 * This service communicates with a Match Manager to register itself and allocate/terminate matches.
 */
@GrpcService
class GameServerService(
    private val matchService: MatchService,
    @Value("\${gameservermanager.host}") private val managerHost: String,
    @Value("\${gameservermanager.port}") private val managerPort: Int,
    @Value("\${gameserver.advertise.host}") private val advertiseHost: String,
    @Value("\${gameserver.advertise.grpc-port}") private val advertiseGrpcPort: Int,
    @Value("\${gameserver.max-concurrent-matches:5}") private val maxConcurrentMatches: Int
) : GameServerGrpc.GameServerImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var channel: ManagedChannel
    private lateinit var stub: MatchManagerGrpc.MatchManagerBlockingStub
    private val serverId = System.getenv("HOSTNAME") ?: "localhost"

    @PostConstruct
    fun initialize() {
        channel = ManagedChannelBuilder.forAddress(managerHost, managerPort) //this needs to be changed
            .usePlaintext().build()

        stub = MatchManagerGrpc.newBlockingStub(channel)

        registerServer()
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun registerServer() {
        try {
            val request =
                GameServerProto.RegisterServerRequest.newBuilder().setServerId(serverId).setIpAddress(advertiseHost)
                    .setPort(advertiseGrpcPort).setMaxConcurrentMatches(maxConcurrentMatches).build()

            val response = stub.registerServer(request)

            if (response.success) {
                logger.info("Registered with manager: ${response.message}")
            } else {
                logger.error("Registration failed: ${response.message}")
            }
        } catch (e: Exception) {
            logger.error("Failed to register", e)
        }
    }

    /**
     * Allocates a new match on this server based on a request from the Match Manager.
     *
     * @param request The allocation request containing match and player details.
     * @param responseObserver Observer for sending the allocation response.
     */
    @OptIn(ExperimentalTime::class)
    override fun allocateMatch(
        request: GameServerProto.AllocateMatchRequest,
        responseObserver: StreamObserver<GameServerProto.AllocateMatchResponse>
    ) {

        try {

            logger.info("Allocating match ${request.matchId}")
            val whitePlayer: Player
            val blackPlayer: Player
            if (!(request.botModelType == null || request.botModelType == "null")) {
                val bot = Player.Bot(botModelType = BotModelType.valueOf(request.botModelType))
                val human = Player.Anonymous(
                    displayName = request.whitePlayerName, playerId = request.whitePlayerId
                )
                val botSide = if (Random.nextBoolean()) Colour.WHITE else Colour.BLACK
                whitePlayer = if (botSide == Colour.WHITE) bot else human
                blackPlayer = if (botSide == Colour.WHITE) human else bot
            } else {
                whitePlayer = if (request.whitePlayerRegistered) {
                    Player.Anonymous(
                        displayName = request.whitePlayerName, playerId = request.whitePlayerId
                    )
                } else {
                    Player.Anonymous(  //this needs to be "registered" when accounts are implemented!!!!
                        displayName = request.whitePlayerName, playerId = request.whitePlayerId
                    )
                }

                blackPlayer = if (request.blackPlayerRegistered) {
                    Player.Anonymous(
                        displayName = request.blackPlayerName, playerId = request.blackPlayerId
                    )
                } else {
                    Player.Anonymous(
                        displayName = request.blackPlayerName, //this needs to be "registered" when accounts are implemented!!!!
                        playerId = request.blackPlayerId
                    )
                }
            }


            // Create time control
            val timeControl: TimeControl = TimeControl.fromMinutesAndIncrement(
                minutes = request.initialTimeSeconds / 60, increment = request.incrementSeconds
            )
                ?: throw IllegalArgumentException("Invalid time control: ${request.initialTimeSeconds / 60}+${request.incrementSeconds}")

            // Create match
            matchService.createMatch(
                matchId = request.matchId,
                players = listOf(whitePlayer, blackPlayer),
                timeControl = timeControl,
                matchType = MatchType.valueOf(request.matchType),
                botModelType = if(request.botModelType == "null") null else BotModelType.valueOf(request.botModelType)
            )
            logger.info("Created match: ${request.matchId} with players $whitePlayer & $blackPlayer of type ${request.matchType}")
            val response =
                GameServerProto.AllocateMatchResponse.newBuilder().setSuccess(true).setMessage("Match created").build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

        } catch (e: Exception) {
            logger.error("Failed to allocate match", e)
            val response = GameServerProto.AllocateMatchResponse.newBuilder().setSuccess(false)
                .setMessage(e.message ?: "Unknown error").build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    /**
     * Terminates an existing match.
     *
     * @param request The termination request.
     * @param responseObserver Observer for sending the termination response.
     */
    override fun terminateMatch(
        request: GameServerProto.TerminateMatchRequest,
        responseObserver: StreamObserver<GameServerProto.TerminateMatchResponse>
    ) {
        try {
            logger.info("Terminating match ${request.matchId}")

            matchService.removeMatch(request.matchId)

            val response = GameServerProto.TerminateMatchResponse.newBuilder().setSuccess(true).build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

        } catch (e: Exception) {
            logger.error("Failed to terminate match", e)
            responseObserver.onError(e)
        }
    }

    /**
     * Periodically sends a heartbeat to the Match Manager with server statistics.
     */
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    fun sendHeartbeat() {
        try {
            val activeMatches = matchService.getActiveMatchCount()
            val activePlayers = matchService.getActivePlayerCount()

            val request =
                GameServerProto.HeartbeatRequest.newBuilder().setServerId(serverId).setActiveMatchCount(activeMatches)
                    .setConnectedPlayerCount(activePlayers).build()

            val response = stub.heartbeat(request)

            if (response.acknowledged) {
                logger.debug("Heartbeat acknowledged")
            }
        } catch (e: Exception) {
            logger.error("Heartbeat failed", e)
        }
    }
}
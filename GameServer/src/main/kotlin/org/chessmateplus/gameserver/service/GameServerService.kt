package org.chessmateplus.gameserver.service

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.chessmateplus.chessLibrary.playerModels.Player
import org.chessmateplus.chessLibrary.playerModels.TimeControl
import org.chessmateplus.gameserver.proto.GameServerGrpc
import org.chessmateplus.gameserver.proto.GameServerProto
import org.chessmateplus.gameserver.proto.MatchManagerGrpc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.grpc.server.service.GrpcService
import org.springframework.scheduling.annotation.Scheduled
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime

@GrpcService
class GameServerService(
    private val matchService: MatchService,
) : GameServerGrpc.GameServerImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var channel: ManagedChannel
    private lateinit var stub: MatchManagerGrpc.MatchManagerBlockingStub
    private val serverId = System.getenv("HOSTNAME") ?: "localhost"
    @PostConstruct
    fun initialize() {
        channel = ManagedChannelBuilder
            .forAddress("localhost", 9090) //this needs to be changed
            .usePlaintext()
            .build()

        stub = MatchManagerGrpc.newBlockingStub(channel)

        registerServer()
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun registerServer() {
        val podName = System.getenv("HOSTNAME") ?: "localhost"
        try {
            val request = GameServerProto.RegisterServerRequest.newBuilder()
                .setServerId(serverId)
                .setIpAddress("127.0.0.1")
                .setPort(9091)
                .setMaxConcurrentMatches(5)
                .build()

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
    @OptIn(ExperimentalTime::class)
    override fun allocateMatch(
        request: GameServerProto.AllocateMatchRequest,
        responseObserver: StreamObserver<GameServerProto.AllocateMatchResponse>
    ) {
        try {
            logger.info("Allocating match ${request.matchId}")



            val whitePlayer: Player = if (request.whitePlayerRegistered) {
                Player.Anonymous(
                    displayName = request.whitePlayerName,
                    playerId = request.whitePlayerId
                )
            } else {
                Player.Anonymous(  //this needs to be "registered" when accounts are implemented!!!!
                    displayName = request.whitePlayerName,
                    playerId = request.whitePlayerId
                )
            }

            val blackPlayer: Player = if (request.blackPlayerRegistered) {
                Player.Anonymous(
                    displayName = request.blackPlayerName,
                    playerId = request.blackPlayerId
                )
            } else {
                Player.Anonymous(
                    displayName = request.blackPlayerName, //this needs to be "registered" when accounts are implemented!!!!
                    playerId = request.blackPlayerId
                )
            }




            // Create time control
            val timeControl: TimeControl = TimeControl.fromMinutesAndIncrement(
                minutes = request.initialTimeSeconds / 60,
                increment = request.incrementSeconds
            )!!

            // Create match
            matchService.createMatch(
                matchId = request.matchId,
                players = listOf(whitePlayer, blackPlayer),
                timeControl = timeControl
            )

            val response = GameServerProto.AllocateMatchResponse.newBuilder()
                .setSuccess(true)
                .setMessage("Match created")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

        } catch (e: Exception) {
            logger.error("Failed to allocate match", e)
            val response = GameServerProto.AllocateMatchResponse.newBuilder()
                .setSuccess(false)
                .setMessage(e.message ?: "Unknown error")
                .build()
            responseObserver.onNext(response)
            responseObserver.onCompleted()
        }
    }

    override fun terminateMatch(
        request: GameServerProto.TerminateMatchRequest,
        responseObserver: StreamObserver<GameServerProto.TerminateMatchResponse>
    ) {
        try {
            logger.info("Terminating match ${request.matchId}")

            matchService.removeMatch(request.matchId)

            val response = GameServerProto.TerminateMatchResponse.newBuilder()
                .setSuccess(true)
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

        } catch (e: Exception) {
            logger.error("Failed to terminate match", e)
            responseObserver.onError(e)
        }
    }
    @Scheduled(fixedDelay = 30000) // Every 30 seconds
    fun sendHeartbeat() {
        try {
            val activeMatches = matchService.getActiveMatchCount()
            val activePlayers = matchService.getActivePlayerCount()

            val request = GameServerProto.HeartbeatRequest.newBuilder()
                .setServerId(serverId)
                .setActiveMatchCount(activeMatches)
                .setConnectedPlayerCount(activePlayers)
                .build()

            val response = stub.heartbeat(request)

            if (response.acknowledged) {
                logger.debug("Heartbeat acknowledged")
            }
        } catch (e: Exception) {
            logger.error("Heartbeat failed", e)
        }
    }
}
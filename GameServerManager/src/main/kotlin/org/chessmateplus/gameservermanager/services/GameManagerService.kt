package org.chessmateplus.gameservermanager.services


import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.chessmateplus.gameserver.proto.GameServerGrpc
import org.chessmateplus.gameserver.proto.GameServerProto
import org.chessmateplus.gameserver.proto.MatchManagerGrpc
import org.chessmateplus.gameservermanager.models.GameServerInstance
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
@GrpcService
class GameManagerService : MatchManagerGrpc.MatchManagerImplBase() {
    private var registeredInstances: ConcurrentHashMap<String, GameServerInstance> = ConcurrentHashMap()
    private val logger = LoggerFactory.getLogger(javaClass)
    private val serverChannels = ConcurrentHashMap<String, ManagedChannel>()
    private val serverStubs = ConcurrentHashMap<String, GameServerGrpc.GameServerBlockingStub>()


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
                lastheartbeatTime = System.currentTimeMillis()
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

                logger.info("Match ${request.matchId} allocated to ${server.value.serverId}")
                logger.info("  Server now at ${server.value.activeMatches}/${server.value.maxMatches} )")

                val response = GameServerProto.ServerResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Server allocated successfully")
                    .setServer(
                        GameServerProto.AllocatedServer.newBuilder()
                            .setServerId(server.value.serverId)
                            .setMatchId(request.matchId)
                            .setWebsocketUrl(server.value.ipAddress) //NOTE: when running for real the domain should be added
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

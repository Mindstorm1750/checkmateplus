package org.checkmateplus.gameservermanager.services

import io.grpc.stub.StreamObserver
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.ServerAllocatorGrpc
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import org.springframework.stereotype.Service

/**
 * gRPC service for requesting a game server for a new match.
 * This service acts as an entry point for the matchmaking service to allocate server resources.
 */
@Service
@GrpcService
class ServerAllocatorService(
    private val gameManagerService: GameManagerService
) : ServerAllocatorGrpc.ServerAllocatorImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Handles a request to allocate a server for a new match.
     * Delegates the request to [GameManagerService].
     *
     * @param request The server allocation request containing match details.
     * @param responseObserver Observer for sending the allocation response.
     */
    override fun requestServer(
        request: GameServerProto.ServerRequest,
        responseObserver: StreamObserver<GameServerProto.ServerResponse>
    ) {
        gameManagerService.requestServer(request, responseObserver)
    }
}
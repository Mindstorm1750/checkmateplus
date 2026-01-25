package org.chessmateplus.gameservermanager.services

import io.grpc.stub.StreamObserver
import org.chessmateplus.gameserver.proto.GameServerProto
import org.chessmateplus.gameserver.proto.ServerAllocatorGrpc
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService
import org.springframework.stereotype.Service

@Service
@GrpcService
class ServerAllocatorService(
    private val gameManagerService: GameManagerService
) : ServerAllocatorGrpc.ServerAllocatorImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun requestServer(
        request: GameServerProto.ServerRequest,
        responseObserver: StreamObserver<GameServerProto.ServerResponse>
    ) {
        // Delegate to the existing implementation in GameManagerService
        gameManagerService.requestServer(request, responseObserver)
    }
}
package org.checkmateplus.playerservice.services

import io.grpc.stub.StreamObserver
import org.checkmateplus.playerservice.proto.PlayerServiceGrpc
import org.checkmateplus.playerservice.proto.PlayerServiceProto
import org.slf4j.LoggerFactory
import org.springframework.grpc.server.service.GrpcService

@GrpcService
class PlayerServiceGrpcImpl(
    private val matchRecordService: MatchRecordService
) : PlayerServiceGrpc.PlayerServiceImplBase() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun submitMatchRecord(
        request: PlayerServiceProto.MatchRecord,
        responseObserver: StreamObserver<PlayerServiceProto.MatchRecordResponse>
    ) {
        return try {
            matchRecordService.save(request)
            responseObserver.onNext(
                PlayerServiceProto.MatchRecordResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Match record stored successfully")
                    .build()
            )
            responseObserver.onCompleted()
        } catch (e: Exception) {
            logger.error("Failed to store match record: ${e.message}", e)
            responseObserver.onNext(
                PlayerServiceProto.MatchRecordResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to store match record: ${e.message}")
                    .build()
            )
            responseObserver.onCompleted()
        }
    }
}
package org.checkmateplus.gameserver.service

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.checkmateplus.chessLibrary.playerModels.history.MatchRecord
import org.checkmateplus.gameserver.models.toProto
import org.checkmateplus.playerservice.proto.PlayerServiceGrpc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AccountsService(
    private val eventPublisher: ApplicationEventPublisher,
    @Value("\${playerservice.host}") private val playerserviceHost: String,
    @Value("\${playerservice.port}") private val playerservicePort: Int,
): PlayerServiceGrpc.PlayerServiceImplBase() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val serverId = System.getenv("HOSTNAME") ?: "localhost"
    private lateinit var stub: PlayerServiceGrpc.PlayerServiceBlockingStub
    private lateinit var channel: ManagedChannel
    fun sendMatchRecord(matchRecord: MatchRecord): Boolean {
        return try {
            val response = stub.submitMatchRecord(matchRecord.toProto())
            if (response.success) {
                logger.info("[$serverId] MatchRecord ${matchRecord.matchId} stored successfully")
                true
            } else {
                logger.error("[$serverId] MatchRecord ${matchRecord.matchId} rejected: ${response.message}")
                false
            }
        } catch (e: Exception) {
            logger.error("[$serverId] sendMatchRecord threw an exception: ${e.message}")
            false
        }
    }
    @PostConstruct
    fun initialize() {
        channel = ManagedChannelBuilder.forAddress(playerserviceHost, playerservicePort)
            .usePlaintext().build()

        stub = PlayerServiceGrpc.newBlockingStub(channel)
    }
    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}



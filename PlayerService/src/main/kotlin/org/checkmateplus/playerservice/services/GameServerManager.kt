package org.checkmateplus.playerservice.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.MatchManagerGrpc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * gRPC client to GameServerManager's MatchManager service. Used to read the
 * live-match snapshot stored in the GSM.
 */
@Service
class GameServerManager(
    @Value("\${gameservermanager.host}") private val managerHost: String,
    @Value("\${gameservermanager.port}") private val managerPort: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var channel: ManagedChannel
    private lateinit var stub: MatchManagerGrpc.MatchManagerBlockingStub

    @PostConstruct
    fun initialise() {
        channel = ManagedChannelBuilder.forAddress(managerHost, managerPort)
            .usePlaintext().build()
        stub = MatchManagerGrpc.newBlockingStub(channel)
        logger.info("LiveMatchClient connected to GameServerManager at $managerHost:$managerPort")
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun listLiveMatches(): List<GameServerProto.LiveMatch> {
        val response = stub.listLiveMatches(GameServerProto.ListLiveMatchesRequest.getDefaultInstance())
        return response.matchesList
    }
}

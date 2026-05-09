package org.checkmateplus.matchmakingservice.services

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.MatchManagerGrpc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Pushes PENDING and CANCELLED match-status updates from MatchmakingService to
 * the GameServerManager observability hub via the ReportMatchUpdate gRPC.
 *
 * GameServer owns the rest of the lifecycle (ASSIGNED/ACTIVE/COMPLETED/ABANDONED)
 * and reports those directly through its own client.
 */
@Service
class MatchTrackingService(
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
        logger.info("MatchTrackingService connected to GameServerManager at $managerHost:$managerPort")
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun reportPending(matchId: String, white: Player, black: Player, botConfiguration: BotConfiguration?) {
        send(buildBaseUpdate(matchId, GameServerProto.MatchStatus.MATCH_STATUS_PENDING, white, black, botConfiguration).build())
    }

    fun reportCancelled(matchId: String, white: Player, black: Player, reason: String, botConfiguration: BotConfiguration?) {
        send(
            buildBaseUpdate(matchId, GameServerProto.MatchStatus.MATCH_STATUS_CANCELLED, white, black, botConfiguration)
                .setReason(reason)
                .build()
        )
    }

    private fun buildBaseUpdate(
        matchId: String,
        status: GameServerProto.MatchStatus,
        white: Player,
        black: Player,
        botConfiguration: BotConfiguration?,
    ): GameServerProto.MatchUpdate.Builder {
        return GameServerProto.MatchUpdate.newBuilder()
            .setMatchId(matchId)
            .setStatus(status)
            .setWhitePlayerId(white.playerId)
            .setWhitePlayerName(white.displayName)
            .setWhitePlayerRating(white.skillRating ?: 0)
            .setBlackPlayerId(black.playerId)
            .setBlackPlayerName(black.displayName)
            .setBlackPlayerRating(black.skillRating ?: 0)
            .setWhitePlayerRegistered(white.javaClass == Player.Registered::class.java)
            .setBlackPlayerRegistered(black.javaClass == Player.Registered::class.java)
            .apply {
                botConfiguration?.let {
                    setBotConfiguration(
                        GameServerProto.BotConfiguration.newBuilder()
                            .setBotModelType(GameServerProto.BotModelType.valueOf(it.botModelType.name))
                            .setBotMoveTimeModelType(GameServerProto.BotMoveTimeModelType.valueOf(it.botMoveTimeModelType.name))
                            .build()
                    )
                }
            }
    }

    private fun send(update: GameServerProto.MatchUpdate) {
        try {
            val response = stub.reportMatchUpdate(update)
            if (!response.acknowledged) {
                logger.warn("GSM did not acknowledge match update for ${update.matchId} (${update.status})")
            }
        } catch (e: Exception) {
            logger.error("ReportMatchUpdate failed for ${update.matchId} (${update.status}): ${e.message}")
        }
    }
}

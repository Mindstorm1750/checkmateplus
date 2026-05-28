package org.checkmateplus.gameserver.service

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.gameserver.models.MatchState
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameserver.proto.MatchManagerGrpc
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Pushes per-match snapshots (status, FEN, clocks, players) to GameServerManager
 * via the ReportMatchUpdate gRPC. Owns its own channel so reporting failures do
 * not affect the registration/heartbeat path in [GameServerService].
 */
@Service
class MatchReporterService(
    @Value("\${gameservermanager.host}") private val managerHost: String,
    @Value("\${gameservermanager.port}") private val managerPort: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val serverId = System.getenv("HOSTNAME") ?: "localhost"
    private lateinit var channel: ManagedChannel
    private lateinit var stub: MatchManagerGrpc.MatchManagerBlockingStub

    @PostConstruct
    fun initialize() {
        channel = ManagedChannelBuilder.forAddress(managerHost, managerPort)
            .usePlaintext().build()
        stub = MatchManagerGrpc.newBlockingStub(channel)
        logger.info("[$serverId] MatchReporter gRPC channel to $managerHost:$managerPort initialised")
    }

    @PreDestroy
    fun shutdown() {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    fun reportAssigned(match: MatchState) {
        send(buildSnapshot(match, GameServerProto.MatchStatus.MATCH_STATUS_ASSIGNED))
    }

    fun reportActive(match: MatchState) {
        send(buildSnapshot(match, GameServerProto.MatchStatus.MATCH_STATUS_ACTIVE))
    }

    fun reportMove(match: MatchState) {
        send(buildSnapshot(match, GameServerProto.MatchStatus.MATCH_STATUS_ACTIVE))
    }

    fun reportCompleted(match: MatchState, winnerId: String, reason: String) {
        send(buildSnapshot(match, GameServerProto.MatchStatus.MATCH_STATUS_COMPLETED, winnerId, reason))
    }

    fun reportAbandoned(match: MatchState, reason: String = "abandoned") {
        send(buildSnapshot(match, GameServerProto.MatchStatus.MATCH_STATUS_ABANDONED, reason = reason))
    }

    private fun buildSnapshot(
        match: MatchState,
        status: GameServerProto.MatchStatus,
        winnerId: String = "",
        reason: String = "",
    ): GameServerProto.MatchUpdate {
        val builder = GameServerProto.MatchUpdate.newBuilder()
            .setMatchId(match.matchId)
            .setServerId(serverId)
            .setStatus(status)
            .setWhiteTimeSeconds(match.whitePlayerTime)
            .setBlackTimeSeconds(match.blackPlayerTime)
            .setActiveColor(match.activeTurn.name)
            .setMoveNumber(match.turnHistory.size)
            .setWinnerId(winnerId)
            .setReason(reason)
            .setMatchType(GameServerProto.MatchType.valueOf("MATCH_TYPE_${match.matchType.name}"))
            .setTimeControl(GameServerProto.TimeControl.valueOf("TIME_CONTROL_${match.timeControl.name}"))

        match.whitePlayer?.let {
            builder.whitePlayerId = it.playerId
            builder.whitePlayerName = it.displayName
            builder.whitePlayerRating = it.skillRating ?: 0
            builder.whitePlayerRegistered = it.javaClass == Player.Registered::class.java
        }
        match.blackPlayer?.let {
            builder.blackPlayerId = it.playerId
            builder.blackPlayerName = it.displayName
            builder.blackPlayerRating = it.skillRating ?: 0
            builder.blackPlayerRegistered = it.javaClass == Player.Registered::class.java
        }
        match.botConfiguration?.let {
            builder.botConfiguration = GameServerProto.BotConfiguration.newBuilder()
                .setBotModelType(GameServerProto.BotModelType.valueOf(it.botModelType.name))
                .setBotMoveTimeModelType(GameServerProto.BotMoveTimeModelType.valueOf(it.botMoveTimeModelType.name))
                .build()
        }
        match.board?.toFen()?.let { builder.lastFen = it }
        return builder.build()
    }

    private fun send(update: GameServerProto.MatchUpdate) {
        try {
            val response = stub.reportMatchUpdate(update)
            if (!response.acknowledged) {
                logger.warn("Manager did not acknowledge match update for ${update.matchId} (${update.status})")
            }
        } catch (e: Exception) {
            logger.error("ReportMatchUpdate failed for match ${update.matchId} (${update.status}): ${e.message}")
        }
    }
}

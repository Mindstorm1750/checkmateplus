package org.checkmateplus.gameservermanager.services

import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.botEngine.BotConfiguration
import org.checkmateplus.chessLibrary.botEngine.BotModelType
import org.checkmateplus.chessLibrary.botEngine.BotMoveTimeModelType
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.gameservermanager.models.TrackedMatch
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * Stores per-match snapshots received via gRPC into Redis so the GameServerManager
 * acts as the observability hub for every match in the system.
 *
 * Schema:
 *   gsm:matches:{match_id}   STRING (JSON of TrackedMatch)
 *
 * Terminal states (COMPLETED/CANCELLED/ABANDONED) get a 1h TTL so dashboards can
 * still show recently finished games before they roll off.
 */
@Service
class MatchTrackingService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val json: Json,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MATCH_KEY_PREFIX = "gsm:matches:"
        private const val TERMINAL_TTL_SECONDS = 3600L
    }

    fun recordUpdate(update: GameServerProto.MatchUpdate) {
        if (update.matchId.isEmpty()) {
            logger.warn("Ignoring MatchUpdate with empty match_id")
            return
        }
        val tracked = toTrackedMatch(update)
        val key = MATCH_KEY_PREFIX + tracked.matchId
        redisTemplate.opsForValue().set(key, json.encodeToString(TrackedMatch.serializer(), tracked))
        if (isTerminal(update.status)) {
            redisTemplate.expire(key, TERMINAL_TTL_SECONDS, TimeUnit.SECONDS)
        }
        logger.debug("Tracked match {} -> {}", tracked.matchId, tracked.status)
    }

    fun getMatch(matchId: String): TrackedMatch? {
        val raw = redisTemplate.opsForValue().get(MATCH_KEY_PREFIX + matchId) ?: return null
        return json.decodeFromString(TrackedMatch.serializer(), raw)
    }

    fun listLiveMatches(): List<TrackedMatch> {
        val keys = redisTemplate.keys("$MATCH_KEY_PREFIX*")
        if (keys.isEmpty()) return emptyList()
        val jsons = redisTemplate.opsForValue().multiGet(keys) ?: return emptyList()
        return jsons.filterNotNull().map { json.decodeFromString(TrackedMatch.serializer(), it) }
    }

    private fun toTrackedMatch(matchUpdate: GameServerProto.MatchUpdate): TrackedMatch {
        val botConfig = if (matchUpdate.hasBotConfiguration()) matchUpdate.botConfiguration else null
        val white = toPlayer(
            matchUpdate.whitePlayerId,
            matchUpdate.whitePlayerName,
            matchUpdate.whitePlayerRating,
            matchUpdate.whitePlayerRegistered,
            botConfig,
        )
        val black = toPlayer(
            matchUpdate.blackPlayerId,
            matchUpdate.blackPlayerName,
            matchUpdate.blackPlayerRating,
            matchUpdate.blackPlayerRegistered,
            botConfig,
        )
        return TrackedMatch(
            matchId = matchUpdate.matchId,
            serverId = matchUpdate.serverId.ifEmpty { null },
            status = matchUpdate.status.name.removePrefix("MATCH_STATUS_"),
            whitePlayer = white,
            blackPlayer = black,
            lastFen = matchUpdate.lastFen.ifEmpty { null },
            whiteTimeSeconds = if (matchUpdate.lastFen.isEmpty() && matchUpdate.whiteTimeSeconds == 0) null else matchUpdate.whiteTimeSeconds,
            blackTimeSeconds = if (matchUpdate.lastFen.isEmpty() && matchUpdate.blackTimeSeconds == 0) null else matchUpdate.blackTimeSeconds,
            activeColor = matchUpdate.activeColor.ifEmpty { null },
            moveNumber = if (matchUpdate.moveNumber == 0 && matchUpdate.lastFen.isEmpty()) null else matchUpdate.moveNumber,
            winnerId = matchUpdate.winnerId.ifEmpty { null },
            reason = matchUpdate.reason.ifEmpty { null },
            updatedAt = System.currentTimeMillis(),
            matchType = if (matchUpdate.hasMatchType()) matchUpdate.matchType.name.removePrefix("MATCH_TYPE_") else null,
            timeControl = if (matchUpdate.hasTimeControl()) {
                TimeControl.valueOf(matchUpdate.timeControl.name.removePrefix("TIME_CONTROL_"))
            } else null,
        )
    }

    private fun toPlayer(
        id: String,
        name: String,
        rating: Int,
        registered: Boolean,
        botConfig: GameServerProto.BotConfiguration?,
    ): Player? {
        if (id.isEmpty()) return null
        return when {
            registered -> Player.Registered(
                playerId = id,
                displayName = name,
                skillRating = rating,
            )
            botConfig != null -> Player.Bot(
                playerId = id,
                displayName = name,
                skillRating = rating,
                botConfiguration = BotConfiguration(
                    botModelType = BotModelType.valueOf(botConfig.botModelType.name),
                    botMoveTimeModelType = BotMoveTimeModelType.valueOf(botConfig.botMoveTimeModelType.name),
                ),
            )
            else -> Player.Anonymous(
                playerId = id,
                displayName = name,
                skillRating = rating,
            )
        }
    }

    private fun isTerminal(status: GameServerProto.MatchStatus): Boolean =
        status == GameServerProto.MatchStatus.MATCH_STATUS_COMPLETED ||
            status == GameServerProto.MatchStatus.MATCH_STATUS_CANCELLED ||
            status == GameServerProto.MatchStatus.MATCH_STATUS_ABANDONED
}

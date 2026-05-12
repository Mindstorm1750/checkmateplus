package org.checkmateplus.playerservice.services

import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.gameserver.proto.GameServerProto
import org.checkmateplus.playerservice.models.SpectateBotConfiguration
import org.checkmateplus.playerservice.models.SpectateMatch
import org.checkmateplus.playerservice.models.SpectateMatchPage
import org.checkmateplus.playerservice.models.SpectatePlayer
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicReference

/**
 * Polls GameServerManager every 5 seconds for the current set of live matches.
 */
@Service
class SpectateService(
    private val gameServerManager: GameServerManager,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val latest = AtomicReference<List<GameServerProto.LiveMatch>>(emptyList())

    @Scheduled(fixedRate = 1_000L)
    @CacheEvict(value = ["spectate_match"], allEntries = true)
    fun poll() {
        try {
            val matches = gameServerManager.listLiveMatches()
            latest.set(matches)
            logger.debug("Polled GSM: {} live matches", matches.size)
        } catch (e: Exception) {
            logger.warn("Failed to poll live matches from GSM: ${e.message}")
        }
    }

    fun getLiveMatches(afterMatchId: String?, size: Int): SpectateMatchPage {
        val sorted = latest.get().sortedWith(
            compareByDescending<GameServerProto.LiveMatch> { it.updatedAt }.thenBy { it.matchId }
        )

        val startIndex = if (afterMatchId != null) {
            val idx = sorted.indexOfFirst { it.matchId == afterMatchId }
            if (idx == -1) 0 else idx + 1
        } else 0

        val pageSize = size.coerceIn(1, 100)
        val page = sorted.drop(startIndex).take(pageSize)
        val hasMore = startIndex + page.size < sorted.size
        val nextCursor = if (hasMore) page.lastOrNull()?.matchId else null

        return SpectateMatchPage(
            matches = page.map { toSpectateMatch(it) },
            nextCursor = nextCursor,
            hasMore = hasMore,
        )
    }

    @Cacheable(value = ["spectate_match"], key = "#matchId", unless = "#result == null")
    fun getMatch(matchId: String): SpectateMatch? =
        latest.get().firstOrNull { it.matchId == matchId }?.let { toSpectateMatch(it) }

    private fun toSpectateMatch(match: GameServerProto.LiveMatch): SpectateMatch {
        val botConfig = if (match.hasBotConfiguration()) {
            SpectateBotConfiguration(
                botModelType = match.botConfiguration.botModelType.name,
                botMoveTimeModelType = match.botConfiguration.botMoveTimeModelType.name,
            )
        } else null

        return SpectateMatch(
            matchId = match.matchId,
            status = match.status.name.removePrefix("MATCH_STATUS_"),
            serverId = match.serverId.ifEmpty { null },
            websocketUrl = match.websocketUrl.ifEmpty { null },
            whitePlayer = toSpectatePlayer(
                match.whitePlayerId,
                match.whitePlayerName,
                match.whitePlayerRating,
                match.whitePlayerRegistered,
                botConfig != null && match.whitePlayerName.startsWith("Bot-"),
            ),
            blackPlayer = toSpectatePlayer(
                match.blackPlayerId,
                match.blackPlayerName,
                match.blackPlayerRating,
                match.blackPlayerRegistered,
                botConfig != null && match.blackPlayerName.startsWith("Bot-"),
            ),
            whiteTimeSeconds = match.whiteTimeSeconds,
            blackTimeSeconds = match.blackTimeSeconds,
            lastFen = match.lastFen.ifEmpty { null },
            activeColor = match.activeColor.ifEmpty { null },
            moveNumber = match.moveNumber,
            winnerId = match.winnerId.ifEmpty { null },
            reason = match.reason.ifEmpty { null },
            updatedAt = match.updatedAt,
            botConfiguration = botConfig,
            matchType = if (match.hasMatchType()) match.matchType.name.removePrefix("MATCH_TYPE_") else null,
            timeControl = if (match.hasTimeControl()) {
                TimeControl.valueOf(match.timeControl.name.removePrefix("TIME_CONTROL_"))
            } else null,
        )
    }

    private fun toSpectatePlayer(
        id: String,
        name: String,
        rating: Int,
        registered: Boolean,
        isBot: Boolean,
    ): SpectatePlayer? {
        if (id.isEmpty()) return null
        val type = when {
            registered -> "REGISTERED"
            isBot -> "BOT"
            else -> "ANONYMOUS"
        }
        return SpectatePlayer(playerId = id, displayName = name, rating = rating, type = type)
    }
}

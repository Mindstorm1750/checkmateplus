package org.checkmateplus.playerservice.services

import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.models.MatchHistoryPage
import org.checkmateplus.playerservice.models.PlayerStats
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.playerservice.repository.PlayerRepository
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
class PlayerStatsService(
    private val matchRecordRepository: MatchRecordRepository,
    private val playerRepository: PlayerRepository
) {

    fun getStats(playerId: String): PlayerStats {
        val player = playerRepository.findById(playerId).orElseThrow {
            IllegalArgumentException("Player not found")
        }
        val matches = matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)

        val wins = matches.count { it.winnerId == playerId }
        val losses = matches.count { it.loserId == playerId }
        val draws = matches.count { it.isDraw }
        val total = matches.size

        val winRate = if (total > 0) (wins.toDouble() / total * 100).roundToInt() else 0

        val winsAsWhite = matches.count { it.whitePlayer?.playerId == playerId && it.winnerId == playerId }
        val winsAsBlack = matches.count { it.blackPlayer.playerId == playerId && it.winnerId == playerId }
        val gamesAsWhite = matches.count { it.whitePlayer?.playerId == playerId }
        val gamesAsBlack = matches.count { it.blackPlayer.playerId == playerId }

        val currentStreak = calculateStreak(playerId, matches)
        val longestStreak = calculateLongestStreak(playerId, matches)

        val avgDurationMs = if (matches.isNotEmpty())
            matches.map { it.gameDurationMs }.average().toLong()
        else 0L

        val favouriteTimeControl = matches
            .groupBy { it.timeControl }
            .maxByOrNull { it.value.size }
            ?.key

        return PlayerStats(
            playerId = playerId,
            displayName = player.displayName,
            skillRating = player.skillRating,
            ratingDeviation = player.ratingDeviation,
            wins = wins,
            losses = losses,
            draws = draws,
            totalGames = total,
            winRate = winRate,
            winsAsWhite = winsAsWhite,
            winsAsBlack = winsAsBlack,
            gamesAsWhite = gamesAsWhite,
            gamesAsBlack = gamesAsBlack,
            currentStreak = currentStreak,
            longestWinStreak = longestStreak,
            averageGameDurationMs = avgDurationMs,
            favouriteTimeControl = favouriteTimeControl,
            memberSinceEpochMs = player.joinedQueueAtEpochMs
        )
    }

    fun getMatchHistory(playerId: String, afterMatchId: String?, size: Int): MatchHistoryPage {
        val allMatches = matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerIdOrderByCreatedAtEpochMsDesc(playerId, playerId)

        val startIndex = if (afterMatchId != null) {
            val idx = allMatches.indexOfFirst { it.matchId == afterMatchId }
            if (idx == -1) 0 else idx + 1
        } else 0

        val page = allMatches.drop(startIndex).take(size)
        val hasMore = startIndex + page.size < allMatches.size
        val nextCursor = if (hasMore) page.lastOrNull()?.matchId else null

        return MatchHistoryPage(
            matches = page.map { it.toSummary(playerId) },
            nextCursor = nextCursor,
            hasMore = hasMore
        )
    }

    private fun calculateStreak(playerId: String, matches: List<MatchRecordEntity>): Int {
        var streak = 0
        for (match in matches.sortedByDescending { it.createdAtEpochMs }) {
            if (match.winnerId == playerId) streak++
            else break
        }
        return streak
    }

    private fun calculateLongestStreak(playerId: String, matches: List<MatchRecordEntity>): Int {
        var longest = 0
        var current = 0
        for (match in matches.sortedBy { it.createdAtEpochMs }) {
            if (match.winnerId == playerId) {
                current++
                if (current > longest) longest = current
            } else {
                current = 0
            }
        }
        return longest
    }
}
package org.checkmateplus.playerservice.controllers

import org.checkmateplus.playerservice.models.LeaderboardEntry
import org.checkmateplus.playerservice.models.MatchHistoryPage
import org.checkmateplus.playerservice.models.PlayerStats
import org.checkmateplus.playerservice.services.PlayerStatsService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class StatsGraphQLController(
    private val playerStatsService: PlayerStatsService
) {
    @QueryMapping
    fun playerStats(@Argument playerId: String): PlayerStats =
        playerStatsService.getStats(playerId)

    @QueryMapping
    @PreAuthorize("hasRole('USER')")
    fun matchHistory(
        @Argument playerId: String,
        @Argument afterCursor: String?,
        @Argument size: Int? = 20
    ): MatchHistoryPage =
        playerStatsService.getMatchHistory(playerId, afterCursor, size ?: 20)

    @QueryMapping
    fun leaderboard(@Argument size: Int? = 10): List<LeaderboardEntry> =
        playerStatsService.getLeaderboard(size ?: 10)
}
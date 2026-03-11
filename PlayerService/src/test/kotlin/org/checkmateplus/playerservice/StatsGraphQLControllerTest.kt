package org.checkmateplus.playerservice

import org.checkmateplus.playerservice.controllers.StatsGraphQLController
import org.checkmateplus.playerservice.models.MatchHistoryPage
import org.checkmateplus.playerservice.models.PlayerStats
import org.checkmateplus.playerservice.services.PlayerStatsService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.test.assertEquals

@ExtendWith(MockitoExtension::class)
class StatsGraphQLControllerTest {

    @Mock lateinit var playerStatsService: PlayerStatsService
    @InjectMocks lateinit var controller: StatsGraphQLController

    private val playerId = "player-001"

    private fun mockStats() = PlayerStats(
        playerId = playerId,
        displayName = "TestPlayer",
        skillRating = 1200,
        ratingDeviation = 350.0,
        wins = 5, losses = 3, draws = 2,
        totalGames = 10, winRate = 50,
        winsAsWhite = 3, winsAsBlack = 2,
        gamesAsWhite = 6, gamesAsBlack = 4,
        currentStreak = 1, longestWinStreak = 3,
        averageGameDurationMs = 120_000L,
        favouriteTimeControl = null,
        memberSinceEpochMs = 0L
    )

    @Test
    fun `playerStats delegates to service`() {
        `when`(playerStatsService.getStats(playerId)).thenReturn(mockStats())

        val result = controller.playerStats(playerId)

        assertEquals(playerId, result.playerId)
        verify(playerStatsService).getStats(playerId)
    }

    @Test
    fun `matchHistory delegates to service with correct args`() {
        val expectedPage = MatchHistoryPage(emptyList(), null, false)
        `when`(playerStatsService.getMatchHistory(playerId, null, 20))
            .thenReturn(expectedPage)

        val result = controller.matchHistory(playerId, null, null)

        assertEquals(expectedPage, result)
        verify(playerStatsService).getMatchHistory(playerId, null, 20)
    }

    @Test
    fun `matchHistory uses provided size`() {
        val expectedPage = MatchHistoryPage(emptyList(), null, false)
        `when`(playerStatsService.getMatchHistory(playerId, null, 5))
            .thenReturn(expectedPage)

        controller.matchHistory(playerId, null, 5)

        verify(playerStatsService).getMatchHistory(playerId, null, 5)
    }

    @Test
    fun `matchHistory passes cursor to service`() {
        val cursor = "match-abc"
        val expectedPage = MatchHistoryPage(emptyList(), null, false)
        `when`(playerStatsService.getMatchHistory(playerId, cursor, 20))
            .thenReturn(expectedPage)

        controller.matchHistory(playerId, cursor, null)

        verify(playerStatsService).getMatchHistory(playerId, cursor, 20)
    }
}
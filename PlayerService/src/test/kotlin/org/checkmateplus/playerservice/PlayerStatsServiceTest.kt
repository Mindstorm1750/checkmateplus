package org.checkmateplus.playerservice

import org.checkmateplus.chessLibrary.playerModels.MatchStatus
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.chessLibrary.playerModels.history.MatchOutcome
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.playerservice.repository.PlayerRepository
import org.checkmateplus.playerservice.repository.RegisteredPlayerRepository
import org.checkmateplus.playerservice.services.PlayerStatsService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*
import kotlin.time.Clock

@ExtendWith(MockitoExtension::class)
class PlayerStatsServiceTest {

    @Mock
    lateinit var matchRecordRepository: MatchRecordRepository

    @Mock
    lateinit var playerRepository: PlayerRepository

    @Mock
    lateinit var RegisteredPlayerRepository: RegisteredPlayerRepository

    lateinit var service: PlayerStatsService

    private val playerId = "player-001"
    private val opponentId = "player-002"

    private val player = RegisteredPlayerEntity(
        playerId = playerId,
        displayName = "TestPlayer",
        joinedQueueAt = Clock.System.now().toEpochMilliseconds(),
        skillRating = 1200,
        ratingDeviation = 350.0,
        volatility = 0.06
    )

    @BeforeEach
    fun setup() {
        service = PlayerStatsService(
            matchRecordRepository, playerRepository,
            RegisteredPlayerRepository
        )

    }

    private fun makeMatch(
        outcome: MatchOutcome,
        whiteId: String = playerId,
        blackId: String = opponentId,
        createdAt: Long = Clock.System.now().toEpochMilliseconds()
    ): MatchRecordEntity {
        val white = RegisteredPlayerEntity(
            playerId = whiteId, displayName = "W",
            joinedQueueAt = Clock.System.now().toEpochMilliseconds()
        )
        val black = RegisteredPlayerEntity(
            playerId = blackId, displayName = "B",
            joinedQueueAt = Clock.System.now().toEpochMilliseconds()
        )
        return MatchRecordEntity(
            matchId = "match-${System.nanoTime()}",
            boardJson = "{}",
            timeControl = TimeControl.BLITZ_5_0,
            matchType = MatchType.CASUAL_PVP,
            matchOutcome = outcome,
            whitePlayer = white,
            blackPlayer = black,
            remainingWhitePlayerTime = 200L,
            remainingBlackPlayerTime = 150L,
            matchStatus = MatchStatus.COMPLETED,
            createdAtEpochMs = createdAt
        )
    }

    // --- getStats ---

    @Test
    fun `getStats throws when player not found`() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.empty())
        assertThrows<IllegalArgumentException> { service.getStats(playerId) }
    }

    @Test
    fun `getStats returns correct win loss draw counts`() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)).thenReturn(
            listOf(
                makeMatch(MatchOutcome.WHITE_WINS),  // win (player is white)
                makeMatch(MatchOutcome.BLACK_WINS),  // loss (player is white)
                makeMatch(MatchOutcome.DRAW),
                makeMatch(MatchOutcome.WHITE_WINS, whiteId = opponentId, blackId = playerId) // loss (player is black)
            )
        )

        val stats = service.getStats(playerId)

        assertEquals(1, stats.wins)
        assertEquals(2, stats.losses)
        assertEquals(1, stats.draws)
        assertEquals(4, stats.totalGames)
    }

    @Test
    fun `getStats winRate is 0 when no games played`() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)).thenReturn(
            emptyList()
        )

        val stats = service.getStats(playerId)
        assertEquals(0, stats.winRate)
        assertEquals(0, stats.totalGames)
    }

    @Test
    fun `getStats winRate is 100 when all games won`() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)).thenReturn(
            listOf(
                makeMatch(MatchOutcome.WHITE_WINS),
                makeMatch(MatchOutcome.WHITE_WINS),
            )
        )

        val stats = service.getStats(playerId)
        assertEquals(100, stats.winRate)
    }

    @Test
    fun `getStats calculates winsAsWhite and winsAsBlack correctly`() {
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(
            matchRecordRepository
                .findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)
        )
            .thenReturn(
                listOf(
                    makeMatch(MatchOutcome.WHITE_WINS, whiteId = playerId, blackId = opponentId),
                    makeMatch(MatchOutcome.BLACK_WINS, whiteId = opponentId, blackId = playerId),
                )
            )

        val stats = service.getStats(playerId)
        assertEquals(1, stats.winsAsWhite)
        assertEquals(1, stats.winsAsBlack)
    }

    @Test
    fun `getStats currentStreak counts consecutive wins from most recent`() {
        val now = Clock.System.now().toEpochMilliseconds()
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)).thenReturn(
            listOf(
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - 3000),
                makeMatch(MatchOutcome.DRAW, createdAt = now - 2000),
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - 1000),
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now),
            )
        )

        val stats = service.getStats(playerId)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `getStats longestWinStreak finds correct streak`() {
        val now = Clock.System.now().toEpochMilliseconds()
        `when`(playerRepository.findById(playerId)).thenReturn(Optional.of(player))
        `when`(matchRecordRepository.findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(playerId, playerId)).thenReturn(
            listOf(
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - 5000),
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - 4000),
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - 3000),
                makeMatch(MatchOutcome.DRAW, createdAt = now - 2000),
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - 1000),
                makeMatch(MatchOutcome.WHITE_WINS, createdAt = now),
            )
        )

        val stats = service.getStats(playerId)
        assertEquals(3, stats.longestWinStreak)
    }

    // --- getMatchHistory ---
    @Test
    fun `getMatchHistory returns first page when no cursor`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val matches = (1..5).map {
            makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - it * 1000L)
        }
        `when`(
            matchRecordRepository
                .findByWhitePlayerPlayerIdOrBlackPlayerPlayerIdOrderByCreatedAtEpochMsDesc(playerId, playerId)
        )
            .thenReturn(matches)

        val page = service.getMatchHistory(playerId, null, 3)

        assertEquals(3, page.matches.size)
        assertTrue(page.hasMore)
        assertNotNull(page.nextCursor)
    }

    @Test
    fun `getMatchHistory hasMore is false when results fit in one page`() {
        `when`(
            matchRecordRepository
                .findByWhitePlayerPlayerIdOrBlackPlayerPlayerIdOrderByCreatedAtEpochMsDesc(playerId, playerId)
        )
            .thenReturn(
                listOf(
                    makeMatch(MatchOutcome.WHITE_WINS),
                    makeMatch(MatchOutcome.DRAW)
                )
            )

        val page = service.getMatchHistory(playerId, null, 10)

        assertFalse(page.hasMore)
        assertNull(page.nextCursor)
        assertEquals(2, page.matches.size)
    }

    @Test
    fun `getMatchHistory respects cursor`() {
        val now = Clock.System.now().toEpochMilliseconds()
        val matches = (1..5).map {
            makeMatch(MatchOutcome.WHITE_WINS, createdAt = now - it * 1000L)
        }
        `when`(
            matchRecordRepository
                .findByWhitePlayerPlayerIdOrBlackPlayerPlayerIdOrderByCreatedAtEpochMsDesc(playerId, playerId)
        )
            .thenReturn(matches)

        val firstPage = service.getMatchHistory(playerId, null, 2)
        val secondPage = service.getMatchHistory(playerId, firstPage.nextCursor, 2)

        assertEquals(2, secondPage.matches.size)
        assertNotEquals(firstPage.matches[0].matchId, secondPage.matches[0].matchId)
    }

    @Test
    fun `getMatchHistory returns empty when no matches`() {
        `when`(
            matchRecordRepository
                .findByWhitePlayerPlayerIdOrBlackPlayerPlayerIdOrderByCreatedAtEpochMsDesc(playerId, playerId)
        )
            .thenReturn(emptyList())

        val page = service.getMatchHistory(playerId, null, 10)

        assertTrue(page.matches.isEmpty())
        assertFalse(page.hasMore)
        assertNull(page.nextCursor)
    }
}
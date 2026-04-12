package org.checkmateplus.playerservice
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import org.checkmateplus.chessLibrary.playerModels.history.MatchOutcome
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.checkmateplus.playerservice.models.MatchResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Clock

class MatchRecordEntityTest {

    private fun makeEntity(
        matchOutcome: MatchOutcome,
        whitePlayerId: String = "white-123",
        blackPlayerId: String = "black-456",
        remainingWhiteTime: Long = 240L,
        remainingBlackTime: Long = 180L,
        timeControl: TimeControl = TimeControl.BLITZ_5_0
    ): MatchRecordEntity {
        val whitePlayer = RegisteredPlayerEntity(
            playerId = whitePlayerId,
            displayName = "White",
            joinedQueueAt = Clock.System.now().toEpochMilliseconds()
        )
        val blackPlayer = RegisteredPlayerEntity(
            playerId = blackPlayerId,
            displayName = "Black",
            joinedQueueAt = Clock.System.now().toEpochMilliseconds()
        )
        return MatchRecordEntity(
            matchId = "match-001",
            boardJson = "{}",
            timeControl = timeControl,
            matchType = MatchType.CASUAL_PVP,
            matchOutcome = matchOutcome,
            whitePlayer = whitePlayer,
            blackPlayer = blackPlayer,
            remainingWhitePlayerTime = remainingWhiteTime,
            remainingBlackPlayerTime = remainingBlackTime,
            matchStatus = org.checkmateplus.chessLibrary.playerModels.MatchStatus.COMPLETED,
            createdAtEpochMs = Clock.System.now().toEpochMilliseconds()
        )
    }

    // --- winnerId / loserId ---

    @Test
    fun `winnerId is white player when WHITE_WINS`() {
        val entity = makeEntity(MatchOutcome.WHITE_WINS)
        assertEquals("white-123", entity.winnerId)
    }

    @Test
    fun `winnerId is black player when BLACK_WINS`() {
        val entity = makeEntity(MatchOutcome.BLACK_WINS)
        assertEquals("black-456", entity.winnerId)
    }

    @Test
    fun `winnerId is null on DRAW`() {
        val entity = makeEntity(MatchOutcome.DRAW)
        assertNull(entity.winnerId)
    }

    @Test
    fun `winnerId is null on ABANDONED`() {
        val entity = makeEntity(MatchOutcome.ABANDONED)
        assertNull(entity.winnerId)
    }

    @Test
    fun `loserId is black player when WHITE_WINS`() {
        val entity = makeEntity(MatchOutcome.WHITE_WINS)
        assertEquals("black-456", entity.loserId)
    }

    @Test
    fun `loserId is white player when BLACK_WINS`() {
        val entity = makeEntity(MatchOutcome.BLACK_WINS)
        assertEquals("white-123", entity.loserId)
    }

    @Test
    fun `loserId is null on DRAW`() {
        val entity = makeEntity(MatchOutcome.DRAW)
        assertNull(entity.loserId)
    }

    @Test
    fun `isDraw is true on DRAW`() {
        assertTrue(makeEntity(MatchOutcome.DRAW).isDraw)
    }

    @Test
    fun `isDraw is false on WHITE_WINS`() {
        assertFalse(makeEntity(MatchOutcome.WHITE_WINS).isDraw)
    }

    // --- gameDurationMs ---

    @Test
    fun `gameDurationMs calculates correctly for BLITZ_5_0`() {
        // 5 min = 300s, white used 300-240=60s, black used 300-180=120s, total=180s=180000ms
        val entity = makeEntity(
            MatchOutcome.WHITE_WINS,
            remainingWhiteTime = 240L,
            remainingBlackTime = 180L,
            timeControl = TimeControl.BLITZ_5_0
        )
        assertEquals(180_000L, entity.gameDurationMs)
    }

    @Test
    fun `gameDurationMs is zero when no time used`() {
        val entity = makeEntity(
            MatchOutcome.DRAW,
            remainingWhiteTime = 300L,
            remainingBlackTime = 300L,
            timeControl = TimeControl.BLITZ_5_0
        )
        assertEquals(0L, entity.gameDurationMs)
    }

    @Test
    fun `gameDurationMs handles full time used by both players`() {
        // 10 min = 600s, both used all time, total = 1200s = 1200000ms
        val entity = makeEntity(
            MatchOutcome.WHITE_WINS,
            remainingWhiteTime = 0L,
            remainingBlackTime = 0L,
            timeControl = TimeControl.RAPID_10_0
        )
        assertEquals(1_200_000L, entity.gameDurationMs)
    }

    // --- toSummary ---

    @Test
    fun `toSummary returns WIN for white player when WHITE_WINS`() {
        val summary = makeEntity(MatchOutcome.WHITE_WINS).toSummary("white-123")
        assertEquals(MatchResult.WIN, summary.result)
    }

    @Test
    fun `toSummary returns LOSS for black player when WHITE_WINS`() {
        val summary = makeEntity(MatchOutcome.WHITE_WINS).toSummary("black-456")
        assertEquals(MatchResult.LOSS, summary.result)
    }

    @Test
    fun `toSummary returns DRAW for either player on DRAW`() {
        val entity = makeEntity(MatchOutcome.DRAW)
        assertEquals(MatchResult.DRAW, entity.toSummary("white-123").result)
        assertEquals(MatchResult.DRAW, entity.toSummary("black-456").result)
    }

    @Test
    fun `toSummary returns ABANDONED on ABANDONED`() {
        val summary = makeEntity(MatchOutcome.ABANDONED).toSummary("white-123")
        assertEquals(MatchResult.ABANDONED, summary.result)
    }

    @Test
    fun `toSummary sets correct colour for white player`() {
        val summary = makeEntity(MatchOutcome.WHITE_WINS).toSummary("white-123")
        assertEquals(org.checkmateplus.chessLibrary.chessEngine.Colour.WHITE, summary.colour)
    }

    @Test
    fun `toSummary sets correct colour for black player`() {
        val summary = makeEntity(MatchOutcome.WHITE_WINS).toSummary("black-456")
        assertEquals(org.checkmateplus.chessLibrary.chessEngine.Colour.BLACK, summary.colour)
    }

    @Test
    fun `toSummary sets correct opponentId for white player`() {
        val summary = makeEntity(MatchOutcome.WHITE_WINS).toSummary("white-123")
        assertEquals("black-456", summary.opponentId)
    }

    @Test
    fun `toSummary sets correct opponentId for black player`() {
        val summary = makeEntity(MatchOutcome.WHITE_WINS).toSummary("black-456")
        assertEquals("white-123", summary.opponentId)
    }
}
package org.checkmateplus.stockfishengine.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class InfoParserTests {

    @Test
    fun parsesStandardInfoLine() {
        val line = InfoParser.parseInfoLine(
            "info depth 18 seldepth 24 multipv 1 score cp 35 nodes 1234 nps 5000 time 200 pv e2e4 e7e5 g1f3"
        )
        assertNotNull(line)
        assertEquals(18, line.depth)
        assertEquals(24, line.seldepth)
        assertEquals(1, line.multipv)
        assertEquals(35, line.score.cp)
        assertNull(line.score.mateIn)
        assertEquals(1234L, line.nodes)
        assertEquals(5000L, line.nps)
        assertEquals(200L, line.timeMs)
        assertEquals(listOf("e2e4", "e7e5", "g1f3"), line.pv)
    }

    @Test
    fun parsesPositiveMate() {
        val line = InfoParser.parseInfoLine("info depth 5 multipv 1 score mate 3 pv e2e4")
        assertNotNull(line)
        assertEquals(3, line.score.mateIn)
        assertNull(line.score.cp)
    }

    @Test
    fun parsesNegativeMate() {
        val line = InfoParser.parseInfoLine("info depth 5 multipv 1 score mate -2 pv e2e4")
        assertNotNull(line)
        assertEquals(-2, line.score.mateIn)
    }

    @Test
    fun parsesWdlTriple() {
        val line = InfoParser.parseInfoLine(
            "info depth 12 multipv 1 score cp 50 wdl 800 150 50 pv e2e4"
        )
        assertNotNull(line)
        assertEquals(Triple(800, 150, 50), line.wdl)
    }

    @Test
    fun ignoresInfoString() {
        assertNull(InfoParser.parseInfoLine("info string Hash usage 12%"))
    }

    @Test
    fun ignoresInfoWithoutScore() {
        assertNull(
            InfoParser.parseInfoLine("info depth 1 currmove e2e4 currmovenumber 1")
        )
    }

    @Test
    fun handlesWhitespace() {
        val line = InfoParser.parseInfoLine(
            "   info depth 4 multipv 1 score cp 10 pv e2e4   "
        )
        assertNotNull(line)
        assertEquals(4, line.depth)
    }

    @Test
    fun reorderedTokensStillParse() {
        val line = InfoParser.parseInfoLine(
            "info multipv 2 depth 10 score cp -5 nodes 99 pv c7c5"
        )
        assertNotNull(line)
        assertEquals(10, line.depth)
        assertEquals(2, line.multipv)
        assertEquals(-5, line.score.cp)
    }

    @Test
    fun handlesScoreBoundQualifier() {
        val line = InfoParser.parseInfoLine(
            "info depth 8 multipv 1 score cp 42 lowerbound nodes 100 pv e2e4"
        )
        assertNotNull(line)
        assertEquals(42, line.score.cp)
        assertEquals(100L, line.nodes)
    }

    @Test
    fun parsesBestMoveWithPonder() {
        val (best, ponder) = InfoParser.parseBestMove("bestmove e2e4 ponder e7e5")!!
        assertEquals("e2e4", best)
        assertEquals("e7e5", ponder)
    }

    @Test
    fun parsesBestMoveNone() {
        val (best, ponder) = InfoParser.parseBestMove("bestmove (none)")!!
        assertNull(best)
        assertNull(ponder)
    }

    @Test
    fun parsesBestMoveWithoutPonder() {
        val (best, ponder) = InfoParser.parseBestMove("bestmove g1f3")!!
        assertEquals("g1f3", best)
        assertNull(ponder)
    }
}

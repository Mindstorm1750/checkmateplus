package org.checkmateplus.stockfishengine.engine

import org.checkmateplus.stockfishengine.models.stockfishTypes.SearchLimit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.util.Locale
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StockfishDriverTests {

    private lateinit var driver: StockfishDriver

    private val midGameFen = "8/6pk/8/1R5p/3K3P/8/6r1/8 b - - 0 42"
    private val mateInOneFen = "7k/R7/6K1/8/8/8/8/8 w - - 0 1"
    private val startFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

    private fun executablePath(): String {
        val os = System.getProperty("os.name").lowercase(Locale.ROOT)
        val rel = if (os.contains("win")) {
            "src/main/resources/stockfishExecutables/windows/stockfish-windows-x86-64-avx2.exe"
        } else {
            "src/main/resources/stockfishExecutables/linux/stockfish-ubuntu-x86-64-avx2"
        }
        val f = File(rel)
        if (!os.contains("win") && f.exists() && !f.canExecute()) f.setExecutable(true)
        return f.absolutePath
    }

    @BeforeAll
    fun setUp() {
        driver = StockfishDriver(executablePath())
        Assertions.assertTrue(driver.start(), "Engine failed to start")
    }

    @AfterAll
    fun tearDown() {
        driver.close()
        driver.close() // idempotent
    }

    @Test
    fun analyseDepthReturnsBestMoveAndScore() {
        driver.newGame()
        driver.setPosition(midGameFen)
        val result = driver.analyse(SearchLimit.Depth(10))
        assertNotNull(result.bestMove)
        Assertions.assertTrue(result.bestMove!!.length >= 4)
        Assertions.assertTrue(result.lines.isNotEmpty(), "Expected at least one info line")
        val score = result.lines.first().score
        Assertions.assertTrue(score.cp != null || score.mateIn != null)
    }

    @Test
    fun mateInOneDetected() {
        driver.newGame()
        driver.setPosition(mateInOneFen)
        val result = driver.analyse(SearchLimit.Depth(6))
        val score = result.lines.last().score
        Assertions.assertTrue(score.isMate, "Expected mate score, got $score")
        Assertions.assertEquals(1, score.mateIn)
        assertNotNull(result.bestMove)
    }

    @Test
    fun multiPvReturnsMultipleLines() {
        driver.newGame()
        driver.setPosition(startFen)
        val result = driver.analyse(SearchLimit.Depth(10), multiPv = 3)
        Assertions.assertEquals(3, result.lines.size, "Expected 3 PVs")
        Assertions.assertEquals(listOf(1, 2, 3), result.lines.map { it.multipv })
        // restore to single PV so other tests aren't affected
        driver.setOption("MultiPV", "1")
    }

    @Test
    fun setPositionWithMovesEqualsFen() {
        // After 1.e4, FEN is well-known.
        val afterE4 = "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1"

        driver.newGame()
        driver.setPosition(startFen, listOf("e2e4"))
        val viaMoves = driver.analyse(SearchLimit.Depth(8)).bestMove

        driver.newGame()
        driver.setPosition(afterE4)
        val viaFen = driver.analyse(SearchLimit.Depth(8)).bestMove

        Assertions.assertEquals(viaFen, viaMoves)
    }

    @Test
    fun newGameKeepsBestMoveStable() {
        driver.newGame()
        driver.setPosition(startFen)
        val first = driver.analyse(SearchLimit.Depth(8)).bestMove
        driver.newGame()
        driver.setPosition(startFen)
        val second = driver.analyse(SearchLimit.Depth(8)).bestMove
        Assertions.assertEquals(first, second)
    }

    @Test
    fun bestMoveSugarRespectsMovetime() {
        val started = System.currentTimeMillis()
        val move = driver.bestMove(midGameFen, movetimeMs = 200)
        val elapsed = System.currentTimeMillis() - started
        assertNotNull(move)
        Assertions.assertTrue(move.length >= 4)
        Assertions.assertTrue(elapsed < 5_000, "bestMove took ${elapsed}ms — too slow for movetime=200")
    }

    @Test
    fun bestMoveSugarWithSkillLevel() {
        val move = driver.bestMove(midGameFen, movetimeMs = 200, skillLevel = 5)
        assertNotNull(move)
    }

    @Test
    fun bestMoveOnClockReturnsMove() {
        val started = System.currentTimeMillis()
        val move = driver.bestMoveOnClock(
            midGameFen,
            SearchLimit.Clock(wtimeMs = 60_000, btimeMs = 60_000, wincMs = 1_000, bincMs = 1_000),
        )
        val elapsed = System.currentTimeMillis() - started
        assertNotNull(move)
        Assertions.assertTrue(elapsed < 10_000, "Clock search took ${elapsed}ms — too slow for 60s clock")
    }

    @Test
    fun legalMovesFromStartIs20() {
        val moves = driver.legalMoves(startFen)
        Assertions.assertEquals(20, moves.size, "Start position has 20 legal moves, got ${moves.size}: $moves")
    }

    @Test
    fun legalMovesIncludesKnownRookMove() {
        val moves = driver.legalMoves(midGameFen)
        Assertions.assertTrue(moves.any { it.startsWith("g2") }, "Expected a g2 move: $moves")
    }

    @Test
    fun analyseAfterCloseFails() {
        val tmp = StockfishDriver(executablePath())
        Assertions.assertTrue(tmp.start())
        tmp.close()
        Assertions.assertThrows(IllegalStateException::class.java) {
            tmp.setPosition(startFen)
        }
    }
}
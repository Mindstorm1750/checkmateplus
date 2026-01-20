package uk.ac.kent.backend

import FenToBoard
import Move
import STARTING_FEN
import org.junit.jupiter.api.Test
import toFileRank
import toSquare
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineTest {
    @Test
    fun testColourOpposite() {
        val white = Colour.WHITE
        val black = Colour.BLACK
        assertEquals(white, black.opposite())
        assertEquals(black, white.opposite())
    }

    @Test
    fun testSquareToFileRank() {
        val a1 = 0
        val h1 = 7
        val a8 = 56
        val h8 = 63
        val d4 = 27

        assertEquals(a1.toFileRank(), Pair(0, 0))
        assertEquals(h1.toFileRank(), Pair(7, 0))
        assertEquals(a8.toFileRank(), Pair(0, 7))
        assertEquals(h8.toFileRank(), Pair(7, 7))
        assertEquals(d4.toFileRank(), Pair(3, 3))
    }

    @Test
    fun testFileRankToSquare() {
        val a1 = Pair(0, 0)
        val h1 = Pair(7, 0)
        val a8 = Pair(0, 7)
        val h8 = Pair(7, 7)
        val d4 = Pair(3, 3)

        assertEquals(a1.toSquare(), 0)
        assertEquals(h1.toSquare(), 7)
        assertEquals(a8.toSquare(), 56)
        assertEquals(h8.toSquare(), 63)
        assertEquals(d4.toSquare(), 27)
    }

    /**
     * Also tests the FenToBoard helper
     */
    @Test
    fun testBoardEquals() {
        val startBoard = Board.startingPosition()
        val sameBoard = FenToBoard(STARTING_FEN)
        assertTrue(sameBoard == startBoard)
    }

    @Test
    fun testBoardNotEquals() {
        val startBoard = Board.startingPosition()
        val differentBoard = Board.startingPosition().applyMove(Move(12, 28))
        assertTrue(startBoard != differentBoard)
    }

    /**
     * Moves that aren't captures or pawn moves should increment halfMoveClock
     */
    @Test
    fun testHalfMoveIncrement() {
        val board = FenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R b kq - 7 10")
        val boardAfterPieceMove = board.applyMove(Move(60, 62)) // black castles kingside
        assertEquals(board.halfMoveClock, 7)
        assertEquals(boardAfterPieceMove.halfMoveClock, 8)
    }

    /**
     * A pawn move should reset halfMoveClock to 0
     */
    @Test
    fun testHalfMovePawnReset() {
        val board = FenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R b kq - 7 10")
        val boardAfterPawnMove = board.applyMove(Move(55, 39)) // black plays pawn to h5
        assertEquals(board.halfMoveClock, 7)
        assertEquals(boardAfterPawnMove.halfMoveClock, 0)
    }

    /**
     * A capture should reset halfMoveClock to 0
     */
    @Test
    fun testHalfMoveCaptureReset() {
        val board = FenToBoard("8/8/1p6/p3n1p1/P1P1kp1p/1P3N1P/4K1P1/8 b - - 5 7")
        val boardAfterPawnMove = board.applyMove(Move(36, 21)) // black plays Nxf3
        assertEquals(board.halfMoveClock, 5)
        assertEquals(boardAfterPawnMove.halfMoveClock, 0)
    }

    /**
     * Test that fullMoveNumber is incremented when Black makes a move
     */
    @Test
    fun testFullMoveIncrement() {
        val board = FenToBoard("rnbq1rk1/ppp1bppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R b KQ - 2 6")
        val boardAfterMove = board.applyMove(Move(57, 51))
        assertEquals(board.fullMoveNumber, 6)
        assertEquals(boardAfterMove.fullMoveNumber, 7)
    }

    /**
     * Test that fullMoveNumber doesn't change when White makes a move
     */
    @Test
    fun testFullMoveNotIncrement() {
        val board = FenToBoard("r1bq1rk1/pppnbppp/4pn2/3p2B1/2PP4/2N1PN2/PP3PPP/R2QKB1R w KQ - 3 7")
        val boardAfterMove = board.applyMove(Move(5, 19))
        assertEquals(board.fullMoveNumber, 7)
        assertEquals(boardAfterMove.fullMoveNumber, 7)
    }
}
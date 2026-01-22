package uk.ac.kent.backend

import FenToBoard
import Move
import STARTING_FEN
import org.junit.jupiter.api.Test
import toFileRank
import toSquare
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.expect

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

    @Test
    fun testWhiteKingsideCastle() {
        val board = FenToBoard("r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2QK2R w KQ - 0 8")
        val expectedBoardAfterCastle = FenToBoard("r1bq1rk1/pp1nbppp/4pn2/2pp2B1/2PP4/2NBPN2/PP3PPP/R2Q1RK1 b - - 1 8")
        val boardAfterCastle = board.applyMove(Move(4, 6))
        assertEquals(expectedBoardAfterCastle, boardAfterCastle)
    }

    @Test
    fun testWhiteQueensideCastle() {
        val board = FenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/R3KB1R w KQkq - 6 10")
        val expectedBoardAfterCastle = FenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R b kq - 7 10")
        val boardAfterCastle = board.applyMove(Move(4, 2))
        assertEquals(expectedBoardAfterCastle, boardAfterCastle)
    }

    @Test
    fun testBlackKingsideCastle() {
        val board = FenToBoard("r2qk2r/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R b kq - 7 10")
        val expectedBoardAfterCastle = FenToBoard("r2q1rk1/1p1nbppp/p2pbn2/4p3/4P3/1NN1BP2/PPPQ2PP/2KR1B1R w - - 8 11")
        val boardAfterCastle = board.applyMove(Move(60, 62))
        assertEquals(expectedBoardAfterCastle, boardAfterCastle)
    }

    @Test
    fun testBlackQueensideCastle() {
        val board = FenToBoard("r3kbnr/1ppb1ppp/p1p5/8/3NP3/8/PPP2PPP/RNB2RK1 b kq - 2 8")
        val expectedBoardAfterCastle = FenToBoard("2kr1bnr/1ppb1ppp/p1p5/8/3NP3/8/PPP2PPP/RNB2RK1 w - - 3 9")
        val boardAfterCastle = board.applyMove(Move(60, 58))
        assertEquals(expectedBoardAfterCastle, boardAfterCastle)
    }

    @Test
    fun testApplyMoveCapture() {
        val board = FenToBoard("rnbq1rk1/ppp1bppp/4p3/3p2B1/2PPn3/2N1PN2/PP3PPP/R2QKB1R w KQ - 1 7")
        val expectedBoardAfterMove = FenToBoard("rnbq1rk1/ppp1bppp/4p3/3p2B1/2PPN3/4PN2/PP3PPP/R2QKB1R b KQ - 0 7")
        val boardAfterMove = board.applyMove(Move(18, 28))
        assertEquals(expectedBoardAfterMove, boardAfterMove)
    }

    @Test
    fun testApplyMoveNonCapture() {
        val board = FenToBoard("rnbq1rk1/ppp1bppp/4p3/6B1/2PPp3/4PN2/PP3PPP/R2QKB1R w KQ - 0 8")
        val expectedBoardAfterMove = FenToBoard("rnbq1rk1/ppp1bppp/4p3/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R b KQ - 1 8")
        val boardAfterMove = board.applyMove(Move(21, 36))
        assertEquals(expectedBoardAfterMove, boardAfterMove)
    }

    @Test
    fun testApplyMovePawnMove() {
        val board = FenToBoard("rnbq1rk1/ppp1bppp/4p3/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R b KQ - 1 8")
        val expectedBoardAfterMove = FenToBoard("rnbq1rk1/ppp1b1pp/4pp2/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R w KQ - 0 9")
        val boardAfterMove = board.applyMove(Move(53, 45))
        assertEquals(expectedBoardAfterMove, boardAfterMove)
    }

    @Test
    fun testApplyMovePawnTwoSquaresMove() {
        val board = FenToBoard("rnbq1rk1/ppp1bppp/4p3/4N1B1/2PPp3/4P3/PP3PPP/R2QKB1R b KQ - 1 8")
        val expectedBoardAfterMove = FenToBoard("rnbq1rk1/ppp1b1pp/4p3/4NpB1/2PPp3/4P3/PP3PPP/R2QKB1R w KQ f6 0 9")
        val boardAfterMove = board.applyMove(Move(53, 37))
        assertEquals(expectedBoardAfterMove, boardAfterMove)
    }

    @Test
    fun testApplyMoveEnPassant() {
        val board = FenToBoard("rnbqkbnr/pp2pppp/8/2ppP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 3")
        val expectedBoardAfterMove = FenToBoard("rnbqkbnr/pp2pppp/3P4/2p5/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 3")
        val boardAfterMove = board.applyMove(Move(36, 43))
        assertEquals(expectedBoardAfterMove, boardAfterMove)
    }
}
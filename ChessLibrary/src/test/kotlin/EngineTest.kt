import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Colour
import org.checkmateplus.chessLibrary.chessEngine.FenToBoard
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.chessEngine.PieceType
import org.checkmateplus.chessLibrary.chessEngine.STARTING_FEN
import org.checkmateplus.chessLibrary.chessEngine.Square
import org.checkmateplus.chessLibrary.chessEngine.toAlgebraic
import org.checkmateplus.chessLibrary.chessEngine.toFileRank
import org.checkmateplus.chessLibrary.chessEngine.toSquare
import kotlin.test.Test
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

    @Test
    fun testStringToSquare() {
        assertEquals("a1".toSquare(), 0)
        assertEquals("h1".toSquare(), 7)
        assertEquals("a8".toSquare(), 56)
        assertEquals("h8".toSquare(), 63)
        assertEquals("d4".toSquare(), 27)
    }

    @Test
    fun testSquareToAlgebraic() {
        assertEquals("a1", 0.toAlgebraic())
        assertEquals("h1", 7.toAlgebraic())
        assertEquals("a8", 56.toAlgebraic())
        assertEquals("h8", 63.toAlgebraic())
        assertEquals("d4", 27.toAlgebraic())
    }

    /**
     * Also tests the FenToBoard helper
     */
    @Test
    fun testBoardEquals() {
        val startBoard = Board.startingPosition()
        val sameBoard = FenToBoard(STARTING_FEN)
        assertEquals(sameBoard, startBoard)
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

    @Test
    fun testApplyMovePromotion() {
        val board = FenToBoard("8/k2PN3/1nK5/8/8/8/8/8 w - - 0 1")
        val expectedBoard = FenToBoard("3Q4/k3N3/1nK5/8/8/8/8/8 b - - 0 1")
        val boardAfterPromotion = board.applyMove(Move("d7".toSquare()!!, "d8".toSquare()!!, PieceType.QUEEN))
        assertEquals(expectedBoard, boardAfterPromotion)
    }

    @Test
    fun testApplyMoveUnderpromotion() {
        val board = FenToBoard("8/8/8/1p6/1P6/4Q3/2kp1K2/8 b - - 0 1")
        val expectedBoard = FenToBoard("8/8/8/1p6/1P6/4Q3/2k2K2/3n4 w - - 0 2")
        val boardAfterUnderpromotion = board.applyMove(Move("d2".toSquare()!!, "d1".toSquare()!!, PieceType.KNIGHT))
        assertEquals(expectedBoard, boardAfterUnderpromotion)
    }

    /**
     * Regardless of whose turn it is,
     * white having an extra pawn should result in a higher output from .evaluate()
     */
    @Test
    fun testEvaluateExtraPawn() {
        val whiteToPlayWithExtraPawn = FenToBoard("r3k1nr/pppp1ppp/8/8/8/8/PPPPPPPP/R3K1NR w KQkq - 0 1")
        val whiteToPlayAPawnDown = FenToBoard("r3k1nr/pppppppp/8/8/8/8/PPPP1PPP/R3K1NR w KQkq - 0 1")
        val blackToPlayAPawnDown = FenToBoard("r3k1nr/pppp1ppp/8/8/8/8/PPPPPPPP/R3K1NR b KQkq - 0 1")
        val blackToPlayWithExtraPawn = FenToBoard("r3k1nr/pppppppp/8/8/8/8/PPPP1PPP/R3K1NR b KQkq - 0 1")
        assert(whiteToPlayWithExtraPawn.evaluate() > whiteToPlayAPawnDown.evaluate())
        assert(whiteToPlayWithExtraPawn.evaluate() > blackToPlayWithExtraPawn.evaluate())
        assert(blackToPlayAPawnDown.evaluate() > whiteToPlayAPawnDown.evaluate())
        assert(blackToPlayAPawnDown.evaluate() > blackToPlayWithExtraPawn.evaluate())
    }

    /**
     * The "exchange" means a trade of a rook for a bishop or knight.
     * This tests the majority case where the side "up the exchange" should be better.
     */
    @Test
    fun testEvaluateExchangeImbalance() {
        val whiteHasRookForKnight = FenToBoard("1n2k3/pp3ppp/2p5/3p4/3P4/4P3/PP3PPP/1R2K3 b - - 0 1")
        val blackHasRookForBishop = FenToBoard("r3k3/2p2ppp/p2p4/1p2p3/3PP3/2P5/PPB2PPP/4K3 w q - 0 1")
        assert(whiteHasRookForKnight.evaluate() > 0)
        assert(blackHasRookForBishop.evaluate() < 0)
    }

    /**
     * A queen is worth more than a rook and bishop/knight.
     */
    @Test
    fun testEvaluateQueenVsRookAndMinor() {
        val blackHasQueenForRookAndBishop = FenToBoard("3qk3/pp3ppp/4p3/2ppP3/3P4/2P5/PP3PPP/R3KB2 w Q - 0 1")
        val whiteHasQueenForRookAndKnight = FenToBoard("r3k3/pp3ppp/2n1p3/3p4/3P4/4P3/PP3PPP/3QK3 w q - 0 1")
        assert(blackHasQueenForRookAndBishop.evaluate() < 0)
        assert(whiteHasQueenForRookAndKnight.evaluate() > 0)
    }

    /**
     * A bishop or knight is worth more than 2 pawns.
     */
    @Test
    fun testEvaluateMinorVsTwoPawns() {
        val blackHasKnightForTwoPawns = FenToBoard("r1bqkb1r/pppn1p2/3p1n2/4p1B1/4P3/3P4/PPP2PPP/RN1QKB1R b KQkq - 0 1")
        val whiteHasBishopForTwoPawns = FenToBoard("r1bqk2r/ppp2ppp/3ppn2/8/1n1PP3/2N5/2P2PPP/R1BQKBNR b KQkq - 0 1")
        assert(blackHasKnightForTwoPawns.evaluate() < 0)
        assert(whiteHasBishopForTwoPawns.evaluate() > 0)
    }

    /**
     * Extensively testing that Board.legalMoves() works by recursively calling legalMoves()
     * until a fixed depth and comparing the total number of positions reached
     * with a known correct number.
     * The position used is not the starting position, but instead a famous position
     * for engine testing called "kiwipete".
     * This recursive function is called "perft".
     */
    @Test
    fun testLegalMovesPerft() {
        val board = FenToBoard("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        val expectedResultForDepthFive = 193690690

        fun perft(board: Board, depth: Int): Int {
            val moves = board.generateLegalMoves()
            if(depth == 1) {
                return moves.size
            }
            var count = 0
            for (move in moves) {
                count += perft(board.applyMove(move), depth - 1)
            }

            return count
        }
        assertEquals(expectedResultForDepthFive, perft(board, 5))
    }

    /**
     * Test that legalMoves() works on a near-middlegame position
     */
    @Test
    fun testLegalMovesMiddleGame() {
        val board = FenToBoard("r3k2r/2qbbppp/p1nppn2/1p3P2/3NP3/2N1B2P/PPPQB1P1/R3K2R w KQkq - 0 12")
        val expectedLegalMoves: Set<Move> = setOf(
            Move("a2".toSquare()!!, "a3".toSquare()!!),
            Move("b2".toSquare()!!, "b3".toSquare()!!),
            Move("g2".toSquare()!!, "g3".toSquare()!!),
            Move("h3".toSquare()!!, "h4".toSquare()!!),
            Move("e4".toSquare()!!, "e5".toSquare()!!),
            Move("a2".toSquare()!!, "a4".toSquare()!!),
            Move("b2".toSquare()!!, "b4".toSquare()!!),
            Move("g2".toSquare()!!, "g4".toSquare()!!),
            Move("f5".toSquare()!!, "e6".toSquare()!!),
            Move("c3".toSquare()!!, "b1".toSquare()!!),
            Move("c3".toSquare()!!, "d1".toSquare()!!),
            Move("c3".toSquare()!!, "a4".toSquare()!!),
            Move("c3".toSquare()!!, "b5".toSquare()!!),
            Move("c3".toSquare()!!, "d5".toSquare()!!),
            Move("d4".toSquare()!!, "b3".toSquare()!!),
            Move("d4".toSquare()!!, "f3".toSquare()!!),
            Move("d4".toSquare()!!, "b5".toSquare()!!),
            Move("d4".toSquare()!!, "c6".toSquare()!!),
            Move("d4".toSquare()!!, "e6".toSquare()!!),
            Move("e2".toSquare()!!, "d1".toSquare()!!),
            Move("e2".toSquare()!!, "f1".toSquare()!!),
            Move("e2".toSquare()!!, "d3".toSquare()!!),
            Move("e2".toSquare()!!, "f3".toSquare()!!),
            Move("e2".toSquare()!!, "c4".toSquare()!!),
            Move("e2".toSquare()!!, "g4".toSquare()!!),
            Move("e2".toSquare()!!, "b5".toSquare()!!),
            Move("e2".toSquare()!!, "h5".toSquare()!!),
            Move("e3".toSquare()!!, "g1".toSquare()!!),
            Move("e3".toSquare()!!, "f2".toSquare()!!),
            Move("e3".toSquare()!!, "f4".toSquare()!!),
            Move("e3".toSquare()!!, "g5".toSquare()!!),
            Move("e3".toSquare()!!, "h6".toSquare()!!),
            Move("a1".toSquare()!!, "b1".toSquare()!!),
            Move("a1".toSquare()!!, "c1".toSquare()!!),
            Move("a1".toSquare()!!, "d1".toSquare()!!),
            Move("h1".toSquare()!!, "f1".toSquare()!!),
            Move("h1".toSquare()!!, "g1".toSquare()!!),
            Move("h1".toSquare()!!, "h2".toSquare()!!),
            Move("d2".toSquare()!!, "c1".toSquare()!!),
            Move("d2".toSquare()!!, "d1".toSquare()!!),
            Move("d2".toSquare()!!, "d3".toSquare()!!),
            Move("e1".toSquare()!!, "d1".toSquare()!!),
            Move("e1".toSquare()!!, "f1".toSquare()!!),
            Move("e1".toSquare()!!, "f2".toSquare()!!),
            Move("e1".toSquare()!!, "g1".toSquare()!!),
            Move("e1".toSquare()!!, "c1".toSquare()!!)
        )
    assertEquals(expectedLegalMoves, board.generateLegalMoves().toSet())
    }

    /**
     * Test that legalMoves() works with pawn promotion moves as white
     */
    @Test
    fun testLegalMovesWhitePromotion() {
        val board = FenToBoard("8/1P6/7k/8/7K/8/8/8 w - - 0 1")
        val expectedLegalMoves: Set<Move> = setOf(
            Move("b7".toSquare()!!, "b8".toSquare()!!, PieceType.QUEEN),
            Move("b7".toSquare()!!, "b8".toSquare()!!, PieceType.ROOK),
            Move("b7".toSquare()!!, "b8".toSquare()!!, PieceType.BISHOP),
            Move("b7".toSquare()!!, "b8".toSquare()!!, PieceType.KNIGHT),
            Move("h4".toSquare()!!, "g3".toSquare()!!),
            Move("h4".toSquare()!!, "h3".toSquare()!!),
            Move("h4".toSquare()!!, "g4".toSquare()!!)
        )
        assertEquals(expectedLegalMoves, board.generateLegalMoves().toSet())
    }

    /**
     * Test that legalMoves() works with pawn promotion moves as black
     */
    @Test
    fun testLegalMovesBlackPromotion() {
        val board = FenToBoard("8/8/7k/8/7K/8/1p6/8 b - - 0 1")
        val expectedLegalMoves: Set<Move> = setOf(
            Move("b2".toSquare()!!, "b1".toSquare()!!, PieceType.QUEEN),
            Move("b2".toSquare()!!, "b1".toSquare()!!, PieceType.ROOK),
            Move("b2".toSquare()!!, "b1".toSquare()!!, PieceType.BISHOP),
            Move("b2".toSquare()!!, "b1".toSquare()!!, PieceType.KNIGHT),
            Move("h6".toSquare()!!, "h7".toSquare()!!),
            Move("h6".toSquare()!!, "g7".toSquare()!!),
            Move("h6".toSquare()!!, "g6".toSquare()!!)
        )
        assertEquals(expectedLegalMoves, board.generateLegalMoves().toSet())
    }

    @Test
    fun testGetBoardState() {
        val boardInProgress = FenToBoard("rn1q1rk1/ppp1bppp/3pp3/4Pb2/3P4/2P2NP1/P1P1BP1P/R1BQ1RK1 b - - 0 9")
        val boardStalemate = FenToBoard("2k5/2P5/2K5/8/8/8/8/8 b - - 0 1")
        val boardCheckmate = FenToBoard("kr6/pp6/1N6/8/8/8/8/R1K5 b - - 0 1")

        assertEquals(BoardState.IN_PROGRESS, boardInProgress.getBoardState())
        assertEquals(BoardState.STALEMATE, boardStalemate.getBoardState())
        assertEquals(BoardState.CHECKMATE, boardCheckmate.getBoardState())
    }
    @Test
    fun testBoardToFenString(){
        val board = FenToBoard("rn1q1rk1/ppp1bppp/3pp3/4Pb2/3P4/2P2NP1/P1P1BP1P/R1BQ1RK1 b - - 0 9")
        assertEquals(board.toFen(), "rn1q1rk1/ppp1bppp/3pp3/4Pb2/3P4/2P2NP1/P1P1BP1P/R1BQ1RK1 b - - 0 9")
    }
    @Test
    fun testBoardToFenString_ComplexPosition() {
        val board = FenToBoard("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 4 5")
        assertEquals("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 4 5", board.toFen())
    }
    @Test
    fun testBoardToFenString_ScholarsMate() {
        val board = FenToBoard("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4")
        assertEquals("r1bqkb1r/pppp1Qpp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4", board.toFen())
    }
}
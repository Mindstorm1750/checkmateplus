package org.checkmateplus.gameserver.util

import kotlin.test.Test
import kotlin.test.assertEquals
import org.checkmateplus.chessLibrary.chessEngine.FenToBoard
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.chessEngine.PieceType
import org.checkmateplus.chessLibrary.chessEngine.SanFormatter
import org.checkmateplus.chessLibrary.chessEngine.toSquare

class SanFormatterTest {

    private fun san(fen: String, fromAlg: String, toAlg: String, promotion: PieceType? = null): String {
        val pre = FenToBoard(fen)
        val move = Move(
            from = fromAlg.toSquare()!!,
            to = toAlg.toSquare()!!,
            promotion = promotion
        )
        val post = pre.applyMove(move)
        return SanFormatter.toSan(pre, move, post)
    }

    @Test
    fun pawnPushFromStart() {
        assertEquals("e4", san("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", "e2", "e4"))
    }

    @Test
    fun pawnCapture() {
        // White pawn on e4, black pawn on d5, white captures.
        assertEquals(
            "exd5",
            san("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "e4", "d5")
        )
    }

    @Test
    fun knightDisambiguationByFile() {
        // Two white knights on b1 and f3 can both reach d2.
        // Move Nbd2 (knight from b1).
        assertEquals(
            "Nbd2",
            san("8/8/8/8/8/5N2/8/1N1K3k w - - 0 1", "b1", "d2")
        )
    }

    @Test
    fun castlingKingside() {
        assertEquals(
            "O-O",
            san("rnbqk2r/pppp1ppp/5n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4", "e1", "g1")
        )
    }

    @Test
    fun castlingQueenside() {
        assertEquals(
            "O-O-O",
            san("r3kbnr/pppqpppp/2n5/3p1b2/3P1B2/2N5/PPPQPPPP/R3KBNR w KQkq - 6 5", "e1", "c1")
        )
    }

    @Test
    fun pawnPromotion() {
        assertEquals(
            "e8=Q",
            san("8/4P3/8/8/8/8/8/4K2k w - - 0 1", "e7", "e8", PieceType.QUEEN)
        )
    }

    @Test
    fun bishopCaptureWithCheck() {
        // Greek-gift style: white bishop d3 captures pawn h7, attacks black king on g8.
        assertEquals(
            "Bxh7+",
            san("6k1/7p/8/8/8/3B4/8/4K3 w - - 0 1", "d3", "h7")
        )
    }

    @Test
    fun queenCheckmate() {
        // Scholar's mate: position before 4.Qxf7#.
        assertEquals(
            "Qxf7#",
            san("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 0 4", "h5", "f7")
        )
    }

    @Test
    fun enPassantCapture() {
        // After 1.e4 c5 2.e5 d5 black just played d5, white captures en passant: exd6
        assertEquals(
            "exd6",
            san("rnbqkbnr/pp2pppp/8/2pPp3/8/8/PPP1PPPP/RNBQKBNR w KQkq d6 0 3", "e5", "d6")
        )
    }
}

import org.chessmateplus.chessLibrary.chessEngine.Colour
import org.chessmateplus.chessLibrary.chessEngine.toFileRank
import org.chessmateplus.chessLibrary.chessEngine.toSquare
import kotlin.test.Test
import kotlin.test.assertEquals

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


}
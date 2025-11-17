enum class PieceType{
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

enum class Colour {
    WHITE, BLACK;
    fun opposite() = if (this == WHITE) BLACK else Colour.WHITE
}

data class Piece(val type: PieceType, val color: Colour)

typealias Square = Int //All Squares are now Int - code easier to read

fun Square.toFileRank(): Pair<Square, Square> = this % 8 to this / 8
fun fileRank (file: Int, rank:Int): Square = rank*8 + file

//Move representation
data class Move(
    val from: Square, // Where piece starts
    val to: Square, // Where piece ends up
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
)

data class CastlingRights(
    var whiteKingSide: Boolean = true,
    var whiteQueenSide: Boolean = true,
    var blackKingSide: Boolean = true,
    var blackQueenSide: Boolean = true
)

class Board{
    private val squares: Array<Piece?> = Array(64) { null }
    var sideToMove = Colour.WHITE
    var castlingRights = CastlingRights()
    var enPassantSquare: Square? = null
    var halfMoveClock = 0
    var fullMoveNumber = 1

    init{
        setupStartPosition()
    }

    private fun setupStartPosition() {
        //Clear the board
        for (i in 0 until 64) squares[i] = null

        //Input Pawns
        for (file in 0..7){
            squares[fileRank(file,1)] = Piece(PieceType.PAWN, Colour.WHITE)
            squares[fileRank(file,6)] = Piece(PieceType.PAWN, Colour.BLACK)
        }

        val backRank = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
            PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
            PieceType.KNIGHT, PieceType.ROOK
        )
        //Fill in back ranks
        for (file in 0..7){
            squares[fileRank(file,0)] = Piece(backRank[file], Colour.WHITE)
            squares[fileRank(file,7)] = Piece(backRank[file], Colour.BLACK)

        }

        sideToMove = Colour.WHITE
        castlingRights = CastlingRights()
        enPassantSquare = null
        halfMoveClock = 0
        fullMoveNumber = 1

    }


    fun generatePawnMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = if(color == Colour.WHITE) 8 else -8
        val startRank = if(color == Colour.WHITE) 1 else 6
        val forward = from + direction

        //Move forward 1 square
        if (forward in 0..63 && squares[forward] == null){
            moves.add(Move(from, forward))

            //Move forward 2 squares from starting position
            val doubleForward = from + 2 * direction
            if ((from / 8 == startRank) && squares[doubleForward] == null && squares[forward]==null){
                moves.add(Move(from, doubleForward))
            }
        }

        //Capture
        val captureOffsets = if (color == Colour.WHITE) listOf(7,9) else listOf(-7, -9)
        for (offset in captureOffsets){
            val target = from + offset
            if (target in 0..63){
                val piece = squares[target]
                if(piece != null && piece.color != color){
                    moves.add(Move(from, target, isCapture = true))
                }
            }
        }



        return moves
    }

    //to be completed
    fun generateKnightMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val knightMoves = listOf(-17, -15, -10, -6, 6, 10, 15, 17)

        return moves

    }



    //temp testing function
    fun printBoard() {
        println("  a b c d e f g h")
        for (rank in 7 downTo 0) {
            print("${rank + 1} ")
            for (file in 0..7) {
                val sq = fileRank(file, rank)
                val piece = squares[sq]
                val symbol = when (piece?.type) {
                    PieceType.PAWN -> if (piece.color == Colour.WHITE) "P" else "p"
                    PieceType.KNIGHT -> if (piece.color == Colour.WHITE) "N" else "n"
                    PieceType.BISHOP -> if (piece.color == Colour.WHITE) "B" else "b"
                    PieceType.ROOK -> if (piece.color == Colour.WHITE) "R" else "r"
                    PieceType.QUEEN -> if (piece.color == Colour.WHITE) "Q" else "q"
                    PieceType.KING -> if (piece.color == Colour.WHITE) "K" else "k"
                    null -> "."
                }
                print("$symbol ")
            }
            println("${rank + 1}")
        }
        println("  a b c d e f g h")
        println("Side to move: $sideToMove")
    }


}




fun main() {
    val board = Board()   // sets up the starting position
    board.printBoard()    // 👈 shows the board in the console
}

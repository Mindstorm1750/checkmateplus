package uk.ac.kent.chessLibrary.chessEngine

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
            squares[Pair(file,1).toSquare()] = Piece(PieceType.PAWN, Colour.WHITE)
            squares[Pair(file,6).toSquare()] = Piece(PieceType.PAWN, Colour.BLACK)
        }

        val backRank = listOf(
            PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP,
            PieceType.QUEEN, PieceType.KING, PieceType.BISHOP,
            PieceType.KNIGHT, PieceType.ROOK
        )
        //Fill in back ranks
        for (file in 0..7){
            squares[Pair(file,0).toSquare()] = Piece(backRank[file], Colour.WHITE)
            squares[Pair(file,7).toSquare()] = Piece(backRank[file], Colour.BLACK)

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
                val fileRank = Pair(file, rank)
                val sq = fileRank.toSquare()
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
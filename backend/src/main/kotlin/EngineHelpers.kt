const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

val charToPieceMap: Map<Char, Piece> = mapOf(
    'p' to Piece(PieceType.PAWN, color = Colour.BLACK),
    'n' to Piece(PieceType.KNIGHT, color = Colour.BLACK),
    'b' to Piece(PieceType.BISHOP, color = Colour.BLACK),
    'r' to Piece(PieceType.ROOK, color = Colour.BLACK),
    'q' to Piece(PieceType.QUEEN, color = Colour.BLACK),
    'k' to Piece(PieceType.KING, color = Colour.BLACK),

    'P' to Piece(PieceType.PAWN, color = Colour.WHITE),
    'N' to Piece(PieceType.KNIGHT, color = Colour.WHITE),
    'B' to Piece(PieceType.BISHOP, color = Colour.WHITE),
    'R' to Piece(PieceType.ROOK, color = Colour.WHITE),
    'Q' to Piece(PieceType.QUEEN, color = Colour.WHITE),
    'K' to Piece(PieceType.KING, color = Colour.WHITE),
)

val pieceToCharMap: Map<Piece, Char> = charToPieceMap.map { (k, v) -> v to k }.toMap()

/**
 * Turns a FEN string into a board. Intended to help engine testing.
 * Can also be used to create a Board for the starting position using STARTING_FEN
 * @return a Board represented by the FEN string
 */
fun FenToBoard(fen: String): Board {
    val fenParts = fen.trim().split(' ')

    var squares: Array<Piece?> = Array(64) { null }
    val sideToMove = if(fenParts[1].first() == 'w') Colour.WHITE else Colour.BLACK
    val castlingRights = CastlingRights(
        whiteKingSide = fenParts[2].contains('K'),
        whiteQueenSide = fenParts[2].contains('Q'),
        blackKingSide = fenParts[2].contains('k'),
        blackQueenSide = fenParts[2].contains('q'),)
    var enPassantSquare: Square? = null
    val halfMoveClock = fenParts[4].toInt()
    val fullMoveNumber = fenParts[5].toInt()

    if(fenParts[3].first() != '-') {
        val epSquareFile = fenParts[3][0].code - 97 // 'a' is 0, 'b' is 1, 'c' is 2, ... 'h' is 7
        val epSquareRank = fenParts[3][1].digitToInt() - 1 // -1 because of 0-indexing
        val fileRank: FileRank = Pair(epSquareFile, epSquareRank)
        enPassantSquare = fileRank.toSquare()
    }

    // fen starts from the 8th rank, we want to iterate starting from the 1st rank, hence reversed()
    val boardFEN = fenParts[0].trim().split('/').reversed().joinToString("")

    var i = 0
    while(i < squares.size) {
        val squareChar = boardFEN[i]
        val square = charToPieceMap[squareChar]

        if (square != null) {
            squares[i] = square
        }
        else { // if square is null then squareChar must be a number representing a series of empty squares
            i += squareChar.digitToInt()
        }
        i++
    }

    return Board() // placeholder until Board class allows parameters in its constructor
}
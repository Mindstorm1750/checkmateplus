package uk.ac.kent.chessLibrary.chessEngine

import kotlin.math.abs

data class Board(
    val squares: Array<Piece?> = Array(64) { null },
    val sideToMove: Colour = Colour.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantSquare: Square? = null,
    val halfMoveClock: Int = 0,
    val fullMoveNumber: Int = 1
) {

    fun applyMove(move: Move): Board {
        val newSquares = squares.copyOf()
        val piece = newSquares[move.from]
            ?: throw IllegalArgumentException("No piece on square ${move.from}")

        newSquares[move.from] = null
        newSquares[move.to] = if (move.promotion!= null){
            Piece(move.promotion, piece.color)
        } else{
            piece
        }

        val newEnPassantSquare = if (piece.type == PieceType.PAWN && abs(move.to - move.from) ==16){
            (move.from + move.to)/2
        }else{
            null
        }

        val isCapture = squares[move.to] != null
        val newHalfMoveClock = if (isCapture|| piece.type == PieceType.PAWN) {
            0
        }else{
            halfMoveClock +1
        }

        val newFullMoveNumber = if (sideToMove == Colour.BLACK){
            fullMoveNumber+1
        }else {
            fullMoveNumber
        }
        //castling to be completed

        return Board(
            squares = newSquares,
            sideToMove = sideToMove.opposite(),
            castlingRights = castlingRights,
            enPassantSquare = newEnPassantSquare,
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMoveNumber
        )
    }

    fun generatePawnMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = if (color == Colour.WHITE) 8 else -8
        val startRank = if (color == Colour.WHITE) 1 else 6
        val forward = from + direction

        //Move forward 1 square
        if (forward in 0..63 && squares[forward] == null) {
            moves.add(Move(from, forward))

            //Move forward 2 squares from starting position
            val doubleForward = from + 2 * direction
            if ((from / 8 == startRank) && squares[doubleForward] == null && squares[forward] == null) {
                moves.add(Move(from, doubleForward))
            }
        }

        //Capture
        val captureOffsets = if (color == Colour.WHITE) listOf(7, 9) else listOf(-7, -9)
        for (offset in captureOffsets) {
            val target = from + offset
            if (target in 0..63) {
                val piece = squares[target]
                if (piece != null && piece.color != color) {
                    moves.add(Move(from, target))
                }
            }
        }

        return moves
    }

    //knight moves
    fun generateKnightMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val knightMoves = listOf(-17, -15, -10, -6, 6, 10, 15, 17)

        val (fromFile, fromRank) = from.toFileRank()

        for(offset in knightMoves){
            val target = from+offset
            if (target in 0..63){
                val (toFile, toRank) = target.toFileRank()
                //ensure knight doesnt wrap around board
                val fileDiff = abs(toFile - fromFile)
                val rankDiff = abs(toRank - fromRank)

                if((fileDiff==2 && rankDiff == 1)||(fileDiff == 1 && rankDiff == 2)){
                    val piece = squares[target]
                    if(piece == null){
                        moves.add(Move(from, target))
                    } else if (piece.color!= color){
                        moves.add(Move(from, target))
                    }

                }

            }
        }


        return moves
    }

    fun generateBishopMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(-9, -7, 7, 9)
        val (fromFile, fromRank) = from.toFileRank()

        for (direction in directions){
            var target = from + direction
            var currentFile = fromFile
            var currentRank = fromRank

            while(target in 0..63){
                val(toFile, toRank) = target.toFileRank()
                val fileDiff = abs(toFile - currentFile)
                val rankDiff = abs(toRank - currentRank)
                if(fileDiff!=1 || rankDiff!=1){
                    break
                }
                val piece = squares[target]
                if(piece == null){
                    moves.add(Move(from, target))
                } else{
                    if (piece.color != color){
                        moves.add(Move(from, target))
                    }
                    break
                }

                currentFile = toFile
                currentRank = toRank
                target += direction
            }
        }
        return moves
    }

    fun generateRookMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(-8, -1, 1, 8)

        for (direction in directions){
            var target = from + direction
            val (fromFile, _) = from.toFileRank()

            while(target in 0..63){
                val (toFile, _) = target.toFileRank()

                if(direction == 1 || direction == -1){
                    if (abs(toFile - fromFile) >1){
                        break
                    }
                }
                val piece = squares[target]
                if(piece == null){
                    moves.add(Move(from, target))
                    target+= direction
                } else{
                    if(piece.color != color){
                        moves.add(Move(from, target))
                    }
                    break
                }
            }

        }
        return moves
    }

    fun generateQueenMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        moves.addAll(generateBishopMoves(from, color))
        moves.addAll(generateRookMoves(from, color))
        return moves
    }

    fun generateKingMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val kingMoves = listOf(-9, -8, -7, -1, 1, 7, 8, 9)
        val (fromFile, fromRank) = from.toFileRank()

        for (offset in kingMoves) {
            val target = from + offset
            if (target in 0..63) {
                val (toFile, toRank) = target.toFileRank()

                // Check king didn't wrap around board
                val fileDiff = abs(toFile - fromFile)
                val rankDiff = abs(toRank - fromRank)

                if (fileDiff <= 1 && rankDiff <= 1) {
                    val piece = squares[target]
                    if (piece == null || piece.color != color) {
                        moves.add(Move(from, target))
                    }
                }
            }
        }

        // TODO: Add castling moves later

        return moves
    }




    companion object {
        fun startingPosition(): Board {
            val squares = Array<Piece?>(64) { null }

            //input pawns on row 1 and 6
            for (file in 0..7) {
                squares[Pair(file, 1).toSquare()] = Piece(PieceType.PAWN, Colour.WHITE)
                squares[Pair(file, 6).toSquare()] = Piece(PieceType.PAWN, Colour.BLACK)
            }

            val backRank = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK,
            )

            for (file in 0..7) {
                squares[Pair(file, 0).toSquare()] = Piece(backRank[file], Colour.WHITE)
                squares[Pair(file, 7).toSquare()] = Piece(backRank[file], Colour.BLACK)
            }

            return Board(squares = squares)
        }
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Board) return false

        if (!squares.contentEquals(other.squares)) return false
        if (sideToMove != other.sideToMove) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantSquare != other.enPassantSquare) return false
        if (halfMoveClock != other.halfMoveClock) return false
        if (fullMoveNumber != other.fullMoveNumber) return false

        return true
    }

    override fun hashCode(): Int {
        var result = squares.contentHashCode()
        result = 31 * result + sideToMove.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantSquare ?: 0)
        result = 31 * result + halfMoveClock
        result = 31 * result + fullMoveNumber
        return result
    }



    //temp testing function
    fun printBoard() {
        println(" a b c d e f g h")
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
        println(" a b c d e f g h")
        println("Side to move: $sideToMove")
    }
}






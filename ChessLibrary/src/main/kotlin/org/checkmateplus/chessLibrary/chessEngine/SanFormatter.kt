package org.checkmateplus.chessLibrary.chessEngine

import kotlin.math.abs

object SanFormatter {

    fun toSan(preMoveBoard: Board, move: Move, postMoveBoard: Board): String {
        val piece = preMoveBoard.squares[move.from]
            ?: throw IllegalArgumentException("No piece on from-square ${move.from}")
        val pieceType = piece.type
        val colour = piece.color
        val opponent = colour.opposite()

        if (pieceType == PieceType.KING && abs(move.to - move.from) == 2) {
            val castle = when (move.to) {
                6, 62 -> "O-O"
                2, 58 -> "O-O-O"
                else -> throw IllegalStateException("Invalid castling target ${move.to}")
            }
            return castle + checkSuffix(postMoveBoard, opponent)
        }

        val isPawn = pieceType == PieceType.PAWN
        val isCapture = preMoveBoard.squares[move.to] != null ||
                (isPawn && move.to == preMoveBoard.enPassantSquare)

        val sb = StringBuilder()

        if (isPawn) {
            if (isCapture) {
                sb.append('a' + (move.from % 8))
                sb.append('x')
            }
            sb.append(move.to.toAlgebraic())
            move.promotion?.let { sb.append('=').append(pieceLetter(it)) }
        } else {
            sb.append(pieceLetter(pieceType))
            val ambiguous = preMoveBoard.generateLegalMoves().filter { other ->
                other != move &&
                        other.to == move.to &&
                        preMoveBoard.squares[other.from]?.type == pieceType &&
                        preMoveBoard.squares[other.from]?.color == colour
            }
            if (ambiguous.isNotEmpty()) {
                val (fromFile, fromRank) = move.from.toFileRank()
                val fileCollides = ambiguous.any { it.from.toFileRank().first == fromFile }
                val rankCollides = ambiguous.any { it.from.toFileRank().second == fromRank }
                when {
                    !fileCollides -> sb.append('a' + fromFile)
                    !rankCollides -> sb.append(fromRank + 1)
                    else -> sb.append(move.from.toAlgebraic())
                }
            }
            if (isCapture) sb.append('x')
            sb.append(move.to.toAlgebraic())
        }

        sb.append(checkSuffix(postMoveBoard, opponent))
        return sb.toString()
    }

    private fun pieceLetter(type: PieceType): String = when (type) {
        PieceType.PAWN -> ""
        PieceType.KNIGHT -> "N"
        PieceType.BISHOP -> "B"
        PieceType.ROOK -> "R"
        PieceType.QUEEN -> "Q"
        PieceType.KING -> "K"
    }

    private fun checkSuffix(postMoveBoard: Board, opponent: Colour): String {
        val state = postMoveBoard.getBoardState()
        return when {
            state == BoardState.CHECKMATE -> "#"
            postMoveBoard.isInCheck(opponent) -> "+"
            else -> ""
        }
    }
}
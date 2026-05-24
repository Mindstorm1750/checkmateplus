package org.checkmateplus.chessLibrary.chessEngine


import kotlinx.serialization.Serializable
import kotlin.math.abs

private const val MATE_SCORE = 1000000

@Serializable
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
        newSquares[move.to] = if (move.promotion != null) {
            Piece(move.promotion, piece.color)
        } else {
            piece
        }

        //En passant
        if (piece.type == PieceType.PAWN && move.to == enPassantSquare) {
            val capturedPawnSquare = if (piece.color == Colour.WHITE) {
                move.to - 8
            } else {
                move.to + 8
            }
            newSquares[capturedPawnSquare] = null
        }

        //Castling
        if (piece.type == PieceType.KING && abs(move.to - move.from) == 2) {
            val (rookFrom, rookTo) = when (move.to) {
                6 -> Pair(7, 5)
                2 -> Pair(0, 3)
                62 -> Pair(63, 61)
                58 -> Pair(56, 59)
                else -> throw IllegalArgumentException("Invalid Castling Move")
            }
            newSquares[rookTo] = newSquares[rookFrom]
            newSquares[rookFrom] = null
        }

        //applyMove - the en passant check
        val newEnPassantSquare = if (piece.type == PieceType.PAWN && abs(move.to - move.from) == 16) {
            (move.from + move.to) / 2
        } else {
            null
        }
        val newCastlingRights = updateCastlingRights(move, piece)

        val isCapture = squares[move.to] != null
        val newHalfMoveClock = if (isCapture || piece.type == PieceType.PAWN) {
            0
        } else {
            halfMoveClock + 1
        }

        val newFullMoveNumber = if (sideToMove == Colour.BLACK) {
            fullMoveNumber + 1
        } else {
            fullMoveNumber
        }

        return Board(
            squares = newSquares,
            sideToMove = sideToMove.opposite(),
            castlingRights = newCastlingRights,
            enPassantSquare = newEnPassantSquare,
            halfMoveClock = newHalfMoveClock,
            fullMoveNumber = newFullMoveNumber
        )
    }

    //Evaluation function - Represents how good position is for white (+-)
    fun evaluate(): Int {
        var score = 0

        for (square in 0..63) {
            val piece = squares[square] ?: continue
            val value = pieceValue(piece.type)

            score += if (piece.color == Colour.WHITE) value else -value

            //pawn evaluation
            //passed pawn eval
            if (piece.type == PieceType.PAWN) {
                if (isPassedPawn(square, piece.color)) {
                    val bonus = passedPawnBonus(square, piece.color)
                    score += if (piece.color == Colour.WHITE) bonus else -bonus
                }
                //central pawn eval
                val centreBonus = centralPawnBonus(square, piece.color)
                score += if (piece.color == Colour.WHITE) centreBonus else -centreBonus
            }

            //rook evaluation
            if (piece.type == PieceType.ROOK) {
                val bonus = rookFileBonus(square, piece.color)
                score += if (piece.color == Colour.WHITE) bonus else -bonus
            }

            //knight eval
            if (piece.type == PieceType.KNIGHT) {
                val bonus = knightBonus(square, piece.color)
                score += if (piece.color == Colour.WHITE) bonus else -bonus
            }

        }


        return score
    }

    //pawn move generation -
    fun generatePawnMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val direction = if (color == Colour.WHITE) 8 else -8
        val startRank = if (color == Colour.WHITE) 1 else 6
        val forward = from + direction

        val (fromFile, fromRank) = from.toFileRank()

        //Move forward 1 square
        if (forward in 0..63 && squares[forward] == null) {
            if ((color == Colour.WHITE && forward in 56..63) || (color == Colour.BLACK && forward in 0..7)) {
                //promo moves
                moves.add(Move(from, forward, PieceType.KNIGHT))
                moves.add(Move(from, forward, PieceType.BISHOP))
                moves.add(Move(from, forward, PieceType.ROOK))
                moves.add(Move(from, forward, PieceType.QUEEN))
            } else {
                moves.add(Move(from, forward))

                //pawn forward 2 from start
                val doubleForward = from + 2 * direction
                if ((fromRank == startRank) && squares[doubleForward] == null) {
                    moves.add(Move(from, doubleForward))
                }
            }
        }

        //Capture
        val captureOffsets = if (color == Colour.WHITE) listOf(7, 9) else listOf(-7, -9)
        for (offset in captureOffsets) {
            val target = from + offset
            if (target !in 0..63) continue

            val (toFile, _) = target.toFileRank()
            if (abs(toFile - fromFile) != 1) continue

            val piece = squares[target]

            //regular capture
            if (piece != null && piece.color != color) {
                if ((color == Colour.WHITE && target in 56..63) || (color == Colour.BLACK && target in 0..7)) {
                    moves.add(Move(from, target, PieceType.KNIGHT))
                    moves.add(Move(from, target, PieceType.BISHOP))
                    moves.add(Move(from, target, PieceType.ROOK))
                    moves.add(Move(from, target, PieceType.QUEEN))
                } else {
                    moves.add(Move(from, target))
                }
            }

            //En passant capture
            else if (target == enPassantSquare) {
                moves.add(Move(from, target))
            }

        }

        return moves
    }


    //knight moves - L shape
    fun generateKnightMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val knightMoves = listOf(-17, -15, -10, -6, 6, 10, 15, 17)

        val (fromFile, fromRank) = from.toFileRank()

        for (offset in knightMoves) {
            val target = from + offset
            if (target in 0..63) {
                val (toFile, toRank) = target.toFileRank()
                //ensure knight doesnt wrap around board
                val fileDiff = abs(toFile - fromFile)
                val rankDiff = abs(toRank - fromRank)

                if ((fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2)) {
                    val piece = squares[target]
                    if (piece == null) {
                        moves.add(Move(from, target))
                    } else if (piece.color != color) {
                        moves.add(Move(from, target))
                    }

                }

            }
        }


        return moves
    }

    //generate bishop moves - diagonals
    fun generateBishopMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(-9, -7, 7, 9)
        val (fromFile, fromRank) = from.toFileRank()

        for (direction in directions) {
            var target = from + direction
            var currentFile = fromFile
            var currentRank = fromRank

            while (target in 0..63) {
                val (toFile, toRank) = target.toFileRank()
                val fileDiff = abs(toFile - currentFile)
                val rankDiff = abs(toRank - currentRank)
                if (fileDiff != 1 || rankDiff != 1) {
                    break
                }
                val piece = squares[target]
                if (piece == null) {
                    moves.add(Move(from, target))
                } else {
                    if (piece.color != color) {
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

    //generate rook moves - up down left right only
    fun generateRookMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        val directions = listOf(-8, -1, 1, 8)

        for (direction in directions) {
            var target = from + direction
            val isHorizontal = (direction == 1 || direction == -1)

            while (target in 0..63) {
                //Stop wrapping around
                if (isHorizontal && target / 8 != from / 8) break

                val piece = squares[target]
                if (piece == null) {
                    moves.add(Move(from, target))
                } else {
                    if (piece.color != color) {
                        moves.add(Move(from, target))
                    }
                    break
                }
                target += direction
            }
        }
        return moves
    }

    //Queen move generation - combine bishop and rook movement
    fun generateQueenMoves(from: Square, color: Colour): List<Move> {
        val moves = mutableListOf<Move>()
        moves.addAll(generateBishopMoves(from, color))
        moves.addAll(generateRookMoves(from, color))
        return moves
    }

    //king moves generation - with/without castling
    fun generateKingMoves(from: Square, color: Colour, includeCastling: Boolean = true): List<Move> {
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

        //Skip castling if not needed
        if (!includeCastling) return moves


        //Castling
        //White castle
        if (color == Colour.WHITE && from == 4) {
            //kingside
            if (castlingRights.whiteKingSide && squares[5] == null && squares[6] == null &&
                !isSquareAttacked(4, Colour.BLACK) &&
                !isSquareAttacked(5, Colour.BLACK) &&
                !isSquareAttacked(6, Colour.BLACK)
            ) {
                moves.add(Move(4, 6))
            }
            //queenside
            if (castlingRights.whiteQueenSide && squares[1] == null && squares[2] == null && squares[3] == null &&
                !isSquareAttacked(4, Colour.BLACK) &&
                !isSquareAttacked(3, Colour.BLACK) &&
                !isSquareAttacked(2, Colour.BLACK)
            ) {
                moves.add(Move(4, 2))
            }
        }

        //Black castling
        if (color == Colour.BLACK && from == 60) {
            //kingside
            if (castlingRights.blackKingSide && squares[61] == null && squares[62] == null &&
                !isSquareAttacked(60, Colour.WHITE) &&
                !isSquareAttacked(61, Colour.WHITE) &&
                !isSquareAttacked(62, Colour.WHITE)
            ) {
                moves.add(Move(60, 62))
            }
            //queenside
            if (castlingRights.blackQueenSide && squares[57] == null && squares[58] == null && squares[59] == null &&
                !isSquareAttacked(60, Colour.WHITE) &&
                !isSquareAttacked(59, Colour.WHITE) &&
                !isSquareAttacked(58, Colour.WHITE)
            ) {
                moves.add(Move(60, 58))
            }
        }

        return moves
    }

    //castling rights check - if king/rook moved castling disabled
    private fun updateCastlingRights(move: Move, piece: Piece): CastlingRights {
        var rights = castlingRights.copy()

        //king moves - lose castling rights
        if (piece.type == PieceType.KING) {
            if (piece.color == Colour.WHITE) {
                rights = rights.copy(whiteKingSide = false, whiteQueenSide = false)
            } else {
                rights = rights.copy(blackKingSide = false, blackQueenSide = false)
            }
        }

        //rook moves - lose castling rights
        if (piece.type == PieceType.ROOK) {
            when (move.from) {
                0 -> rights = rights.copy(whiteQueenSide = false)  // a1
                7 -> rights = rights.copy(whiteKingSide = false)   // h1
                56 -> rights = rights.copy(blackQueenSide = false) // a8
                63 -> rights = rights.copy(blackKingSide = false)  // h8
            }
        }

        //rook captured - lose castling rights
        when (move.to) {
            0 -> rights = rights.copy(whiteQueenSide = false)
            7 -> rights = rights.copy(whiteKingSide = false)
            56 -> rights = rights.copy(blackQueenSide = false)
            63 -> rights = rights.copy(blackKingSide = false)
        }

        return rights
    }

    //Generate all moves
    fun generateAllMoves(): List<Move> {
        val moves = mutableListOf<Move>()
        for (square in 0..63) {
            val piece = squares[square] ?: continue
            if (piece.color != sideToMove) continue

            when (piece.type) {
                PieceType.PAWN -> moves.addAll(generatePawnMoves(square, piece.color))
                PieceType.KNIGHT -> moves.addAll(generateKnightMoves(square, piece.color))
                PieceType.BISHOP -> moves.addAll(generateBishopMoves(square, piece.color))
                PieceType.ROOK -> moves.addAll(generateRookMoves(square, piece.color))
                PieceType.QUEEN -> moves.addAll(generateQueenMoves(square, piece.color))
                PieceType.KING -> moves.addAll(generateKingMoves(square, piece.color))

            }
        }

        return moves
    }

    //generate all legal moves
    fun generateLegalMoves(): List<Move> {
        val legalMoves = generateAllMoves()
        return legalMoves.filter { move ->
            val newBoard = applyMove(move)
            !newBoard.isInCheck(sideToMove)
        }
    }

    /**
     * Used to speed up minimaxAlg by prioritising the searching of moves that are likely to be good
     */
    fun orderMoves(moves: List<Move>, tt: HashMap<String, TranspositionTableEntry>): List<Move> {
        val ttBestMove = tt[fenKey(toFen())]?.bestMove
        return moves.sortedByDescending { move ->
            var priority = 0
            if (move == ttBestMove) {
                priority = 100000
            }
            val moveTarget: PieceType? = squares[move.to]?.type
            val pieceMoved: PieceType = squares[move.from]!!.type
            if (moveTarget != null) {
                // prioritise captures of valuable pieces, especially if they're captured by a low-value piece
                priority += 2 * pieceValue(moveTarget) - pieceValue(pieceMoved)
            }
            if (move.promotion != null) {
                // prioritise promotions in general, with more priority to more valuable promotions
                priority += pieceValue(move.promotion)
            }

            priority
        }
    }

    //check if given colors king is in check
    fun isInCheck(color: Colour): Boolean {
        val kingSquare = squares.indexOfFirst {
            it?.type == PieceType.KING && it.color == color
        }
        if (kingSquare == -1) return false
        return isSquareAttacked(kingSquare, color.opposite())
    }


    //check attack coverage by iterating all enemy pieces and testing their move generation
    fun isSquareAttacked(square: Square, byColour: Colour): Boolean {
        for (sqr in 0..63) {
            val piece = squares[sqr] ?: continue
            if (piece.color != byColour) continue //skip if not attacking colour
            //pawn check
            when (piece.type) {
                PieceType.PAWN -> {
                    //pawns only attack diagonally
                    val direction = if (piece.color == Colour.WHITE) 8 else -8
                    val (pawnFile, _) = sqr.toFileRank()
                    val (targetFile, _) = square.toFileRank()
                    val captureSquares = listOf(sqr + direction - 1, sqr + direction + 1)
                    if (square in captureSquares && abs(targetFile - pawnFile) == 1) {
                        return true
                    }
                }

                //check all other pieces
                PieceType.KNIGHT -> {
                    if (generateKnightMoves(sqr, piece.color).any { it.to == square }) return true
                }

                PieceType.BISHOP -> {
                    if (generateBishopMoves(sqr, piece.color).any { it.to == square }) return true
                }

                PieceType.ROOK -> {
                    if (generateRookMoves(sqr, piece.color).any { it.to == square }) return true
                }

                PieceType.QUEEN -> {
                    if (generateQueenMoves(sqr, piece.color).any { it.to == square }) return true
                }

                PieceType.KING -> {
                    if (generateKingMoves(
                            sqr,
                            piece.color,
                            includeCastling = false
                        ).any { it.to == square }
                    ) return true
                }
            }
        }
        return false
    }

    //Check for passed pawn space - if there is a passed pawn returns true
    //A pawn is passed if no enemy pawn can block or capture on way to promotion
    fun isPassedPawn(square: Int, color: Colour): Boolean {
        val file = square % 8
        val rank = square / 8
        val enemy = color.opposite()

        for (f in maxOf(0, file - 1)..minOf(7, file + 1)) {
            for (r in 0..7) {
                if (color == Colour.WHITE && r <= rank) continue
                if (color == Colour.BLACK && r >= rank) continue

                val sq = r * 8 + f
                val p = squares[sq] ?: continue
                if (p.type == PieceType.PAWN && p.color == enemy) return false
            }
        }
        return true
    }

    fun passedPawnBonus(square: Int, color: Colour): Int {
        val rank = square / 8
        val advance = if (color == Colour.WHITE) rank else 7 - rank
        if (advance <= 1) return 0 //no bonus for pawns that have not advanced
        return 20 + advance * 15
    }


    //rook open file helper 1 (checks there are no pawns anywhere in the file)
    fun isOpenFile(file: Int): Boolean {
        for (r in 0..7) {
            val sq = r * 8 + file
            val p = squares[sq] ?: continue
            if (p.type == PieceType.PAWN) return false
        }
        return true
    }

    //rook open file helper 2 (checks there are no pawns of same colour)
    fun isSemiOpenFile(file: Int, color: Colour): Boolean {
        for (r in 0..7) {
            val sq = r * 8 + file
            val p = squares[sq] ?: continue
            if (p.type == PieceType.PAWN && p.color == color) return false
        }
        return true
    }

    //bonus for rooks in open/semi open files
    fun rookFileBonus(square: Int, color: Colour): Int {
        val file = square % 8
        val rank = square / 8
        var bonus = 0
        if (isOpenFile(file)) bonus += 50
        else if (isSemiOpenFile(file, color)) bonus += 25

        //7th rank bonus
        if (color == Colour.WHITE && rank == 6) bonus += 30
        if (color == Colour.BLACK && rank == 1) bonus += 30

        return bonus
    }

    //pawns in centre of board bonus
    fun centralPawnBonus(square: Int, color: Colour): Int {
        return if (color == Colour.WHITE) PAWN_PST[square]
        else PAWN_PST[mirrorSquare(square)]
    }

    //knights in centre of board bonus
    fun knightBonus(square: Int, color: Colour): Int {
        return if (color == Colour.WHITE) KNIGHT_PST[square]
        else KNIGHT_PST[mirrorSquare(square)]
    }


    fun findBestMoves(depth: Int): Move? {
        val legalMoves = generateLegalMoves()
        if (legalMoves.isEmpty()) return null

        //Check the bookMoves first and ensure that they're legal (just in case)
        val currentFen = fenKey(toFen())
        val bookCandidates = bookMoves[currentFen]
            ?.filter { it in legalMoves }
            ?.takeIf { it.isNotEmpty() }
        if (bookCandidates != null) return bookCandidates.random()


        val tt = HashMap<String, TranspositionTableEntry>()
        // "tt" meaning Transposition Table. Basically, caching results of engine search to improve performance
        val bestMoves = mutableListOf<Move>()//new list to allow for random move choice
        //create best score based on white or black playing (max vs min)
        var bestScore: Int

        // iterative deepening. Doing lower-depth searches before the search at the depth we actually want.
        // Counter-intuitively, this improves performance over just doing a single search
        // because we get our Transposition Table filled up, which helps pruning the search at the full depth
        for (currentDepth in 1..depth) {
            bestMoves.clear() // We only actually want the best moves from the deepest search (last iteration of this loop)
            bestScore = if (sideToMove == Colour.WHITE) Int.MIN_VALUE else Int.MAX_VALUE
            for (move in orderMoves(legalMoves, tt)) {
                val newBoard = applyMove(move)

                val castlingBonus = run {
                    val piece = squares[move.from]
                    if (piece?.type == PieceType.KING &&
                        ((move.from == 4 && (move.to == 6 || move.to == 2)) ||
                                (move.from == 60 && (move.to == 62 || move.to == 58)))
                    ) 50 else 0
                    /** don't need to explicitly check sideToMove yet,
                     * since a king on square 60 moving to 62 or 58 MUST be a black king castling.
                     * We'll check for sideToMove before incrementing or decrementing score.
                     * We're giving a half pawn (50 centipawn) bonus for a move that castles to encourage castling.
                     */
                }
                val score = newBoard.minimaxAlg(currentDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, tt) +
                        if (sideToMove == Colour.WHITE) castlingBonus else -castlingBonus
                //println("Move $move -> score $score")
                if (sideToMove == Colour.WHITE) {
                    if (score > bestScore) { //White check for best move
                        bestScore = score
                        bestMoves.clear()
                        bestMoves.add(move)
                    } else if (score == bestScore) {
                        bestMoves.add(move)
                    }

                } else {
                    if (score < bestScore) { //Black check for best move
                        bestScore = score
                        bestMoves.clear()
                        bestMoves.add(move)
                    } else if (score == bestScore) {
                        bestMoves.add(move)
                    }
                }
            }
        }

        return bestMoves.randomOrNull()
    }


    //minimax algorithm - tried to add alpha beta pruning
    //Depth - how many moves to look ahead, Alpha - best score white, Beta - best score black
    private fun minimaxAlg(depth: Int, alpha: Int, beta: Int, tt: HashMap<String, TranspositionTableEntry>): Int {
        val ttKey = fenKey(toFen())
        tt[ttKey]?.let { entry ->
            if (entry.depth >= depth) {
                when (entry.flag) {
                    TTFlag.EXACT -> return entry.score
                    TTFlag.LOWER_BOUND -> if (entry.score >= beta) return entry.score
                    TTFlag.UPPER_BOUND -> if (entry.score <= alpha) return entry.score
                }
            }
        }
        val state = getBoardState()
        if (state != BoardState.IN_PROGRESS) {
            return when (state) {
                BoardState.STALEMATE -> 0 //Draw check
                //Checkmate check  - large win/loss to be clear
                BoardState.CHECKMATE -> {
                    if (sideToMove == Colour.WHITE) {
                        -MATE_SCORE - depth //white in mate
                    } else {
                        MATE_SCORE + depth//black in mate
                    }
                }
            }
        }


        if (depth == 0) {
            val eval = evaluate()
            tt[ttKey] = TranspositionTableEntry(eval, 0, TTFlag.EXACT)
            return eval
        }

        val legalMoves = orderMoves(generateLegalMoves(), tt)
        var currentAlpha = alpha
        var currentBeta = beta
        val originalAlpha = alpha
        val bestScore: Int
        var bestMove: Move? = null

        //Maximise for white side
        if (sideToMove == Colour.WHITE) {
            var maxScore = Int.MIN_VALUE//start with worst possible score
            for (move in legalMoves) {
                val newBoard = applyMove(move)
                val score = newBoard.minimaxAlg(
                    depth - 1,
                    currentAlpha,
                    currentBeta,
                    tt
                ) //recursively get score from opponents perspective
                if (score > maxScore) {
                    maxScore = score
                    bestMove = move
                }
                currentAlpha = maxOf(currentAlpha, score)

                //alpha beta pruning
                if (currentBeta <= currentAlpha) {
                    break
                }
            }

            bestScore = maxScore

        } else {
            //Black turn
            var minScore = Int.MAX_VALUE
            for (move in legalMoves) {
                val newBoard = applyMove(move)
                val score = newBoard.minimaxAlg(depth - 1, currentAlpha, currentBeta, tt)

                if (score < minScore) {
                    minScore = score
                    bestMove = move
                }
                currentBeta = minOf(currentBeta, score)

                //alpha-beta pruning
                if (currentBeta <= currentAlpha) {
                    break
                }
            }
            bestScore = minScore
        }

        val ttFlag = when {
            bestScore <= originalAlpha -> TTFlag.UPPER_BOUND
            bestScore >= beta -> TTFlag.LOWER_BOUND
            else -> TTFlag.EXACT
        }
        tt[ttKey] = TranspositionTableEntry(bestScore, depth, ttFlag, bestMove)

        return bestScore
    }


    //Check game state - return checkmate/stalemate if no legal moves
    fun getBoardState(): BoardState {
        val legalMoves = generateLegalMoves()

        if (legalMoves.isNotEmpty()) {
            return BoardState.IN_PROGRESS
        }

        return if (isInCheck(sideToMove)) {
            BoardState.CHECKMATE
        } else {
            BoardState.STALEMATE
        }
    }


    companion object {

        //central pawns rewarded
        private val PAWN_PST = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0,
            5, 5, 5, 10, 10, 5, 5, 5,
            5, 10, 15, 35, 35, 15, 10, 5,
            10, 15, 20, 40, 40, 20, 15, 10,
            15, 20, 25, 40, 40, 25, 20, 10,
            20, 25, 30, 45, 45, 30, 25, 20,
            50, 50, 50, 50, 50, 50, 50, 50,
            0, 0, 0, 0, 0, 0, 0, 0
        )


        //knights on edge of board penalised
        private val KNIGHT_PST = intArrayOf(
            -50, -40, -30, -30, -30, -30, -40, -50,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -30, 5, 10, 15, 15, 10, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30,
            -30, 5, 15, 20, 20, 15, 5, -30,
            -30, 0, 10, 15, 15, 10, 0, -30,
            -40, -20, 0, 5, 5, 0, -20, -40,
            -50, -40, -30, -30, -30, -30, -40, -50
        )

        fun mirrorSquare(square: Int): Int = (7 - square / 8) * 8 + (square % 8)

        //book moves. To maintain variety of openings when playing against the bot
        val bookMoves: Map<String, List<Move>> = mapOf(
            //For the starting position, play a mainline move: e4, d4, c4, or Nf3
            "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq -" to listOf(
                Move("e2".toSquare()!!, "e4".toSquare()!!),
                Move("d2".toSquare()!!, "d4".toSquare()!!),
                Move("c2".toSquare()!!, "c4".toSquare()!!),
                Move("g1".toSquare()!!, "f3".toSquare()!!),
            ),
            //Reply to 1. e4 with a mainline move: e5, e6, c5, or c6
            "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3" to listOf(
                Move("e7".toSquare()!!, "e5".toSquare()!!),
                Move("e7".toSquare()!!, "e6".toSquare()!!),
                Move("c7".toSquare()!!, "c5".toSquare()!!),
                Move("c7".toSquare()!!, "c6".toSquare()!!),
            ),
            //Reply to 1. d4 with a mainline or move that transposes to a mainline: d5, Nf6, c6, or e6
            "rnbqkbnr/pppppppp/8/8/3P4/8/PPP1PPPP/RNBQKBNR b KQkq d3" to listOf(
                Move("d7".toSquare()!!, "d5".toSquare()!!),
                Move("g8".toSquare()!!, "f6".toSquare()!!),
                Move("c7".toSquare()!!, "c6".toSquare()!!),
                Move("e7".toSquare()!!, "e6".toSquare()!!),
            ),
            //Reply to 1. c4 with a mainline or move that transposes to a mainline: c5, Nf6, e6, e5
            "rnbqkbnr/pppppppp/8/8/2P5/8/PP1PPPPP/RNBQKBNR b KQkq c3" to listOf(
                Move("c7".toSquare()!!, "c5".toSquare()!!),
                Move("g8".toSquare()!!, "f6".toSquare()!!),
                Move("e7".toSquare()!!, "e6".toSquare()!!),
                Move("e7".toSquare()!!, "e5".toSquare()!!),
            ),
            //Reply to 1. Nf3 with a mainline or move that transposes to a mainline: d5, Nf6, c5, or g6
            "rnbqkbnr/pppppppp/8/8/8/5N2/PPPPPPPP/RNBQKB1R b KQkq -" to listOf(
                Move("d7".toSquare()!!, "d5".toSquare()!!),
                Move("g8".toSquare()!!, "f6".toSquare()!!),
                Move("c7".toSquare()!!, "c5".toSquare()!!),
                Move("g7".toSquare()!!, "g6".toSquare()!!),
            ),
            //Reply to 1. e4 e5 with one of 5 common moves: Nf3, d4, Bc4, Nc3, and f4
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq e6" to listOf(
                Move("g1".toSquare()!!, "f3".toSquare()!!),
                Move("d2".toSquare()!!, "d4".toSquare()!!),
                Move("f1".toSquare()!!, "c4".toSquare()!!),
                Move("b1".toSquare()!!, "c3".toSquare()!!),
                Move("f2".toSquare()!!, "f4".toSquare()!!),
            ),
            //Reply to 1. d4 d5 with one of 4 common moves: c4, Nf3, Bf4, g3
            "rnbqkbnr/ppp1pppp/8/3p4/3P4/8/PPP1PPPP/RNBQKBNR w KQkq d6" to listOf(
                Move("c2".toSquare()!!, "c4".toSquare()!!),
                Move("g1".toSquare()!!, "f3".toSquare()!!),
                Move("g2".toSquare()!!, "g3".toSquare()!!),
                Move("c1".toSquare()!!, "f4".toSquare()!!),
            ),
            //Reply to 1. e4 c5 with one of 3 common moves: Nf3, d4, Nc3
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/8/PPPP1PPP/RNBQKBNR w KQkq c6" to listOf(
                Move("g1".toSquare()!!, "f3".toSquare()!!),
                Move("d2".toSquare()!!, "d4".toSquare()!!),
                Move("b1".toSquare()!!, "c3".toSquare()!!),
            ),
            //Reply to 1. d4 Nf6 with one of 4 common moves: c4, Nf3, Bf4, g3
            "rnbqkb1r/pppppppp/5n2/8/3P4/8/PPP1PPPP/RNBQKBNR w KQkq -" to listOf(
                Move("c2".toSquare()!!, "c4".toSquare()!!),
                Move("g1".toSquare()!!, "f3".toSquare()!!),
                Move("g2".toSquare()!!, "g3".toSquare()!!),
                Move("c1".toSquare()!!, "f4".toSquare()!!),
            ),
            //Reply to 1. e4 c6 with either d4 or Nf3
            "rnbqkbnr/pp1ppppp/2p5/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq -" to listOf(
                Move("g1".toSquare()!!, "f3".toSquare()!!),
                Move("d2".toSquare()!!, "d4".toSquare()!!),
            ),
            //Reply to 1. e4 e6 with either d4 or Nf3
            "rnbqkbnr/pppp1ppp/4p3/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq -" to listOf(
                Move("g1".toSquare()!!, "f3".toSquare()!!),
                Move("d2".toSquare()!!, "d4".toSquare()!!),
            ),
            //Reply to 1. e4 c5 2. Nf3 with either Nc6, d6, e6, or g6
            "rnbqkbnr/pp1ppppp/8/2p5/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq -" to listOf(
                Move("b8".toSquare()!!, "c6".toSquare()!!),
                Move("d7".toSquare()!!, "d6".toSquare()!!),
                Move("e7".toSquare()!!, "e6".toSquare()!!),
                Move("g7".toSquare()!!, "g6".toSquare()!!),
            ),
            //Reply to 1. e4 e5 2. Nf3 with either Nc6, Nf6, or d6
            "rnbqkbnr/pppp1ppp/8/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R b KQkq -" to listOf(
                Move("b8".toSquare()!!, "c6".toSquare()!!),
                Move("g8".toSquare()!!, "f6".toSquare()!!),
                Move("d7".toSquare()!!, "d6".toSquare()!!),
            ),
            //Reply to 1. e4 e6 2. d4 with d5
            "rnbqkbnr/pppp1ppp/4p3/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq d3" to listOf(
                Move("d7".toSquare()!!, "d5".toSquare()!!),
            ),
            //Reply to 1. e4 c6 2. d4 with d5
            "rnbqkbnr/pp1ppppp/2p5/8/3PP3/8/PPP2PPP/RNBQKBNR b KQkq d3" to listOf(
                Move("d7".toSquare()!!, "d5".toSquare()!!),
            ),
        )

        //strip the fen down to the key parts
        fun fenKey(fen: String): String = fen.split(" ").take(4).joinToString(" ")

        //create starting board position
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

        fun pieceValue(type: PieceType) = when (type) {
            PieceType.PAWN -> 100
            PieceType.KNIGHT -> 315
            PieceType.BISHOP -> 330
            PieceType.ROOK -> 500
            PieceType.QUEEN -> 900
            PieceType.KING -> 20000
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


    fun toFen(): String {
        val parts = mutableListOf<String>()

        val rows = mutableListOf<String>()
        for (rank in 7 downTo 0) {
            var emptyCount = 0
            val row = StringBuilder()

            for (file in 0..7) {
                val square = Pair(file, rank).toSquare()
                val piece = squares[square]

                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        row.append(emptyCount)
                        emptyCount = 0
                    }
                    val symbol = when (piece.type) {
                        PieceType.PAWN -> if (piece.color == Colour.WHITE) 'P' else 'p'
                        PieceType.KNIGHT -> if (piece.color == Colour.WHITE) 'N' else 'n'
                        PieceType.BISHOP -> if (piece.color == Colour.WHITE) 'B' else 'b'
                        PieceType.ROOK -> if (piece.color == Colour.WHITE) 'R' else 'r'
                        PieceType.QUEEN -> if (piece.color == Colour.WHITE) 'Q' else 'q'
                        PieceType.KING -> if (piece.color == Colour.WHITE) 'K' else 'k'
                    }
                    row.append(symbol)
                }
            }
            if (emptyCount > 0) {
                row.append(emptyCount)
            }
            rows.add(row.toString())
        }
        parts.add(rows.joinToString("/"))

        parts.add(if (sideToMove == Colour.WHITE) "w" else "b")

        val castling = buildString {
            if (castlingRights.whiteKingSide) append('K')
            if (castlingRights.whiteQueenSide) append('Q')
            if (castlingRights.blackKingSide) append('k')
            if (castlingRights.blackQueenSide) append('q')
        }.ifEmpty { "-" }
        parts.add(castling)

        val epSquare = enPassantSquare?.let {
            val (file, rank) = it.toFileRank()
            "${('a' + file)}${rank + 1}"
        } ?: "-"
        parts.add(epSquare)

        parts.add(halfMoveClock.toString())

        parts.add(fullMoveNumber.toString())

        return parts.joinToString(" ")
    }
}






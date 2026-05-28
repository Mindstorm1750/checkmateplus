package org.checkmateplus.voicecommandservice.service

import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.chessEngine.PieceType
import org.checkmateplus.chessLibrary.chessEngine.toAlgebraic
import org.checkmateplus.chessLibrary.chessEngine.toSquare
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale

/**
 * Service for parsing and validating voice commands into legal chess moves.
 */
@Service
class MoveValidationService(
    private val openAiService: OpenAiService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private fun normalizeTranscript(text: String): String {
        return text.lowercase(Locale.ENGLISH)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Parse transcribed text into a move.
     * Examples:
     * - "e2 to e4"
     * - "knight to f3"
     * - "pawn e2 e4"
     * - "queen takes d5"
     */
    fun parseVoiceMove(text: String): VoiceMoveIntent? {
        val cleaned = normalizeTranscript(text)

        val squareMatches = Regex("\\b([a-h][1-8])\\b")
            .findAll(cleaned)
            .map { it.groupValues[1] }
            .toList()

        val pieceMatch = Regex("\\b(pawn|knight|bishop|rook|queen|king)\\b")
            .find(cleaned)
            ?.groupValues
            ?.get(1)

        val promotionMatch = Regex("\\b(promote|promotion|equals)\\s+(queen|rook|bishop|knight)\\b")
            .find(cleaned)
            ?.groupValues
            ?.get(2)

        return when {
            squareMatches.size >= 2 -> VoiceMoveIntent(
                from = squareMatches[0],
                to = squareMatches[1],
                piece = pieceMatch,
                promotion = promotionMatch
            )

            squareMatches.size == 1 -> VoiceMoveIntent(
                to = squareMatches[0],
                piece = pieceMatch,
                promotion = promotionMatch
            )

            pieceMatch != null -> VoiceMoveIntent(
                piece = pieceMatch,
                promotion = promotionMatch
            )

            else -> null
        }
    }

    /**
     * Validate the parsed move against legal moves for the current board.
     *
     * 1. If both from and to are present, try exact matching first.
     * 2. Otherwise narrow legal moves using the following mechanisms:
     *    - destination square
     *    - piece type
     *    - promotion
     * 3. If exactly one option, accept it.
     * 4. If ambiguous or unresolved, fallback to querying OpenAI to disambiguate the correct move (clunky but kinda works).
     */
    fun validateMove(
        parsedMoveText: String,
        parsedMove: VoiceMoveIntent,
        legalMoves: List<Move>,
        board: Board
    ): ValidationResult {
        val fromSquare = parsedMove.from?.toSquare()
        val toSquare = parsedMove.to?.toSquare()
        val requestedPieceType = parsedMove.piece?.toPieceTypeOrNull()
        val requestedPromotion = parsedMove.promotion?.toPieceTypeOrNull()

        if (parsedMove.from != null && fromSquare == null) {
            logger.debug("Parsed source square '{}' could not be converted", parsedMove.from)
        }
        if (parsedMove.to != null && toSquare == null) {
            logger.debug("Parsed destination square '{}' could not be converted", parsedMove.to)
        }

        if (fromSquare != null && toSquare != null) {
            val exactMatch = legalMoves.find { move ->
                move.from == fromSquare &&
                        move.to == toSquare &&
                        (requestedPromotion == null || move.promotion == requestedPromotion)
            }

            if (exactMatch != null) {
                return ValidationResult.Valid(exactMatch)
            }
        }

        var candidates = legalMoves

        if (fromSquare != null) {
            candidates = candidates.filter { it.from == fromSquare }
        }

        if (toSquare != null) {
            candidates = candidates.filter { it.to == toSquare }
        }

        if (requestedPieceType != null) {
            candidates = candidates.filter { move ->
                board.squares[move.from]?.type == requestedPieceType
            }
        }

        if (requestedPromotion != null) {
            candidates = candidates.filter { move ->
                move.promotion == requestedPromotion
            }
        }

        if (candidates.size == 1) {
            return ValidationResult.Valid(candidates.first())
        }

        if (candidates.isNotEmpty()) {
            val narrowedCandidateStrings = candidates.map { "${it.from.toAlgebraic()}-${it.to.toAlgebraic()}" }
            val fallback = openAiService.disambiguateMove(parsedMoveText, narrowedCandidateStrings)

            if (fallback != null && fallback.from != null && fallback.to != null && fallback.confidence >= 0.6) {
                val fallbackFrom = fallback.from.toSquare()
                val fallbackTo = fallback.to.toSquare()
                val fallbackMove = candidates.find { it.from == fallbackFrom && it.to == fallbackTo }

                if (fallbackMove != null) {
                    logger.info(
                        "OpenAI fallback matched ambiguous voice move '{}' to {} -> {}",
                        parsedMoveText,
                        fallback.from,
                        fallback.to
                    )
                    return ValidationResult.Valid(fallbackMove)
                }
            }

            return ValidationResult.Invalid(
                "Move is ambiguous for current position: $parsedMove"
            )
        }

        val allCandidateStrings = legalMoves.map { "${it.from.toAlgebraic()}-${it.to.toAlgebraic()}" }
        val fallback = openAiService.disambiguateMove(parsedMoveText, allCandidateStrings)

        if (fallback != null && fallback.from != null && fallback.to != null && fallback.confidence >= 0.6) {
            val fallbackFrom = fallback.from.toSquare()
            val fallbackTo = fallback.to.toSquare()
            val fallbackMove = legalMoves.find { it.from == fallbackFrom && it.to == fallbackTo }

            if (fallbackMove != null) {
                logger.info(
                    "OpenAI fallback matched voice move '{}' to {} -> {}",
                    parsedMoveText,
                    fallback.from,
                    fallback.to
                )
                return ValidationResult.Valid(fallbackMove)
            }
        }

        return when {
            parsedMove.to != null && toSquare == null ->
                ValidationResult.Invalid("Could not parse destination square: ${parsedMove.to}")

            parsedMove.from != null && parsedMove.to != null ->
                ValidationResult.Invalid("Move ${parsedMove.from} to ${parsedMove.to} is not legal in this position")

            parsedMove.piece != null && parsedMove.to != null ->
                ValidationResult.Invalid("No legal ${parsedMove.piece} move to ${parsedMove.to}")

            else ->
                ValidationResult.Invalid("Could not resolve move from parsed input: $parsedMove")
        }
    }

    private fun String.toPieceTypeOrNull(): PieceType? {
        return when (this.lowercase(Locale.ENGLISH)) {
            "pawn" -> PieceType.PAWN
            "knight" -> PieceType.KNIGHT
            "bishop" -> PieceType.BISHOP
            "rook" -> PieceType.ROOK
            "queen" -> PieceType.QUEEN
            "king" -> PieceType.KING
            else -> null
        }
    }

    data class VoiceMoveIntent(
        val from: String? = null,
        val to: String? = null,
        val piece: String? = null,
        val promotion: String? = null
    )

    sealed class ValidationResult {
        data class Valid(val move: Move) : ValidationResult()
        data class Invalid(val reason: String) : ValidationResult()
    }
}
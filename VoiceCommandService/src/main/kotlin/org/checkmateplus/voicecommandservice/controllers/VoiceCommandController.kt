package org.checkmateplus.voicecommandservice.controllers

import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.toAlgebraic
import org.checkmateplus.voicecommandservice.service.OpenAiService
import org.checkmateplus.voicecommandservice.service.MoveValidationService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/api/voice")
class VoiceCommandController(
    private val openAiService: OpenAiService,
    private val moveValidationService: MoveValidationService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping(
        "/move",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun voiceMove(
        @RequestPart("audio") audio: MultipartFile,
        @RequestPart("matchId") matchId: String,
        @RequestPart("board") boardJson: String,
    ): VoiceMoveResponse {
        return try {
            val board = Json.decodeFromString<Board>(boardJson) //deserialise the board from sent from the frontend

            // Step 1: Transcribe audio to text
            val transcript = openAiService.transcribe(audio)
            logger.info("Transcribed: '$transcript' for match $matchId")

            // Step 2: Get legal moves for the board
            val legalMoves = board.generateLegalMoves()
            if (legalMoves.isEmpty()) {
                logger.warn("No legal moves available for match $matchId")
                return VoiceMoveResponse(
                    matchId = matchId,
                    transcript = transcript,
                    from = null,
                    to = null,
                    promotion = null,
                    error = "No legal moves available"
                )
            }

            // Step 3: Parse the transcribed text into a move
            val parsedMove = moveValidationService.parseVoiceMove(transcript)
            if (parsedMove == null) {
                logger.warn("Could not parse move from transcript: '$transcript'")
                return VoiceMoveResponse(
                    matchId = matchId,
                    transcript = transcript,
                    from = null,
                    to = null,
                    promotion = null,
                    error = "Could not understand move. Try 'e2 to e4' or 'knight to f3'"
                )
            }

            // Step 5: Validate the parsed move against legal moves
            val validationResult = moveValidationService.validateMove(transcript, parsedMove, legalMoves, board)

            return when (validationResult) {
                is MoveValidationService.ValidationResult.Valid -> {
                    val move = validationResult.move
                    logger.info("Valid move parsed: $move for match $matchId")
                    VoiceMoveResponse(
                        matchId = matchId,
                        transcript = transcript,
                        from = move.from.toAlgebraic(),
                        to = move.to.toAlgebraic(),
                        promotion = move.promotion?.name,
                        error = null
                    )
                }
                is MoveValidationService.ValidationResult.Invalid -> {
                    logger.warn("Invalid move: ${validationResult.reason}")
                    VoiceMoveResponse(
                        matchId = matchId,
                        transcript = transcript,
                        from = null,
                        to = null,
                        promotion = null,
                        error = validationResult.reason
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing voice move", e)
            VoiceMoveResponse(
                matchId = matchId,
                transcript = null,
                from = null,
                to = null,
                promotion = null,
                error = "Server error: ${e.message}"
            )
        }
    }
}

data class VoiceMoveResponse(
    val matchId: String,
    val transcript: String?,
    val from: String?,
    val to: String?,
    val promotion: String?,
    val error: String?
)

data class LegalMovesDebugResponse(
    val matchId: String,
    val legalMoves: List<String>,
    val error: String?
)
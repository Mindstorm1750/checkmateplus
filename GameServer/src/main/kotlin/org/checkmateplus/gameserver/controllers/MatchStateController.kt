package org.checkmateplus.gameserver.controllers

import org.checkmateplus.gameserver.service.MatchService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/matches")
class MatchStateController(
    private val matchService: MatchService
) {

    @GetMapping("/{matchId}/board")
    @ResponseStatus(HttpStatus.OK)
    fun getBoardState(@PathVariable matchId: String): BoardStateDto {
        val match = matchService.getMatch(matchId)
        return if (match?.board != null) {
            BoardStateDto(
                matchId = matchId,
                fen = match.board!!.toFen()
            )
        } else {
            throw IllegalArgumentException("Match not found: $matchId")
        }
    }

    @GetMapping("/{matchId}/legal-moves")
    @ResponseStatus(HttpStatus.OK)
    fun getLegalMoves(@PathVariable matchId: String): LegalMovesDto {
        val match = matchService.getMatch(matchId)
        if (match?.board == null) {
            throw IllegalArgumentException("Match not found: $matchId")
        }

        val legalMoves = match.board!!.generateLegalMoves().map { move ->
            "${move.from} to ${move.to}"
        }

        return LegalMovesDto(
            matchId = matchId,
            moves = legalMoves
        )
    }
}

data class BoardStateDto(
    val matchId: String,
    val fen: String
)

data class LegalMovesDto(
    val matchId: String,
    val moves: List<String>
)
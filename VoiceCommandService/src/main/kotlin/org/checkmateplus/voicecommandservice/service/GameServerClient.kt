package org.checkmateplus.voicecommandservice.service

import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.FenToBoard
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.beans.factory.annotation.Value

/**
 * Client for communicating with GameServer to fetch match/board state with basic HTTP REST calls
 */
@Service
class GameServerClient(
    private val restTemplate: RestTemplate
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${gameserver.url:http://localhost:8080}")
    private lateinit var gameServerUrl: String

    /**
     * Fetch the current board state for a match.
     * Returns null if match not found or error occurs.
     */
    fun getBoardState(matchId: String): Board? {
        return try {
            val response = restTemplate.getForObject(
                "$gameServerUrl/api/matches/$matchId/board",
                BoardStateResponse::class.java
            )

            if (response?.fen != null) {
                FenToBoard(response.fen)
            } else {
                logger.warn("No FEN returned for match $matchId")
                null
            }
        } catch (e: RestClientException) {
            logger.error("Failed to fetch board state for match $matchId", e)
            null
        }
    }

    // Response DTOs
    data class BoardStateResponse(
        val matchId: String,
        val fen: String
    )

    data class LegalMovesResponse(
        val matchId: String,
        val moves: List<String>
    )
}
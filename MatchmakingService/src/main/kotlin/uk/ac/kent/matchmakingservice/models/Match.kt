package uk.ac.kent.matchmakingservice.models

import kotlinx.serialization.Serializable
import uk.ac.kent.chessLibrary.playerModels.Player
import uk.ac.kent.chessLibrary.playerModels.MatchStatus
import uk.ac.kent.chessLibrary.playerModels.TimeControl
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a matched set of players ready to start a game
 * Generic design supports future game variants (e.g., 4-player chess, teams)
 * Color/side assignment is handled by the game server
 */
@Serializable
@OptIn(ExperimentalTime::class)
data class Match(
    val matchId: String = UUID.randomUUID().toString(),
    val players: List<Player>,
    val timeControl: TimeControl,
    val createdAt: Instant = Clock.System.now(),
    val status: MatchStatus = MatchStatus.PENDING
) {
    init {
        require(players.size >= 2) {
            "Match must have at least 2 players, got ${players.size}"
        }
        require(players.map { it.playerId }.distinct().size == players.size) {
            "All players must be unique"
        }
    }

    /**
     * Get a player by their ID
     */
    fun getPlayer(playerId: String): Player? {
        return players.find { it.playerId == playerId }
    }

    /**
     * Check if a player is part of this match
     */
    fun hasPlayer(playerId: String): Boolean {
        return players.any { it.playerId == playerId }
    }
}
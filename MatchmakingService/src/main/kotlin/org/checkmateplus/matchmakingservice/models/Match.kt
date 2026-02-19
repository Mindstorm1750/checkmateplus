package org.checkmateplus.matchmakingservice.models

import kotlinx.serialization.Serializable
import org.checkmateplus.chessLibrary.playerModels.BotModelType
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.MatchStatus
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl
import java.util.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a matched set of players ready to start a game.
 * Generic design supports future game variants (e.g., 4-player chess, teams).
 * Colour/side assignment is handled by the game server.
 *
 * @property matchId Unique identifier for the match.
 * @property players List of players participating in the match.
 * @property timeControl The time control settings for the match.
 * @property createdAt Timestamp when the match was created.
 * @property status Current status of the match.
 * @property matchType The match type (competitive or casual, pvp or pve)
 */
@Serializable
@OptIn(ExperimentalTime::class)
data class Match(
    val matchId: String = UUID.randomUUID().toString(),
    val players: List<Player>,
    val timeControl: TimeControl,
    val createdAt: Instant = Clock.System.now(),
    val status: MatchStatus = MatchStatus.PENDING,
    val matchType: MatchType,
    val botModelType: BotModelType? = null,
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
     * Retrieves a player from the match by their unique ID.
     *
     * @param playerId The ID of the player to find.
     * @return The [Player] if found, null otherwise.
     */
    fun getPlayer(playerId: String): Player? {
        return players.find { it.playerId == playerId }
    }

    /**
     * Checks if a player with the given ID is part of this match.
     *
     * @param playerId The ID of the player to check.
     * @return true if the player is in the match, false otherwise.
     */
    fun hasPlayer(playerId: String): Boolean {
        return players.any { it.playerId == playerId }
    }
}
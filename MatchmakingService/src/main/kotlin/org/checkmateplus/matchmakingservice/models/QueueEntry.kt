package org.checkmateplus.matchmakingservice.models

import kotlinx.serialization.Serializable
import org.checkmateplus.chessLibrary.playerModels.Player
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl

/**
 * Represents an entry in the matchmaking queue.
 *
 * @property player The player who is in the queue.
 * @property matchType The type of match the player is looking for.
 * @property timeControl The preferred time control for the match, if any.
 * @property mmr The matchmaking rating of the player at the time of joining the queue.
 * @property lastNotifiedRange The MMR range last sent to the player, used to detect expansions.
 */
@Serializable
data class QueueEntry(
    val player: Player,
    val matchType: MatchType,
    val timeControl: TimeControl?,
    val mmr: Int?,
    val lastNotifiedRange: Int = 0
)
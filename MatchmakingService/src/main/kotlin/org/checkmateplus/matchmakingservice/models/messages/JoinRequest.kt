package org.checkmateplus.matchmakingservice.models.messages

import org.checkmateplus.chessLibrary.playerModels.BotConfiguration
import org.checkmateplus.chessLibrary.playerModels.MatchType
import org.checkmateplus.chessLibrary.playerModels.TimeControl

/**
 * Request payload for a player joining the matchmaking queue.
 *
 * @property displayName The name the player wish to display during the match.
 * @property matchType The type of queue the player wishes to join.
 * @property timeControl The time control the player wishes to play with.
 */
data class JoinRequest(val displayName: String, val matchType: MatchType, val timeControl: TimeControl?, val botConfiguration: BotConfiguration?)

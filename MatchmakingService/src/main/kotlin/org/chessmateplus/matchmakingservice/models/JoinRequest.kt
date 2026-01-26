package org.chessmateplus.matchmakingservice.models

/**
 * Request payload for a player joining the matchmaking queue.
 *
 * @property displayName The name the player wish to display during the match.
 */
data class JoinRequest(val displayName: String)
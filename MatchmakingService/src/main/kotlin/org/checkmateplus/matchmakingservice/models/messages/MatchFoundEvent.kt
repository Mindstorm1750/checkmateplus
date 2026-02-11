package org.checkmateplus.matchmakingservice.models.messages

/**
 * Event sent to players when a match has been successfully formed and a server allocated.
 *
 * @property matchId The unique identifier of the formed match.
 * @property websocketUrl The WebSocket URL of the game server where the match is hosted.
 * @property opponentName The display name of the opponent player.
 */
data class MatchFoundEvent(
    val matchId: String,
    val websocketUrl: String,
    val opponentName: String,
)
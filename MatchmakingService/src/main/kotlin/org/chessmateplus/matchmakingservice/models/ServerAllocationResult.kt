package org.chessmateplus.matchmakingservice.models

/**
 * Result of a game server allocation request.
 *
 * @property success Whether the allocation was successful.
 * @property serverId The ID of the allocated server.
 * @property matchId The ID of the match allocated to the server.
 * @property websocketUrl The URL to connect to the server via WebSockets.
 * @property estimatedWaitSeconds Estimated wait time if the match hasn't started yet.
 * @property message Additional information or error message.
 */
data class ServerAllocationResult(
    val success: Boolean,
    val serverId: String = "",
    val matchId: String = "",
    val websocketUrl: String = "",
    val estimatedWaitSeconds: Int = 0,
    val message: String = ""
)
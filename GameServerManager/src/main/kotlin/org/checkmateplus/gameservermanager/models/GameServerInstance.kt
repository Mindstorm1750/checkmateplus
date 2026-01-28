package org.checkmateplus.gameservermanager.models


/**
 * Represents a registered game server instance in the matchmaking pool.
 *
 * @property activeMatches Current number of active matches on the server.
 * @property activePlayers Current number of connected players on the server.
 * @property maxMatches Maximum capacity for concurrent matches.
 * @property serverId Unique identifier for the server.
 * @property ipAddress Network address of the server.
 * @property port gRPC port the server is listening on.
 * @property lastheartbeatTime Timestamp of the last received heartbeat in milliseconds.
 */
data class GameServerInstance(
    var activeMatches: Int,
    var activePlayers: Int,
    val maxMatches: Int,
    var serverId: String,
    var ipAddress: String,
    var port: Int,
    var lastheartbeatTime: Long,
)

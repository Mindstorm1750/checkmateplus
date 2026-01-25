package org.chessmateplus.gameservermanager.models


data class GameServerInstance(
    var activeMatches: Int,
    var activePlayers: Int,
    val maxMatches: Int,
    var serverId: String,
    var ipAddress: String,
    var port: Int,
    var lastheartbeatTime: Long,
)

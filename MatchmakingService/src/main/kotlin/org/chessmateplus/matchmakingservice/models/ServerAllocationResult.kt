package org.chessmateplus.matchmakingservice.models

data class ServerAllocationResult(
    val success: Boolean,
    val serverId: String = "",
    val matchId: String = "",
    val websocketUrl: String = "",
    val estimatedWaitSeconds: Int = 0,
    val message: String = ""
)
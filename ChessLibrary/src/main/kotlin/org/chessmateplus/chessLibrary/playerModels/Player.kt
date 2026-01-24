package org.chessmateplus.chessLibrary.playerModels

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Base class for all player types in the matchmaking system
 */
@Serializable
sealed class Player {
    abstract val playerId: String
    abstract val displayName: String
    @OptIn(ExperimentalTime::class)
    abstract val joinedQueueAt: Instant
    abstract val skillRating: Int?
    /**
     * Anonymous player with session-based identity
     */
    @Serializable
    data class Anonymous @OptIn(ExperimentalTime::class) constructor(
        override val playerId: String = UUID.randomUUID().toString(),
        override val displayName: String = "Guest${playerId.take(6)}",
        override val joinedQueueAt: Instant = Clock.System.now(),
        override val skillRating: Int? = null
    ) : Player()
}
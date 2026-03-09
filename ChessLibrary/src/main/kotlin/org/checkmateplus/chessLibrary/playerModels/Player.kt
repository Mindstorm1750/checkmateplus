package org.checkmateplus.chessLibrary.playerModels

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
    /**
     * AI or bot player.
     *
     * @property playerId Unique identifier for the bot player.
     * @property displayName The name of the bot displayed in-game.
     * @property joinedQueueAt The time when the bot joined the queue.
     * @property skillRating The optional skill rating for matchmaking.
     * @property difficulty The AI difficulty level (e.g., 1-10).
     * @property botModelType The AI model type used by the bot.
     */
    @Serializable
    data class Bot @OptIn(ExperimentalTime::class) constructor(
        override val playerId: String = UUID.randomUUID().toString(),
        override val displayName: String = "Bot-${playerId.take(6)}",
        override val joinedQueueAt: Instant = Clock.System.now(),
        override val skillRating: Int? = null,
        val difficulty: Int = 1,
        val botConfiguration: BotConfiguration = BotConfiguration(
            botModelType = BotModelType.RANDOM,
            botMoveTimeModelType = BotMoveTimeModelType.NODELAY
        ),
    ) : Player()
}
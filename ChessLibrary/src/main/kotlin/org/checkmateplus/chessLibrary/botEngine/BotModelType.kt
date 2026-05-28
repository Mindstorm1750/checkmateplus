package org.checkmateplus.chessLibrary.botEngine

import kotlinx.serialization.Serializable

/**
 * Represents the type of AI model used by a bot player.
 */
@Serializable
enum class BotModelType {
    /**
     * Random algorithm for determining the best move.
     */
    RANDOM,
    MINMAX
}
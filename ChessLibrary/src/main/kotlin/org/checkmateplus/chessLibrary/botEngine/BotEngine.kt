package org.checkmateplus.chessLibrary.botEngine

import org.checkmateplus.chessLibrary.chessEngine.Board
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.playerModels.BotModelType

/**
 * Base class for all bot engines that can generate moves for a given chess board state.
 *
 * Implementations encapsulate different AI strategies (e.g., random, Minimax, etc.).
 * Use [create] to obtain an engine instance for a specific [BotModelType].
 */
sealed class BotEngine {
    /**
     * Computes the next move for the provided [board] position.
     *
     * Implementations must return a legal move for the side to play.
     */
    abstract fun getMove(board: Board): Move

    /**
     * A very basic engine that selects a legal move uniformly at random.
     * Useful for smoke tests and baseline comparisons.
     */
    class RandomEngine : BotEngine() {
        /**
         * Returns a random move from [Board.generateLegalMoves].
         * Assumes there is at least one legal move in the current position.
         */
        override fun getMove(board: Board): Move {
            return board.generateLegalMoves().random()
        }
    }

    class MinMaxEngine : BotEngine() {
        override fun getMove(board: Board): Move {
            return board.findBestMoves(4) ?: board.generateLegalMoves().random()
        }
    }

    companion object {
        /**
         * Factory method for creating a [BotEngine] based on the requested [botModelType].
         */
        fun create(botModelType: BotModelType): BotEngine {
            return when (botModelType) {
                BotModelType.RANDOM -> RandomEngine()
                BotModelType.MINMAX -> MinMaxEngine()
            }
        }
    }
}
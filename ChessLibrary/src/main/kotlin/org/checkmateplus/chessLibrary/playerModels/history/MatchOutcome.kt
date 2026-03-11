package org.checkmateplus.chessLibrary.playerModels.history

import kotlinx.serialization.Serializable

@Serializable
enum class MatchOutcome {
    WHITE_WINS, BLACK_WINS, DRAW, ABANDONED
}

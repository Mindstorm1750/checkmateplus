package org.checkmateplus.chessLibrary.botEngine

import org.checkmateplus.chessLibrary.playerModels.BotMoveTimeModelType
import org.checkmateplus.chessLibrary.playerModels.MoveTimeModelFeatures
import kotlin.math.max

/**
 * @return a Long representing milliseconds
 */
fun calculateMoveTimeDelayMs(botMoveTimeModelType: BotMoveTimeModelType, moveTimeModelFeatures: MoveTimeModelFeatures): Long {
    return when(botMoveTimeModelType) {
        BotMoveTimeModelType.NODELAY -> 0L
        BotMoveTimeModelType.MEDIAN -> 2240L
        BotMoveTimeModelType.REALISTIC -> calculateRealisticDelay(moveTimeModelFeatures)
    }
}

/**
 * A linear regression model
 * @return a Long representing milliseconds
 */
private fun calculateRealisticDelay(features: MoveTimeModelFeatures): Long {
    val move_number = features.moveNumber * -0.00824053413451228
    val initial_time = features.initialTime * 0.0058190821748006
    val increment = features.increment * 0.06294285244760271
    val time_remaining_for_this_position = features.timeRemaining * -0.003140769331324015
    val time_opponent_took = features.timeOpponentTook * 0.26757500963761904
    val number_of_legal_moves = features.numberOfLegalMoves * 0.038792908621827236
    val in_check = if (features.inCheck) 0.5525076768512188 else 0.0
    val intercept = 0.2430456998921584
    val sum = move_number +
              initial_time +
              increment +
              time_remaining_for_this_position +
              time_opponent_took +
              number_of_legal_moves +
              in_check +
              intercept

    return max(1000L * sum.toLong(), 0) // don't return a negative value
}

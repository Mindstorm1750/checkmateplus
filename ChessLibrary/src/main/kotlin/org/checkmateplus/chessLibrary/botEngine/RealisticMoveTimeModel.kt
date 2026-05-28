package org.checkmateplus.chessLibrary.botEngine

import kotlin.math.max
import kotlin.math.pow

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
    val was_capture = if (features.wasCapture) -0.49985990582824846 else 0.0
    val move_number = features.moveNumber * -0.022910755280864357
    val initial_time = features.initialTime * 0.005808197681903694
    val increment = features.increment * 0.0678994221188859
    val time_remaining_for_this_position = features.timeRemaining * -0.0031094473295077
    val time_opponent_took = features.timeOpponentTook * 0.2111450747447485
    val number_of_legal_moves = features.numberOfLegalMoves * 0.024391655328449374
    val in_check = if(features.inCheck) 0.22084024995566198 else 0.0
    val time_pressure = (1 / (1 + features.timeRemaining)) * -4.365343091495533
    val opening_unfamiliarity = (1 / (1 + Math.E.pow(6 - features.moveNumber))) * 1.5483814263122477
    val intercept = 0.07368122740219984
    val sum = was_capture +
              move_number +
              initial_time +
              increment +
              time_remaining_for_this_position +
              time_opponent_took +
              number_of_legal_moves +
              in_check +
              time_pressure +
              opening_unfamiliarity +
              intercept

    return max(1000L * sum.toLong(), 0) // don't return a negative value
}

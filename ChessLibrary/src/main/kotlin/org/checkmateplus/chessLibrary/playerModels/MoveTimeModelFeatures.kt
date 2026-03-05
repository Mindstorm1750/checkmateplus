package org.checkmateplus.chessLibrary.playerModels

data class MoveTimeModelFeatures(
    /**
     * Refers to Board's fullMoveNumber
     */
    val moveNumber: Int,
    /**
     * Refers to the same thing as TimeControl's minutes, but initialTime's units are seconds instead
     */
    val initialTime: Int,
    /**
     * Refers to TimeControl's increment (units are seconds)
     */
    val increment: Int,
    /**
     * In seconds
     */
    val timeRemaining: Double,
    /**
     * In seconds
     */
    val timeOpponentTook: Double,
    /**
     * Refers to the size of Board's generateLegalMoves()
     */
    val numberOfLegalMoves: Int,
    /**
     * Refers to Board's isInCheck() for the side to move
     */
    val inCheck: Boolean,
)
package org.checkmateplus.chessLibrary.chessEngine

import kotlinx.serialization.Serializable

@Serializable
data class Move(
    val from: Square, // Where piece starts
    val to: Square, // Where piece ends up
    val promotion: PieceType? = null
)
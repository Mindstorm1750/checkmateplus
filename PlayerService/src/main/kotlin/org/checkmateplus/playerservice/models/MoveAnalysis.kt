package org.checkmateplus.playerservice.models

import kotlinx.serialization.Serializable

@Serializable
data class MoveAnalysis(
    val ply: Int,
    val moveUci: String,
    val bestMoveUci: String,
    val scoreBefore: AnalysisScore,
    val scoreAfter: AnalysisScore,
    val cpLoss: Int,
    val classification: String,
    val pv: List<String>,
    val depth: Int,
)
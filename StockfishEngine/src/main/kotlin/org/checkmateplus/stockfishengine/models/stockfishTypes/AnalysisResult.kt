package org.checkmateplus.stockfishengine.models.stockfishTypes

data class AnalysisResult(
    val bestMove: String?,
    val ponder: String?,
    val lines: List<Line>,
)
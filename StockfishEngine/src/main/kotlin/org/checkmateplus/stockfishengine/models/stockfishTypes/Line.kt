package org.checkmateplus.stockfishengine.models.stockfishTypes

data class Line(
    val multipv: Int,
    val depth: Int,
    val seldepth: Int,
    val score: Score,
    val nodes: Long,
    val nps: Long,
    val timeMs: Long,
    val pv: List<String>,
    val wdl: Triple<Int, Int, Int>?,
)
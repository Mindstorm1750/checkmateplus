package org.checkmateplus.stockfishengine.models.stockfishTypes

sealed interface SearchLimit {
    data class Depth(val plies: Int) : SearchLimit
    data class Movetime(val ms: Int) : SearchLimit
    data class Nodes(val n: Long) : SearchLimit
    data class Clock(
        val wtimeMs: Long,
        val btimeMs: Long,
        val wincMs: Long = 0,
        val bincMs: Long = 0,
        val movestogo: Int? = null,
    ) : SearchLimit
}
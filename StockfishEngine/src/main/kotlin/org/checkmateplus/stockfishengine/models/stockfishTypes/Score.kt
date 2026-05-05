package org.checkmateplus.stockfishengine.models.stockfishTypes

/**
 * Score from the side-to-move's POV. Caller flips for White-POV when needed.
 * Exactly one of [cp] / [mateIn] is non-null in a well-formed line.
 */
data class Score(val cp: Int?, val mateIn: Int?) {
    val isMate: Boolean get() = mateIn != null

    /** Total order across cp and mate values for ranking lines. */
    fun toCpForCompare(): Int =
        mateIn?.let { if (it > 0) 100_000 - it else -100_000 - it } ?: cp!!
}
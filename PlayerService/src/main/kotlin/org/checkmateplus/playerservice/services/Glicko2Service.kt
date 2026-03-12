package org.checkmateplus.playerservice.services

import org.springframework.stereotype.Service
import kotlin.math.*

/**
 * Glicko-2 rating system implementation.
 *
 * Reference: Mark Glickman, "Example of the Glicko-2 system"
 * http://www.glicko.net/glicko/glicko2.pdf
 *
 * Based On: https://github.com/goochjs/glicko2
 *
 * Per-game updates are used (rather than batched rating periods), which is a minor
 * deviation from the strict Glicko-2 spec but is widely used in practice.
 */
@Service
class Glicko2Service {

    companion object {
        /** Converts between Glicko-1 and Glicko-2 scales. */
        private const val SCALE = 173.7178

        /**
         * System constant τ — constrains how much volatility can change per game.
         * Smaller = more stable volatility. Glickman recommends 0.3–1.2.
         */
        private const val TAU = 0.5

        /** Convergence tolerance for the Illinois algorithm. */
        private const val EPSILON = 0.000001

        /** Minimum RD — a very established player's rating uncertainty floor. */
        private const val MIN_RD = 30.0

        /** Maximum RD — an unrated or very inactive player. */
        private const val MAX_RD = 350.0
    }

    data class RatingResult(
        val rating: Int,
        val ratingDeviation: Double,
        val volatility: Double
    )

    enum class Outcome { WHITE_WINS, BLACK_WINS, DRAW }

    /**
     * Calculates updated Glicko-2 ratings for both players after a single game.
     *
     * @return Pair of (updatedWhiteResult, updatedBlackResult)
     */
    fun updateRatings(
        whiteRating: Int, whiteRD: Double, whiteVolatility: Double,
        blackRating: Int, blackRD: Double, blackVolatility: Double,
        outcome: Outcome
    ): Pair<RatingResult, RatingResult> {
        val whiteScore = when (outcome) {
            Outcome.WHITE_WINS -> 1.0
            Outcome.DRAW -> 0.5
            Outcome.BLACK_WINS -> 0.0
        }

        val newWhite = calculateNewRating(
            rating = whiteRating, rd = whiteRD, volatility = whiteVolatility,
            oppRating = blackRating, oppRd = blackRD,
            score = whiteScore
        )
        val newBlack = calculateNewRating(
            rating = blackRating, rd = blackRD, volatility = blackVolatility,
            oppRating = whiteRating, oppRd = whiteRD,
            score = 1.0 - whiteScore
        )

        return Pair(newWhite, newBlack)
    }

    /**
     * Applies the Glicko-2 update formula for one player against one opponent.
     *
     * Steps follow Glickman's paper §3.
     */
    private fun calculateNewRating(
        rating: Int, rd: Double, volatility: Double,
        oppRating: Int, oppRd: Double, score: Double
    ): RatingResult {
        // Step 2: Convert to Glicko-2 scale
        val mu = (rating - 1500.0) / SCALE
        val phi = rd / SCALE
        val muJ = (oppRating - 1500.0) / SCALE
        val phiJ = oppRd / SCALE

        // Step 3: g(φj) and E(μ, μj, φj)
        val gPhiJ = g(phiJ)
        val e = E(mu, muJ, gPhiJ)

        // Step 4: estimated variance v
        val v = 1.0 / (gPhiJ.pow(2) * e * (1.0 - e))

        // Step 5: estimated improvement Δ
        val delta = v * gPhiJ * (score - e)

        // Steps 5.1–5.7: new volatility σ' via Illinois algorithm
        val newVolatility = newVolatility(phi, volatility, v, delta)

        // Step 6: pre-rating-period deviation φ*
        val phiStar = sqrt(phi.pow(2) + newVolatility.pow(2))

        // Step 7: updated deviation φ' and rating μ'
        val newPhi = 1.0 / sqrt(1.0 / phiStar.pow(2) + 1.0 / v)
        val newMu = mu + newPhi.pow(2) * gPhiJ * (score - e)

        // Step 8: convert back to Glicko-1 scale
        return RatingResult(
            rating = (SCALE * newMu + 1500.0).roundToInt(),
            ratingDeviation = (SCALE * newPhi).coerceIn(MIN_RD, MAX_RD),
            volatility = newVolatility
        )
    }

    /** g(φ) = 1 / sqrt(1 + 3φ²/π²) */
    private fun g(phi: Double): Double =
        1.0 / sqrt(1.0 + 3.0 * phi.pow(2) / PI.pow(2))

    /** E(μ, μj, φj) = 1 / (1 + exp(-g(φj)(μ - μj))) */
    private fun E(mu: Double, muJ: Double, gPhiJ: Double): Double =
        1.0 / (1.0 + exp(-gPhiJ * (mu - muJ)))

    /**
     * Determines new volatility σ' using the Illinois algorithm (regula falsi variant).
     *
     * Solves for the root of f(x) where:
     *   f(x) = [exp(x)(Δ² - φ² - v - exp(x))] / [2(φ² + v + exp(x))²] - (x - a)/τ²
     */
    private fun newVolatility(phi: Double, sigma: Double, v: Double, delta: Double): Double {
        val a = ln(sigma.pow(2))

        fun f(x: Double): Double {
            val ex = exp(x)
            val denom = phi.pow(2) + v + ex
            return (ex * (delta.pow(2) - phi.pow(2) - v - ex)) / (2.0 * denom.pow(2)) -
                    (x - a) / TAU.pow(2)
        }

        // Step 5.4: initial bracket [A, B]
        var A = a
        var B = if (delta.pow(2) > phi.pow(2) + v) {
            ln(delta.pow(2) - phi.pow(2) - v)
        } else {
            var k = 1
            while (f(a - k * TAU) < 0) k++
            a - k * TAU
        }

        // Step 5.5
        var fA = f(A)
        var fB = f(B)

        // Step 5.6: iterate until convergence
        while (abs(B - A) > EPSILON) {
            val C = A + (A - B) * fA / (fB - fA)
            val fC = f(C)
            if (fC * fB < 0) {
                A = B
                fA = fB
            } else {
                fA /= 2.0   // Illinois modification
            }
            B = C
            fB = fC
        }

        // Step 5.7
        return exp(A / 2.0)
    }
}
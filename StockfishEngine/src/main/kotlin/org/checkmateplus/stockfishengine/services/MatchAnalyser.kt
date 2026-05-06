package org.checkmateplus.stockfishengine.services

import org.checkmateplus.stockfishengine.engine.StockfishDriver
import org.checkmateplus.stockfishengine.models.stockfishTypes.Line
import org.checkmateplus.stockfishengine.models.stockfishTypes.Score as DriverScore
import org.checkmateplus.stockfishengine.models.stockfishTypes.SearchLimit
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto.AnalyseMatchRequest
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto.AnalyseMatchResponse
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto.AnalysisConfig
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto.Classification
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto.MoveAnalysis
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto.Score as ProtoScore
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private const val DEFAULT_DEPTH = 16
private const val STARTPOS_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

@Service
class MatchAnalyser(
    private val driver: StockfishDriver,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val lock = ReentrantLock()

    fun analyse(request: AnalyseMatchRequest): AnalyseMatchResponse {
        val startingFen = request.startingFen.ifBlank { STARTPOS_FEN }
        val moves = request.uciMovesList
        val limit = resolveLimit(request.config)
        val depth = if (request.config.depth > 0) request.config.depth else DEFAULT_DEPTH

        return lock.withLock {
            try {
                driver.newGame()

                val analyses = ArrayList<MoveAnalysis>(moves.size)
                for (i in moves.indices) {
                    val played = moves[i]
                    val before = positionEval(startingFen, moves.subList(0, i), limit)
                    val after = positionEval(startingFen, moves.subList(0, i + 1), limit)

                    val bestCp = before.score.toCpForCompare()
                    val afterCpFromMoverPov = -after.score.toCpForCompare()
                    val cpLoss = (bestCp - afterCpFromMoverPov).coerceAtLeast(0)

                    analyses += MoveAnalysis.newBuilder()
                        .setPly(i + 1)
                        .setMoveUci(played)
                        .setBestMoveUci(before.pv.firstOrNull().orEmpty())
                        .setScoreBefore(before.score.toProto())
                        .setScoreAfter(flip(after.score).toProto())
                        .setCpLoss(cpLoss)
                        .setClassification(classify(cpLoss, played, before.pv.firstOrNull()))
                        .addAllPv(before.pv)
                        .setDepth(depth)
                        .build()
                }

                AnalyseMatchResponse.newBuilder()
                    .setMatchId(request.matchId)
                    .setSuccess(true)
                    .addAllMoves(analyses)
                    .build()
            } catch (e: Exception) {
                logger.error("Analysis failed for match ${request.matchId}: ${e.message}", e)
                AnalyseMatchResponse.newBuilder()
                    .setMatchId(request.matchId)
                    .setSuccess(false)
                    .setError(e.message ?: e.javaClass.simpleName)
                    .build()
            }
        }
    }

    private fun positionEval(startingFen: String, moves: List<String>, limit: SearchLimit): Line {
        driver.setPosition(startingFen, moves)
        val result = driver.analyse(limit, multiPv = 1)
        return result.lines.firstOrNull()
            ?: error("No analysis line returned for position fen=$startingFen moves=${moves.size}")
    }

    private fun resolveLimit(config: AnalysisConfig): SearchLimit = when {
        config.movetimeMs > 0 -> SearchLimit.Movetime(config.movetimeMs)
        config.depth > 0 -> SearchLimit.Depth(config.depth)
        else -> SearchLimit.Depth(DEFAULT_DEPTH)
    }

    private fun classify(cpLoss: Int, played: String, best: String?): Classification {
        if (best != null && played == best) return Classification.CLASSIFICATION_BEST
        return when {
            cpLoss < 10 -> Classification.CLASSIFICATION_BEST
            cpLoss < 25 -> Classification.CLASSIFICATION_GREAT
            cpLoss < 50 -> Classification.CLASSIFICATION_GOOD
            cpLoss < 100 -> Classification.CLASSIFICATION_INACCURACY
            cpLoss < 250 -> Classification.CLASSIFICATION_MISTAKE
            else -> Classification.CLASSIFICATION_BLUNDER
        }
    }

    private fun flip(s: DriverScore): DriverScore =
        DriverScore(cp = s.cp?.let { -it }, mateIn = s.mateIn?.let { -it })

    private fun DriverScore.toProto(): ProtoScore {
        val b = ProtoScore.newBuilder()
        val m = mateIn
        if (m != null) b.mate = m else b.cp = cp ?: 0
        return b.build()
    }
}

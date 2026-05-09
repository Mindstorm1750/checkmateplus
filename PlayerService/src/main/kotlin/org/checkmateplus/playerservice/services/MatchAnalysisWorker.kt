package org.checkmateplus.playerservice.services

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.checkmateplus.chessLibrary.chessEngine.Move
import org.checkmateplus.chessLibrary.chessEngine.PieceType
import org.checkmateplus.chessLibrary.chessEngine.toAlgebraic
import org.checkmateplus.chessLibrary.playerModels.history.Turn
import org.checkmateplus.playerservice.entitys.AnalysisStatus
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.models.AnalysisScore
import org.checkmateplus.playerservice.models.MatchAnalysis
import org.checkmateplus.playerservice.models.MoveAnalysis
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.stockfishengine.proto.StockfishEngineProto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private const val STARTPOS_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

@Service
class MatchAnalysisWorker(
    private val matchRecordRepository: MatchRecordRepository,
    private val stockfishEngineClient: StockfishEngineClient,
    @Value("\${analysis.depth:16}") private val depth: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val turnsSerializer = ListSerializer(Turn.serializer())

    suspend fun run(matchId: String) {
        try {
            val entity = loadEntity(matchId) ?: return
            markRunning(matchId)
            val turns = Json.decodeFromString(turnsSerializer, entity.historyJson)
            if (turns.isEmpty()) {
                completeEmpty(matchId)
                return
            }

            val moves = turns.map { uciOf(it.move) }
            val request = StockfishEngineProto.AnalyseMatchRequest.newBuilder()
                .setMatchId(matchId)
                .setStartingFen(STARTPOS_FEN)
                .addAllUciMoves(moves)
                .setConfig(
                    StockfishEngineProto.AnalysisConfig.newBuilder()
                        .setDepth(depth)
                        .setMultiPv(1)
                        .build()
                )
                .build()

            val response = stockfishEngineClient.analyseMatch(request)
            if (!response.success) {
                logger.warn("StockfishEngine reported failure for matchId={}: {}", matchId, response.error)
                markFailed(matchId)
                return
            }

            val analysis = response.toDomain()
            val json = Json.encodeToString(MatchAnalysis.serializer(), analysis)
            markComplete(matchId, json)
            logger.info("Stored analysis for matchId={} ({} plies)", matchId, analysis.moves.size)
        } catch (e: Exception) {
            logger.error("Analysis failed for matchId={}: {}", matchId, e.message, e)
            markFailed(matchId)
        }
    }

    private fun loadEntity(matchId: String): MatchRecordEntity? {
        val entity = matchRecordRepository.findById(matchId).orElse(null)
        if (entity == null) logger.warn("MatchAnalysisWorker: match not found {}", matchId)
        return entity
    }

    private fun markRunning(matchId: String) {
        val entity = matchRecordRepository.findById(matchId).orElse(null) ?: return
        entity.analysisStatus = AnalysisStatus.RUNNING
        matchRecordRepository.save(entity)
    }

    private fun markComplete(matchId: String, json: String) {
        val entity = matchRecordRepository.findById(matchId).orElse(null) ?: return
        entity.analysisStatus = AnalysisStatus.COMPLETE
        entity.analysisJson = json
        matchRecordRepository.save(entity)
    }

    private fun markFailed(matchId: String) {
        val entity = matchRecordRepository.findById(matchId).orElse(null) ?: return
        entity.analysisStatus = AnalysisStatus.FAILED
        matchRecordRepository.save(entity)
    }

    private fun completeEmpty(matchId: String) {
        val entity = matchRecordRepository.findById(matchId).orElse(null) ?: return
        entity.analysisStatus = AnalysisStatus.COMPLETE
        entity.analysisJson = Json.encodeToString(
            MatchAnalysis.serializer(),
            MatchAnalysis(matchId, emptyList())
        )
        matchRecordRepository.save(entity)
    }

    private fun uciOf(move: Move): String {
        val from = move.from.toAlgebraic()
        val to = move.to.toAlgebraic()
        val promo = move.promotion?.let { promoChar(it) }.orEmpty()
        return "$from$to$promo"
    }

    private fun promoChar(p: PieceType): String = when (p) {
        PieceType.QUEEN -> "q"
        PieceType.ROOK -> "r"
        PieceType.BISHOP -> "b"
        PieceType.KNIGHT -> "n"
        else -> ""
    }

    private fun StockfishEngineProto.AnalyseMatchResponse.toDomain(): MatchAnalysis = MatchAnalysis(
        matchId = matchId,
        moves = movesList.map { it.toDomain() },
    )

    private fun StockfishEngineProto.MoveAnalysis.toDomain(): MoveAnalysis = MoveAnalysis(
        ply = ply,
        moveUci = moveUci,
        bestMoveUci = bestMoveUci,
        scoreBefore = scoreBefore.toDomain(),
        scoreAfter = scoreAfter.toDomain(),
        cpLoss = cpLoss,
        classification = classification.name,
        pv = pvList.toList(),
        depth = depth,
    )

    private fun StockfishEngineProto.Score.toDomain(): AnalysisScore = when (kindCase) {
        StockfishEngineProto.Score.KindCase.CP -> AnalysisScore(cp = cp)
        StockfishEngineProto.Score.KindCase.MATE -> AnalysisScore(mate = mate)
        else -> AnalysisScore()
    }
}

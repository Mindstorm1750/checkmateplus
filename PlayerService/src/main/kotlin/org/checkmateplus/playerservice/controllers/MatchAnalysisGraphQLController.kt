package org.checkmateplus.playerservice.controllers

import kotlinx.serialization.json.Json
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.models.MatchAnalysis
import org.checkmateplus.playerservice.models.MatchAnalysisGraphQLView
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.playerservice.services.MatchAnalysisService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class MatchAnalysisGraphQLController(
    private val matchRecordRepository: MatchRecordRepository,
    private val matchAnalysisService: MatchAnalysisService,
) {
    @QueryMapping
    fun matchAnalysis(@Argument matchId: String): MatchAnalysisGraphQLView? {
        val entity = matchRecordRepository.findById(matchId).orElse(null) ?: return null
        return entity.toView()
    }

    @MutationMapping
    fun requestMatchAnalysis(@Argument matchId: String): MatchAnalysisGraphQLView {
        val entity = try {
            matchAnalysisService.requestAnalysis(matchId)
        } catch (e: MatchAnalysisService.MatchNotFoundException) {
            throw IllegalArgumentException(e.message, e)
        }
        return entity.toView()
    }

    private fun MatchRecordEntity.toView(): MatchAnalysisGraphQLView {
        val analysis = analysisJson?.let { Json.decodeFromString(MatchAnalysis.serializer(), it) }
        return MatchAnalysisGraphQLView(
            matchId = matchId,
            status = analysisStatus.name,
            moves = analysis?.moves ?: emptyList(),
        )
    }
}

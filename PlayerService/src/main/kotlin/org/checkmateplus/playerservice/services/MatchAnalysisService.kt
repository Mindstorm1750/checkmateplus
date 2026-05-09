package org.checkmateplus.playerservice.services

import org.checkmateplus.playerservice.entitys.AnalysisStatus
import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional



@Service
class MatchAnalysisService(
    private val matchRecordRepository: MatchRecordRepository,
    private val analysisQueue: AnalysisQueue,
    @Value("\${analysis.enabled:true}") private val enabled: Boolean,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    class MatchNotFoundException(matchId: String) : RuntimeException("Match not found: $matchId")
    @Transactional
    fun requestAnalysis(matchId: String): MatchRecordEntity {
        val entity = matchRecordRepository.findById(matchId).orElseThrow { MatchNotFoundException(matchId) }
        if (!enabled) {
            entity.analysisStatus = AnalysisStatus.DISABLED
            return matchRecordRepository.save(entity)
        }
        if (entity.analysisStatus == AnalysisStatus.QUEUED || entity.analysisStatus == AnalysisStatus.RUNNING) {
            logger.info("Analysis already in progress for matchId={} (status={})", matchId, entity.analysisStatus)
            return entity
        }
        if (entity.analysisStatus == AnalysisStatus.COMPLETE && entity.analysisJson != null) {
            return entity
        }
        entity.analysisStatus = AnalysisStatus.QUEUED
        entity.analysisJson = null
        val saved = matchRecordRepository.save(entity)
        analysisQueue.enqueue(matchId)
        logger.info("Enqueued matchId={} for analysis", matchId)
        return saved
    }
}

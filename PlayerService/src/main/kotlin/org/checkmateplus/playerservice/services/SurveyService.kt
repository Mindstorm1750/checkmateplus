package org.checkmateplus.playerservice.services

import org.checkmateplus.playerservice.entitys.SurveyEntity
import org.checkmateplus.playerservice.models.Survey
import org.checkmateplus.playerservice.repository.MatchRecordRepository
import org.checkmateplus.playerservice.repository.SurveyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SurveyService(
    private val surveyRepository: SurveyRepository,
    private val matchRecordRepository: MatchRecordRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun postGameSurvey(matchId: String, surveyScore: Int, optionalComments: String?): Survey {
        require(surveyScore in 1..5) { "Survey score must be between 1 and 5" }
        val matchRecord = matchRecordRepository.findById(matchId)
            .orElseThrow { IllegalArgumentException("Match not found: $matchId") }
        val entity = SurveyEntity(
            matchRecord = matchRecord,
            surveyScore = surveyScore,
            optionalComments = optionalComments
        )
        val saved = surveyRepository.save(entity)
        logger.info("Stored survey for match: $matchId")
        return Survey(
            matchId = matchId,
            surveyScore = saved.surveyScore,
            optionalComments = saved.optionalComments
        )
    }
}
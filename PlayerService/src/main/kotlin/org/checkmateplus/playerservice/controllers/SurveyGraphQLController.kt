package org.checkmateplus.playerservice.controllers

import org.checkmateplus.playerservice.models.Survey
import org.checkmateplus.playerservice.services.SurveyService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.stereotype.Controller

@Controller
class SurveyGraphQLController(
    private val surveyService: SurveyService
) {
    @MutationMapping
    fun postGameSurvey(
        @Argument matchId: String,
        @Argument surveyScore: Int,
        @Argument optionalComments: String?
    ): Survey = surveyService.postGameSurvey(matchId, surveyScore, optionalComments)
}
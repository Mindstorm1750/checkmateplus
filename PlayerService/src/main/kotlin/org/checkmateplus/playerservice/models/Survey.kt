package org.checkmateplus.playerservice.models

data class Survey(
    val matchId: String,
    val surveyScore: Int,
    val optionalComments: String?
)
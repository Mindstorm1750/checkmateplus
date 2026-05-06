package org.checkmateplus.playerservice.models

import kotlinx.serialization.Serializable

@Serializable
data class AnalysisScore(
    val cp: Int? = null,
    val mate: Int? = null,
)
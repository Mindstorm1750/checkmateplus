package org.checkmateplus.playerservice.models

data class SpectateMatchPage(
    val matches: List<SpectateMatch>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

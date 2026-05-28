package org.checkmateplus.playerservice.models

data class MatchHistoryPage(
    val matches: List<MatchSummary>,
    val nextCursor: String?,
    val hasMore: Boolean
)
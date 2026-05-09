package org.checkmateplus.playerservice.controllers

import org.checkmateplus.playerservice.models.SpectateMatch
import org.checkmateplus.playerservice.models.SpectateMatchPage
import org.checkmateplus.playerservice.services.SpectateService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class SpectateGraphQLController(
    private val spectateService: SpectateService,
) {
    @QueryMapping
    fun liveMatches(
        @Argument afterCursor: String?,
        @Argument size: Int? = 20,
    ): SpectateMatchPage = spectateService.getLiveMatches(afterCursor, size ?: 20)

    @QueryMapping
    fun spectateMatch(@Argument matchId: String): SpectateMatch? =
        spectateService.getMatch(matchId)
}

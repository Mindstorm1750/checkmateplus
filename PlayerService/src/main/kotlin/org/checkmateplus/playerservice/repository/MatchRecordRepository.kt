package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.MatchRecordEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MatchRecordRepository : JpaRepository<MatchRecordEntity, String> {
    fun findByWhitePlayerPlayerIdOrBlackPlayerPlayerId(
        whitePlayerId: String,
        blackPlayerId: String
    ): List<MatchRecordEntity>

    fun findByWhitePlayerPlayerIdOrBlackPlayerPlayerIdOrderByCreatedAtEpochMsDesc(
        whitePlayerId: String,
        blackPlayerId: String
    ): List<MatchRecordEntity>
}
package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface RegisteredPlayerRepository : JpaRepository<RegisteredPlayerEntity, String> {
    fun findAllByOrderBySkillRatingDesc(pageable: Pageable): List<RegisteredPlayerEntity>

    @Query("SELECT p FROM RegisteredPlayerEntity p JOIN AccountEntity a ON a.player = p WHERE a.totpEnabled = true ORDER BY p.skillRating DESC")
    fun findAllVerifiedOrderBySkillRatingDesc(pageable: Pageable): List<RegisteredPlayerEntity>
}
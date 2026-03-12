package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RegisteredPlayerRepository : JpaRepository<RegisteredPlayerEntity, String> {
    fun findAllByOrderBySkillRatingDesc(pageable: Pageable): List<RegisteredPlayerEntity>
}
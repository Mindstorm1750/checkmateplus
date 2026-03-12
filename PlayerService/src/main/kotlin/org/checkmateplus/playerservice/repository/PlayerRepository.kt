package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PlayerRepository : JpaRepository<PlayerEntity, String>

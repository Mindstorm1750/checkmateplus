package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.accounts.AccountEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AccountRepository : JpaRepository<AccountEntity, String> {
    fun findByUsername(username: String): AccountEntity?
    fun existsByUsername(username: String): Boolean
    fun findByGoogleId(googleId: String): AccountEntity?
    fun findByPlayerPlayerId(playerId: String): AccountEntity?
}
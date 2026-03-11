package org.checkmateplus.playerservice.repository

import org.checkmateplus.playerservice.entitys.accounts.RefreshTokenEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, String> {
    fun findByTokenAndRevokedFalse(token: String): RefreshTokenEntity?
    fun deleteAllByAccountId(accountId: String)
}
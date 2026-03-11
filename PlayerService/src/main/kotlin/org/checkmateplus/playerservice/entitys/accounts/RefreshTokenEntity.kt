package org.checkmateplus.playerservice.entitys.accounts

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id val token: String = UUID.randomUUID().toString(),
    @Column(name = "account_id", nullable = false) val accountId: String,
    @Column(name = "expires_at", nullable = false) val expiresAtEpochMs: Long,
    @Column(nullable = false) var revoked: Boolean = false
)
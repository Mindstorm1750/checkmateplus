package org.checkmateplus.playerservice.entitys.accounts

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "refresh_tokens")
class RefreshTokenEntity(
    @Id var token: String = UUID.randomUUID().toString(),
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "account_id", nullable = false) var account: AccountEntity,
    @Column(name = "expires_at", nullable = false) var expiresAtEpochMs: Long,
    @Column(nullable = false) var revoked: Boolean = false
)
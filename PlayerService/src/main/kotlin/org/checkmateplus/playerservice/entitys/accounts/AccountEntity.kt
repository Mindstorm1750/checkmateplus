package org.checkmateplus.playerservice.entitys.accounts

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import java.util.UUID

@Entity
@Table(name = "accounts")
class AccountEntity(

    @Id
    @Column(name = "account_id", nullable = false, updatable = false)
    val accountId: String = "account-${UUID.randomUUID()}",

    @Column(name = "username", unique = true, nullable = false)
    val username: String,

    @Column(name = "totp_secret", nullable = true)
    var totpSecret: String? = null,

    @Column(name = "totp_enabled", nullable = false)
    var totpEnabled: Boolean = false,

    @Column(name = "google_id", unique = true, nullable = true)
    var googleId: String? = null,
    @Column(name = "time_registered", nullable = false)
    var timeRegisteredAt: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: PlayerEntity
)
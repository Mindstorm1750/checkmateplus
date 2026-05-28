package org.checkmateplus.playerservice.services

import jakarta.transaction.Transactional
import org.checkmateplus.playerservice.entitys.accounts.AccountEntity
import org.checkmateplus.playerservice.entitys.accounts.RefreshTokenEntity
import org.checkmateplus.playerservice.entitys.players.AnonymousPlayerEntity
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.checkmateplus.playerservice.entitys.players.RegisteredPlayerEntity
import org.checkmateplus.playerservice.models.AuthResponse
import org.checkmateplus.playerservice.models.PlayerProfile
import org.checkmateplus.playerservice.models.RegisterResponse
import org.checkmateplus.playerservice.repository.AccountRepository
import org.checkmateplus.playerservice.repository.PlayerRepository
import org.checkmateplus.playerservice.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.time.Clock

@Service
class AuthenticationService(
    private val playerRepository: PlayerRepository,
    private val jwtService: JwtService,
    private val totpService: TotpService,
    private val accountRepository: AccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository
) {
    @Transactional
    fun register(username: String): RegisterResponse {
        if (accountRepository.existsByUsername(username)) {
            throw IllegalArgumentException("Username already taken")
        }

        val secret = totpService.generateSecret()
        val playerId = "RUser-${UUID.randomUUID()}"

        // Create the player first
        val player = RegisteredPlayerEntity(
            playerId = playerId,
            displayName = username,
            joinedQueueAt = Clock.System.now().toEpochMilliseconds(),
            skillRating = 1000,
            playerPreferencesJson = null,
        )
        playerRepository.save(player)

        // Create the account linked to the player
        val account = AccountEntity(
            username = username,
            totpSecret = secret,
            totpEnabled = false,
            player = player,
            timeRegisteredAt = Clock.System.now().toEpochMilliseconds(),
        )
        accountRepository.save(account)

        return RegisterResponse(
            qrCodeUri = totpService.generateQrUri(username, secret),
            secret = secret,
            playerId = playerId
        )
    }

    @Transactional
    fun confirmTotp(playerId: String, code: String): AuthResponse {
        val account = accountRepository.findByPlayerPlayerId(playerId)
            ?: throw IllegalArgumentException("Account not found")

        val secret = account.totpSecret ?: throw IllegalStateException("No TOTP secret found")

        if (!totpService.verify(secret, code)) {
            throw IllegalArgumentException("Invalid TOTP code")
        }

        account.totpEnabled = true
        accountRepository.save(account)

        return issueAuth(account.player)
    }
    @Transactional
    fun loginWithTotp(username: String, code: String): AuthResponse {
        val account = accountRepository.findByUsername(username) ?: throw IllegalArgumentException("User not found")

        if (!account.totpEnabled) throw IllegalArgumentException("TOTP not set up")

        val secret = account.totpSecret ?: throw IllegalStateException("No TOTP secret")

        if (!totpService.verify(secret, code)) {
            throw IllegalArgumentException("Invalid TOTP code")
        }

        return issueAuth(account.player)
    }
    @Transactional
    fun loginWithGoogle(googleId: String): AuthResponse {
        val account = accountRepository.findByGoogleId(googleId)
            ?: run {
                // create new account linked to google
                val playerId = "user-${UUID.randomUUID()}"
                val player = AnonymousPlayerEntity(
                    playerId = playerId,
                    displayName = playerId.take(10),
                    joinedQueueAt = Clock.System.now().toEpochMilliseconds(),
                    skillRating = 1000,
                    playerPreferencesJson = null
                )
                playerRepository.save(player)

                val newAccount = AccountEntity(
                    username = playerId.take(10),
                    totpSecret = null,
                    totpEnabled = true, // google handles auth
                    googleId = googleId,
                    player = player
                )
                accountRepository.save(newAccount)
            }

        return issueAuth(account.player)
    }
    @Transactional
    fun loginAsGuest(): AuthResponse {
        val playerId = "anon-${UUID.randomUUID()}"
        val player = AnonymousPlayerEntity(
            playerId = playerId,
            displayName = "Guest${playerId.takeLast(6)}",
            joinedQueueAt = Clock.System.now().toEpochMilliseconds(),
            skillRating = null,
            playerPreferencesJson = null
        )
        playerRepository.save(player)
        return issueAuth(player, isGuest = true)
    }

    @Transactional
    fun refresh(refreshToken: String): AuthResponse {
        val existing = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
            ?: throw IllegalArgumentException("Invalid or expired refresh token")

        if (existing.expiresAtEpochMs < Clock.System.now().toEpochMilliseconds()) {
            refreshTokenRepository.delete(existing)
            throw IllegalArgumentException("Refresh token expired")
        }

        val account = accountRepository.findById(existing.account.accountId).orElseThrow {
            IllegalStateException("Account not found for refresh token")
        }

        // Rotate — delete old, issue new
        refreshTokenRepository.delete(existing)
        return issueAuth(account.player)
    }

    @Transactional
    fun logout(refreshToken: String) {
        refreshTokenRepository.deleteById(refreshToken)
    }

    private fun issueAuth(player: PlayerEntity, isGuest: Boolean = false): AuthResponse {
        val jwt = jwtService.generate(player.playerId, player.displayName, isGuest)

        // Guests don't get refresh tokens
        if (isGuest) return AuthResponse(jwt, null, player.playerId, player.displayName, true)

        val account = accountRepository.findByPlayerPlayerId(player.playerId)
            ?: throw IllegalStateException("No account found for player ${player.playerId}")

        val refreshToken = RefreshTokenEntity(
            account = account,
            expiresAtEpochMs = jwtService.generateRefreshExpiry()
        )
        refreshTokenRepository.save(refreshToken)

        return AuthResponse(jwt, refreshToken.token, player.playerId, player.displayName, false)
    }
    fun getProfile(playerId: String): PlayerProfile {
        val account = accountRepository.findByPlayerPlayerId(playerId)
            ?: throw IllegalArgumentException("Account not found")
        return PlayerProfile(
            playerId = account.player.playerId,
            displayName = account.player.displayName,
            username = account.username,
            skillRating = account.player.skillRating,
            totpEnabled = account.totpEnabled)
    }
}
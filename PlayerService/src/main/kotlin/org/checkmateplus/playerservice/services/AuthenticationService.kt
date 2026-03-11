package org.checkmateplus.playerservice.services

import jakarta.transaction.Transactional
import org.checkmateplus.playerservice.entitys.accounts.AccountEntity
import org.checkmateplus.playerservice.entitys.players.AnonymousPlayerEntity
import org.checkmateplus.playerservice.entitys.players.PlayerEntity
import org.checkmateplus.playerservice.models.AuthResponse
import org.checkmateplus.playerservice.models.PlayerProfile
import org.checkmateplus.playerservice.models.RegisterResponse
import org.checkmateplus.playerservice.repository.AccountRepository
import org.checkmateplus.playerservice.repository.PlayerRepository
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.time.Clock

@Service
class AuthenticationService(
    private val playerRepository: PlayerRepository,
    private val jwtService: JwtService,
    private val totpService: TotpService,
    private val accountRepository: AccountRepository
) {

    fun register(username: String): RegisterResponse {
        if (accountRepository.existsByUsername(username)) {
            throw IllegalArgumentException("Username already taken")
        }

        val secret = totpService.generateSecret()
        val playerId = "user-${UUID.randomUUID()}"

        // Create the player first
        val player = AnonymousPlayerEntity(
            playerId = playerId,
            displayName = username,
            joinedQueueAt = Clock.System.now().toEpochMilliseconds(),
            skillRating = 1000,
            playerPreferencesJson = null
        )
        playerRepository.save(player)

        // Create the account linked to the player
        val account = AccountEntity(
            username = username,
            totpSecret = secret,
            totpEnabled = true,
            player = player
        )
        accountRepository.save(account)

        return RegisterResponse(
            qrCodeUri = totpService.generateQrUri(username, secret),
            secret = secret,
            playerId = playerId
        )
    }


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

    private fun issueAuth(player: PlayerEntity, isGuest: Boolean = false): AuthResponse {
        val jwt = jwtService.generate(player.playerId, player.displayName, isGuest)
        return AuthResponse(jwt, player.playerId, player.displayName, isGuest)
    }
    fun getProfile(playerId: String): PlayerProfile {

        val account = accountRepository.findByPlayerPlayerId(playerId)
            ?: throw IllegalArgumentException("Account not found")
        return PlayerProfile(
            playerId = account.player.playerId,
            displayName = account.player.displayName,
            username = account.username,
            skillRating = account.player.skillRating,
            totpEnabled = account.totpEnabled
        )
    }
}
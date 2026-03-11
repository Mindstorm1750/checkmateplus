package org.checkmateplus.playerservice.services

import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import org.springframework.stereotype.Service
import java.util.Base64

@Service
class TotpService {

    private val secretGenerator = DefaultSecretGenerator(64)
    private val codeGenerator = DefaultCodeGenerator()
    private val timeProvider = SystemTimeProvider()
    private val codeVerifier = DefaultCodeVerifier(codeGenerator, timeProvider).apply {
        setAllowedTimePeriodDiscrepancy(2) // allow 2 periods drift
    }

    fun generateSecret(): String = secretGenerator.generate()

    fun generateQrUri(username: String, secret: String): String {
        return QrData.Builder()
            .label(username)
            .secret(secret)
            .issuer("CheckMatePlus")
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build()
            .let { data ->
                ZxingPngQrGenerator().generate(data)
                    .let { "data:image/png;base64," + Base64.getEncoder().encodeToString(it) }
            }
    }

    fun verify(secret: String, code: String): Boolean {
        return try {
            codeVerifier.isValidCode(secret, code)
        } catch (e: Exception) {
            false
        }
    }
}
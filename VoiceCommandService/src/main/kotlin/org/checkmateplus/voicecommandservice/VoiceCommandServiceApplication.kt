package org.checkmateplus.voicecommandservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
/**
 * Main entry point for the VoiceCommandService application.
 */
@SpringBootApplication
class VoiceCommandServiceApplication

fun main(args: Array<String>) {
    runApplication<VoiceCommandServiceApplication>(*args)
}

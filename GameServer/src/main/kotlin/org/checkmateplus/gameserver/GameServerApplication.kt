package org.checkmateplus.gameserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration

/**
 * Main entry point for the Game Server application.
 * This Spring Boot application handles chess matches and game logic.
 */
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class GameServerApplication

fun main(args: Array<String>) {
    runApplication<GameServerApplication>(*args)
}

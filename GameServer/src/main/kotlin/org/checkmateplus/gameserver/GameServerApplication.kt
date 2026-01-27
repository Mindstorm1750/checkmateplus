package org.checkmateplus.gameserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
/**
 * Main entry point for the Game Server application.
 * This Spring Boot application handles chess matches and game logic.
 */
@SpringBootApplication
class GameServerApplication

fun main(args: Array<String>) {
    runApplication<GameServerApplication>(*args)
}

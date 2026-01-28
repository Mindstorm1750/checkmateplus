package org.checkmateplus.gameservermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main entry point for the Game Server Manager application.
 * This service manages a pool of Game Servers and handles match allocation requests.
 */
@SpringBootApplication
class GameServerManagerApplication

fun main(args: Array<String>) {
    runApplication<GameServerManagerApplication>(*args)
}

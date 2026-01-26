package org.chessmateplus.matchmakingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main entry point for the Matchmaking Service application.
 * This service handles player matchmaking queues and coordinates server allocations.
 */
@SpringBootApplication
class MatchmakingServiceApplication

fun main(args: Array<String>) {
    runApplication<MatchmakingServiceApplication>(*args)
}

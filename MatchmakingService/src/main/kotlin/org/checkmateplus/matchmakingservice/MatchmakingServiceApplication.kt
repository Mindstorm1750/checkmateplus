package org.checkmateplus.matchmakingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Main entry point for the Matchmaking Service application.
 * This service handles player matchmaking queues and coordinates server allocations.
 */
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableScheduling
class MatchmakingServiceApplication

fun main(args: Array<String>) {
    runApplication<MatchmakingServiceApplication>(*args)
}

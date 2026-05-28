package org.checkmateplus.playerservice

import org.checkmateplus.playerservice.configs.PlayerServiceRuntimeHints
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@ImportRuntimeHints(PlayerServiceRuntimeHints::class)
@EnableScheduling
class PlayerServiceApplication

fun main(args: Array<String>) {
    runApplication<PlayerServiceApplication>(*args)
}

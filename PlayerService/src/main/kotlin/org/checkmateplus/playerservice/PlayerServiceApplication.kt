package org.checkmateplus.playerservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PlayerServiceApplication

fun main(args: Array<String>) {
    runApplication<PlayerServiceApplication>(*args)
}

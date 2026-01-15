package uk.ac.kent.matchmakingservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MatchmakingServiceApplication

fun main(args: Array<String>) {
    runApplication<MatchmakingServiceApplication>(*args)
}

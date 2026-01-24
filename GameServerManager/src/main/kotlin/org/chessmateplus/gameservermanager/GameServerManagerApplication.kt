package org.chessmateplus.gameservermanager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GameServerManagerApplication

fun main(args: Array<String>) {
    runApplication<GameServerManagerApplication>(*args)
}

package org.checkmateplus.stockfishengine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class StockfishEngineApplication

fun main(args: Array<String>) {
    runApplication<StockfishEngineApplication>(*args)
}

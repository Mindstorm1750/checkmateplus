package org.checkmateplus.stockfishengine.configs

import jakarta.annotation.PreDestroy
import org.checkmateplus.stockfishengine.engine.StockfishDriver
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions

@Configuration
class StockfishConfig(
    @Value("\${stockfish.executable.path:}") private val configuredPath: String,
    @Value("\${stockfish.threads:1}") private val threads: Int,
    @Value("\${stockfish.hash-mb:128}") private val hashMb: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var driverRef: StockfishDriver? = null

    @Bean
    fun stockfishDriver(): StockfishDriver {
        val resolved = configuredPath.ifBlank { extractBundledExecutable() }
        logger.info("Resolved Stockfish executable: {}", resolved)

        val driver = StockfishDriver(resolved)
        check(driver.start()) { "Failed to start Stockfish at $resolved" }
        driver.setOption("Threads", threads.toString())
        driver.setOption("Hash", hashMb.toString())
        driverRef = driver
        return driver
    }

    @PreDestroy
    fun shutdown() {
        driverRef?.close()
    }

    private fun extractBundledExecutable(): String {
        val osName = System.getProperty("os.name").lowercase()
        val (resource, fileName) = when {
            osName.contains("win") -> "/stockfishExecutables/windows/stockfish-windows-x86-64-avx2.exe" to "stockfish.exe"
            else -> "/stockfishExecutables/linux/stockfish-ubuntu-x86-64-avx2" to "stockfish"
        }

        val stream = javaClass.getResourceAsStream(resource)
            ?: error("Bundled Stockfish executable not found at $resource")

        val tempDir = Files.createTempDirectory("stockfish-engine")
        val target = tempDir.resolve(fileName)
        stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
        makeExecutable(target)
        target.toFile().deleteOnExit()
        tempDir.toFile().deleteOnExit()
        return target.toAbsolutePath().toString()
    }

    private fun makeExecutable(path: Path) {
        val file = path.toFile()
        if (!file.setExecutable(true, false)) {
            try {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"))
            } catch (_: UnsupportedOperationException) {
            }
        }
    }
}

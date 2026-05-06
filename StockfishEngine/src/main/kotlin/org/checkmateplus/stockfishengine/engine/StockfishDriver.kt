package org.checkmateplus.stockfishengine.engine

import org.checkmateplus.stockfishengine.models.stockfishTypes.AnalysisResult
import org.checkmateplus.stockfishengine.models.stockfishTypes.Line
import org.checkmateplus.stockfishengine.models.stockfishTypes.SearchLimit
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class StockfishDriver(private val executablePath: String) : AutoCloseable {

    private val logger = LoggerFactory.getLogger(javaClass)

    private var engineProcess: Process? = null
    private var processWriter: OutputStreamWriter? = null
    private val output = LinkedBlockingQueue<String>()
    private val readerStopped = AtomicBoolean(false)
    private var readerThread: Thread? = null

    private val writeLock = Any()
    private var currentMultiPv: Int = 1

    fun start(): Boolean {
        try {
            logger.info("Starting engine: {}", executablePath)
            val proc = ProcessBuilder(executablePath)
                .redirectErrorStream(true)
                .start()
            engineProcess = proc
            processWriter = OutputStreamWriter(proc.outputStream)

            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            readerThread = thread(start = true, isDaemon = true, name = "stockfish-reader") {
                try {
                    while (true) {
                        val line = reader.readLine() ?: break
                        output.put(line)
                    }
                } catch (e: IOException) {
                    logger.debug("Reader thread closed: {}", e.message)
                } finally {
                    readerStopped.set(true)
                }
            }

            sendRaw("uci")
            drainUntil("uciok", timeoutMs = 5_000)
            syncReady()
        } catch (e: Exception) {
            logger.warn("Failed to start engine", e)
            return false
        }
        return true
    }

    override fun close() {
        val proc = engineProcess ?: return
        try {
            if (proc.isAlive) sendRaw("quit")
        } catch (_: Exception) {
        }
        try {
            readerThread?.join(2_000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        if (proc.isAlive) proc.destroyForcibly()
        try { processWriter?.close() } catch (_: IOException) {}
        engineProcess = null
        processWriter = null
        readerThread = null
    }

    fun setOption(name: String, value: String) {
        sendRaw("setoption name $name value $value")
        if (name.equals("MultiPV", ignoreCase = true)) {
            currentMultiPv = value.toIntOrNull() ?: currentMultiPv
        }
        syncReady()
    }

    fun newGame() {
        sendRaw("ucinewgame")
        syncReady()
    }

    fun setPosition(fen: String, moves: List<String> = emptyList()) {
        val cmd = buildString {
            append("position fen ").append(fen)
            if (moves.isNotEmpty()) append(" moves ").append(moves.joinToString(" "))
        }
        sendRaw(cmd)
    }

    fun setStartPosition(moves: List<String> = emptyList()) {
        val cmd = buildString {
            append("position startpos")
            if (moves.isNotEmpty()) append(" moves ").append(moves.joinToString(" "))
        }
        sendRaw(cmd)
    }

    fun analyse(limit: SearchLimit, multiPv: Int = 1): AnalysisResult {
        require(multiPv >= 1) { "multiPv must be >= 1" }
        if (multiPv != currentMultiPv) setOption("MultiPV", multiPv.toString())

        sendRaw(buildGoCommand(limit))

        val byMultiPv = sortedMapOf<Int, Line>()
        var best: String? = null
        var ponder: String? = null

        val deadlineMs = goDeadlineMs(limit)
        while (true) {
            ensureAlive()
            val line = output.poll(1, TimeUnit.SECONDS) ?: continue
            when {
                line.startsWith("bestmove ") -> {
                    InfoParser.parseBestMove(line)?.let { (b, p) -> best = b; ponder = p }
                    break
                }
                line.startsWith("info ") -> {
                    InfoParser.parseInfoLine(line)?.let { byMultiPv[it.multipv] = it }
                }
            }
            if (System.currentTimeMillis() > deadlineMs) {
                throw IllegalStateException("Stockfish search exceeded hard deadline without bestmove")
            }
        }

        return AnalysisResult(
            bestMove = best,
            ponder = ponder,
            lines = byMultiPv.values.toList(),
        )
    }

    fun bestMove(fen: String, movetimeMs: Int, skillLevel: Int? = null): String? =
        oneShot(fen, SearchLimit.Movetime(movetimeMs), skillLevel)

    fun bestMoveOnClock(fen: String, clock: SearchLimit.Clock, skillLevel: Int? = null): String? =
        oneShot(fen, clock, skillLevel)

    fun legalMoves(fen: String): List<String> {
        setPosition(fen)
        sendRaw("go perft 1")
        val perftLine = Regex("^([a-h][1-8][a-h][1-8][qrbn]?): \\d+$")
        val moves = mutableListOf<String>()
        val deadlineMs = System.currentTimeMillis() + 5_000
        while (true) {
            ensureAlive()
            val line = output.poll(1, TimeUnit.SECONDS) ?: continue
            val trimmed = line.trim()
            perftLine.find(trimmed)?.let { moves.add(it.groupValues[1]) }
            if (trimmed.startsWith("Nodes searched:")) break
            if (System.currentTimeMillis() > deadlineMs) {
                throw IllegalStateException("perft did not complete within 5s")
            }
        }
        return moves
    }

    private fun oneShot(fen: String, limit: SearchLimit, skillLevel: Int?): String? {
        val previousSkill = if (skillLevel != null) {
            setOption("Skill Level", skillLevel.coerceIn(0, 20).toString())
            true
        } else false

        return try {
            newGame()
            setPosition(fen)
            analyse(limit).bestMove
        } finally {
            if (previousSkill) setOption("Skill Level", "20")
        }
    }

    private fun buildGoCommand(limit: SearchLimit): String = when (limit) {
        is SearchLimit.Depth -> "go depth ${limit.plies}"
        is SearchLimit.Movetime -> "go movetime ${limit.ms}"
        is SearchLimit.Nodes -> "go nodes ${limit.n}"
        is SearchLimit.Clock -> buildString {
            append("go wtime ").append(limit.wtimeMs)
            append(" btime ").append(limit.btimeMs)
            append(" winc ").append(limit.wincMs)
            append(" binc ").append(limit.bincMs)
            limit.movestogo?.let { append(" movestogo ").append(it) }
        }
    }

    private fun goDeadlineMs(limit: SearchLimit): Long {
        val budgetMs: Long = when (limit) {
            is SearchLimit.Movetime -> limit.ms.toLong() + 5_000
            is SearchLimit.Clock -> maxOf(limit.wtimeMs, limit.btimeMs) + 10_000
            is SearchLimit.Depth -> 600_000
            is SearchLimit.Nodes -> 600_000
        }
        return System.currentTimeMillis() + budgetMs
    }

    private fun syncReady() {
        sendRaw("isready")
        drainUntil("readyok", timeoutMs = 5_000)
    }

    private fun drainUntil(token: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            ensureAlive()
            val remaining = deadline - System.currentTimeMillis()
            if (remaining <= 0) throw IllegalStateException("Timed out waiting for '$token'")
            val line = output.poll(remaining, TimeUnit.MILLISECONDS) ?: continue
            if (line.trim() == token) return
        }
    }

    private fun ensureAlive() {
        val proc = engineProcess ?: throw IllegalStateException("Engine not started")
        if (!proc.isAlive && output.isEmpty()) {
            throw IllegalStateException("Stockfish died (exit=${proc.exitValue()})")
        }
    }

    private fun sendRaw(cmd: String) {
        val w = processWriter ?: throw IllegalStateException("Engine not started")
        synchronized(writeLock) {
            w.write(cmd)
            w.write("\n")
            w.flush()
        }
    }
}

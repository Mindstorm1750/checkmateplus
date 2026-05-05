package org.checkmateplus.stockfishengine.engine

import org.checkmateplus.stockfishengine.models.stockfishTypes.Line
import org.checkmateplus.stockfishengine.models.stockfishTypes.Score

internal object InfoParser {

    /**
     * Parse a single UCI `info` line into a [Line], or null if the line is not
     * a scored info line (e.g. `info string …`, or no `score` token present).
     *
     * Token-walks forward — UCI doesn't fix field order, vendors append fields,
     * and `pv` consumes the rest of the line.
     */
    fun parseInfoLine(raw: String): Line? {
        val line = raw.trim()
        if (!line.startsWith("info ")) return null

        val tokens = line.split(' ').filter { it.isNotEmpty() }
        if (tokens.size < 2) return null
        if (tokens[1] == "string") return null

        var depth = -1
        var seldepth = 0
        var multipv = 1
        var cp: Int? = null
        var mate: Int? = null
        var nodes = 0L
        var nps = 0L
        var timeMs = 0L
        var wdl: Triple<Int, Int, Int>? = null
        var pv: List<String> = emptyList()

        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> { depth = tokens[i + 1].toInt(); i += 2 }
                "seldepth" -> { seldepth = tokens[i + 1].toInt(); i += 2 }
                "multipv" -> { multipv = tokens[i + 1].toInt(); i += 2 }
                "score" -> {
                    when (tokens[i + 1]) {
                        "cp" -> { cp = tokens[i + 2].toInt(); i += 3 }
                        "mate" -> { mate = tokens[i + 2].toInt(); i += 3 }
                        else -> i += 2
                    }
                    if (i < tokens.size && (tokens[i] == "lowerbound" || tokens[i] == "upperbound")) i++
                }
                "nodes" -> { nodes = tokens[i + 1].toLong(); i += 2 }
                "nps" -> { nps = tokens[i + 1].toLong(); i += 2 }
                "time" -> { timeMs = tokens[i + 1].toLong(); i += 2 }
                "wdl" -> {
                    wdl = Triple(tokens[i + 1].toInt(), tokens[i + 2].toInt(), tokens[i + 3].toInt())
                    i += 4
                }
                "pv" -> {
                    pv = tokens.subList(i + 1, tokens.size)
                    i = tokens.size
                }
                "hashfull", "currmove", "currmovenumber", "cpuload", "refutation", "currline" -> i += 2
                else -> i++
            }
        }

        if (depth < 0) return null
        if (cp == null && mate == null) return null

        return Line(
            multipv = multipv,
            depth = depth,
            seldepth = seldepth,
            score = Score(cp = cp, mateIn = mate),
            nodes = nodes,
            nps = nps,
            timeMs = timeMs,
            pv = pv,
            wdl = wdl,
        )
    }

    /**
     * `bestmove e2e4 ponder e7e5` → "e2e4" to "e7e5".
     * `bestmove (none)` → null to null.
     */
    fun parseBestMove(raw: String): Pair<String?, String?>? {
        val line = raw.trim()
        if (!line.startsWith("bestmove ")) return null
        val tokens = line.split(' ').filter { it.isNotEmpty() }
        if (tokens.size < 2) return null
        val best = if (tokens[1] == "(none)") null else tokens[1]
        val ponder = if (tokens.size >= 4 && tokens[2] == "ponder") {
            if (tokens[3] == "(none)") null else tokens[3]
        } else null
        return best to ponder
    }
}

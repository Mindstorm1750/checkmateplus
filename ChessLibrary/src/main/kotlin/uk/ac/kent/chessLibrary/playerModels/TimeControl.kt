package uk.ac.kent.chessLibrary.playerModels

import kotlinx.serialization.Serializable

/**
 * Chess time control options
 */
@Serializable
enum class TimeControl(val minutes: Int, val increment: Int) {
    BULLET_1_0(1, 0),
    BULLET_2_1(2, 1),
    BLITZ_3_0(3, 0),
    BLITZ_5_0(5, 0),
    RAPID_10_0(10, 0),
    RAPID_15_10(15, 10),
    CLASSICAL_30_0(30, 0);

    companion object {
        fun fromMinutesAndIncrement(minutes: Int, increment: Int): TimeControl? {
            return entries.find { it.minutes == minutes && it.increment == increment }
        }
    }
}
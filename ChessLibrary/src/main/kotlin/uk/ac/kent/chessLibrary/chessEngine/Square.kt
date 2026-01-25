package org.chessmateplus.chessLibrary.chessEngine

typealias Square = Int // All Squares are now Int - code easier to read
typealias FileRank = Pair<Int, Int>
fun Square.toFileRank(): FileRank = this % 8 to this / 8
fun FileRank.toSquare(): Square {
    val (file, rank) = this
    return rank * 8 + file
}
fun String.toSquare(): Square? {
    if (this.length != 2) return null

    val file = this[0].lowercaseChar() - 'a'
    val rank = this[1] - '1'

    if (file !in 0..7 || rank !in 0..7) return null

    return Pair(file, rank).toSquare()
}

fun Square.toAlgebraic(): String {
    val (file, rank) = this.toFileRank()
    return "${('a' + file)}${rank + 1}"
}

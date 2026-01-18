package uk.ac.kent.chessLibrary.chessEngine

typealias Square = Int // All Squares are now Int - code easier to read

/**
 * FileRank isn't a pair of Squares because Squares have values between 0 and 63,
 * whereas File and Rank are values between 0 and 7
 */
typealias FileRank = Pair<Int, Int>


fun Square.toFileRank(): FileRank = this % 8 to this / 8
fun FileRank.toSquare(): Square {
    val (file, rank) = this
    return rank * 8 + file
}


fun main() {
    val board = Board()   // sets up the starting position
    board.printBoard()    // 👈 shows the board in the console
}

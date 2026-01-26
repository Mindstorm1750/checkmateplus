typealias Square = Int // All Squares are now Int - code easier to read
typealias FileRank = Pair<Int, Int>
fun Square.toFileRank(): FileRank = this % 8 to this / 8
fun FileRank.toSquare(): Square {
    val (file, rank) = this
    return rank * 8 + file
}


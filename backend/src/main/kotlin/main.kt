fun main() {
    val board = Board.startingPosition()
    println("Starting position:")
    board.printBoard()

    // testing moving
    val e2 = Pair(4, 1).toSquare()
    val e4 = Pair(4, 3).toSquare()
    val board2 = board.applyMove(Move(e2, e4))
    board2.printBoard()

    val e3 = Pair(1, 6).toSquare()
    val e8 = Pair(1, 5).toSquare()
    val board3 = board2.applyMove(Move(e3, e8))
    board3.printBoard()

    val move1 = (Pair(3, 0).toSquare())
    val move2 = (Pair(5, 2).toSquare())
    val board4 = board3.applyMove(Move(move1, move2))
    board4.printBoard()



    println(board4.evaluate())




}
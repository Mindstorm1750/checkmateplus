package org.chessmateplus.chessLibrary.chessEngine

fun main() {
    val board = Board.startingPosition()
    println("Starting position:")
    board.printBoard()

    // testing moving
    val e2 = Pair(4, 1).toSquare()
    val e4 = Pair(4, 3).toSquare()
    val board2 = board.applyMove(Move(e2, e4))
    println("After 1. e4:")
    board2.printBoard()

    val e3 = Pair(5, 0).toSquare()
    val e8 = Pair(1, 4).toSquare()
    val board3 = board2.applyMove(Move(e3, e8))
    println("Bishop move")
    board3.printBoard()



}
/**
 * As horrific as this looks, this is the way in TypeScript to define a Array type of specific size.
 * "Piece | null" instead of "Piece?" makes it so that an empty array isn't of type Squares.
 */
export type Squares = [
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
  Piece | null,
]

/**
 * Again, another horror but TypeScript doesn't have numeric range types yet.
 */
export type Square =
  | 0
  | 1
  | 2
  | 3
  | 4
  | 5
  | 6
  | 7
  | 8
  | 9
  | 10
  | 11
  | 12
  | 13
  | 14
  | 15
  | 16
  | 17
  | 18
  | 19
  | 20
  | 21
  | 22
  | 23
  | 24
  | 25
  | 26
  | 27
  | 28
  | 29
  | 30
  | 31
  | 32
  | 33
  | 34
  | 35
  | 36
  | 37
  | 38
  | 39
  | 40
  | 41
  | 42
  | 43
  | 44
  | 45
  | 46
  | 47
  | 48
  | 49
  | 50
  | 51
  | 52
  | 53
  | 54
  | 55
  | 56
  | 57
  | 58
  | 59
  | 60
  | 61
  | 62
  | 63

export type Piece = {
  pieceType: PieceType
  colour: Colour
}

/**
 * Can't use enums because of TSConfig option 'erasableSyntaxOnly'
 */
export type Colour = "WHITE" | "BLACK"

export type PieceType = "PAWN" | "KNIGHT" | "BISHOP" | "ROOK" | "QUEEN" | "KING"

export type CastlingRights = {
  whiteKingSide: Boolean
  whiteQueenSide: Boolean
  blackKingSide: Boolean
  blackQueenSide: Boolean
}

export type Board = {
  squares: Squares
  sideToMove: Colour
  castlingRights: CastlingRights
  enPassantSquare: Square | null
  halfMoveClock: number
  fullMoveNumber: number
}

export const EMPTY_BOARD: Board = {
  squares: [
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
    null,
  ],
  sideToMove: "WHITE",
  castlingRights: {
    whiteKingSide: false,
    whiteQueenSide: false,
    blackKingSide: false,
    blackQueenSide: false,
  },
  enPassantSquare: null,
  halfMoveClock: 0,
  fullMoveNumber: 1,
}

export const EMPTY_PIECE_ORIGINS: Map<Square, Square> = new Map(
  Array.from({ length: 64 }, (_, i) => [i, i] as [Square, Square])
)

export type Move = {
  from: Square
  to: Square
  promotion?: PieceType
}

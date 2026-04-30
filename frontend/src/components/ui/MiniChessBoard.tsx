import { pieceToSvg } from "@/lib/chess.helpers"
import { BOARD_THEMES } from "@/lib/board.theme"
import { usePreferences } from "@/lib/PreferencesProvider"
import type { Board, Square } from "@/types/Chess.type"

export type MiniChessBoardProps = {
  boardState: Board
  whitePerspective?: boolean
  className?: string
}

const MiniChessBoard = ({
  boardState,
  whitePerspective = true,
  className,
}: MiniChessBoardProps) => {
  const { preferences } = usePreferences()
  const { lightSquare, darkSquare } = BOARD_THEMES[preferences.boardColour]

  const squaresView = whitePerspective
    ? boardState.squares
    : boardState.squares.toReversed()

  return (
    <div
      className={`relative grid aspect-square w-full grid-cols-8 grid-rows-8 ${className ?? ""}`}
    >
      {squaresView.map((piece, index) => {
        const square = (whitePerspective ? index : 63 - index) as Square
        const row = Math.floor(index / 8)
        const column = index % 8
        const isLightSquare = (row + column) % 2 === 0
        const squareColour = isLightSquare ? lightSquare : darkSquare

        return (
          <div
            key={`mini-square-${square}`}
            className={`${squareColour} relative flex items-center justify-center select-none`}
          >
            {piece && (
              <img
                src={pieceToSvg(piece)}
                alt={`${piece.colour} ${piece.pieceType}`}
                className="pointer-events-none h-4/5 w-4/5"
                draggable="false"
              />
            )}
          </div>
        )
      })}
    </div>
  )
}

export default MiniChessBoard

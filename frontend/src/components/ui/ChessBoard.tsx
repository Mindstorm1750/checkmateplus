import { pieceToSvg } from "@/lib/chess.helpers";
import type { Board, Move, Square } from "@/types/Chess.type"
import { useState } from "react";

type chessBoardProps = {
    moveCallback: (move: Move) => void,
    boardState: Board,
    whitePerspective?: Boolean,
}

const ChessBoard = ({ moveCallback, boardState, whitePerspective = true }: chessBoardProps) => {
    const [squareClicked, setSquareClicked] = useState<Square | null>();
    const squaresView = whitePerspective ? boardState.squares : boardState.squares.toReversed();

    /**
     * TODO have legal move checks
     * @param square 
     */
    const handleClick = (square: Square) => {
        if (!squareClicked) {
            setSquareClicked(square);
        }
        else if (square == squareClicked) {
            setSquareClicked(null);
        }
        else {
            moveCallback({
                from: squareClicked,
                to: square,
                promotion: null // TODO HANDLE PROMOTION POSSIBILITY
            })
            setSquareClicked(null);
        }
    }

    return (
        <div className='grid grid-cols-8 grid-rows-8 w-full aspect-square'>
            {
                squaresView.map((piece, index) => {
                    const square = whitePerspective ? index : 63 - index; 
                    // changing perspective changes the index number of the same square. This is to correct for that.
                    const row = Math.floor(index / 8);
                    const column = index % 8;
                    const isLightSquare = (row + column) % 2 === 0;

                    return (
                        <div
                            key={`square-${index}`}
                            className={`${isLightSquare ? 'bg-amber-200' : 'bg-amber-800'} flex items-center justify-center select-none`}
                            onClick={() => handleClick(square as Square)}>
                            {piece && <img
                                src={pieceToSvg(piece)}
                                alt={`${piece.colour} ${piece.pieceType}`}
                                className="w-4/5 h-4/5 pointer-events-none"
                                draggable="false"
                            />}
                        </div>
                    )
                })
            }
        </div>
    )
}

export default ChessBoard
import { pieceToSvg } from "@/lib/chess.helpers";
import type { Board, Move, Piece } from "@/types/Chess.type"

type chessBoardProps = {
    moveCallback: (move: Move) => null,
    boardState: Board,
    whitePerspective?: Boolean,
}

const ChessBoard = ({ moveCallback, boardState, whitePerspective = true }: chessBoardProps) => {
    const squaresView = whitePerspective ? boardState.squares : boardState.squares.toReversed();

    return (
        <div className='grid grid-cols-8 grid-rows-8 w-full aspect-square'>
            {
                squaresView.map((piece, index) => {
                    const row = Math.floor(index / 8);
                    const column = index % 8;
                    const isLightSquare = (row + column) % 2 === 0;

                    return (
                        <div key={`square-${index}`} className={`${isLightSquare ? 'bg-amber-200' : 'bg-amber-800'} flex items-center justify-center select-none`}>
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
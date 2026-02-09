import { pieceToSvg } from "@/lib/chess.helpers";
import type { Board, Move, Square } from "@/types/Chess.type"
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion"

type chessBoardProps = {
    moveCallback: (move: Move) => void,
    boardState: Board,
    whitePerspective?: Boolean,
}

const ChessBoard = ({ moveCallback, boardState, whitePerspective = true }: chessBoardProps) => {
    const [squareClicked, setSquareClicked] = useState<Square | null>();

    // Used in layoutId to animate pieces. Identifies pieces as they move from boardState to boardState.
    const [pieceOrigins, setPieceOrigins] = useState<Map<Square, Square>>(new Map());

    const squaresView = whitePerspective ? boardState.squares : boardState.squares.toReversed();

    /**
     * TODO have legal move checks
     */
    const handleClick = (square: Square) => {
        if (!squareClicked) {
            setSquareClicked(square);
        }
        else if (square == squareClicked) {
            // unselect a square if clicked twice
            setSquareClicked(null);
        }
        else {
            const move = {
                from: squareClicked,
                to: square,
                promotion: null // TODO HANDLE PROMOTION POSSIBILITY
            };
            setPieceOrigins(prev => {
                const newPieceOrigins = new Map(prev);
                const origin = prev.get(squareClicked) ?? squareClicked;
                newPieceOrigins.set(square, origin);
                newPieceOrigins.delete(squareClicked);
                return newPieceOrigins;
            })
            moveCallback(move);
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
                    const origin = pieceOrigins.get(square as Square) ?? square;

                    return (
                        <div
                            key={`square-${index}`}
                            className={`${isLightSquare ? 'bg-amber-200' : 'bg-amber-800'} flex items-center justify-center select-none relative`}
                            onClick={() => handleClick(square as Square)}>
                            <AnimatePresence>
                                {piece && (
                                    <motion.img
                                        key={`${piece.colour}-${piece.pieceType}-${square}`}
                                        layoutId={`piece-${piece.colour}-${piece.pieceType}-${origin}`}
                                        src={pieceToSvg(piece)}
                                        alt={`${piece.colour} ${piece.pieceType}`}
                                        className="w-4/5 h-4/5 pointer-events-none absolute"
                                        draggable="false"
                                        initial={{ opacity: 0, scale: 1 }}
                                        animate={{ opacity: 1, scale: 1 }}
                                        exit={{ opacity: 0, scale: 1 }}
                                        transition={{ duration: 0.2 }}
                                    />
                                )}
                            </AnimatePresence>
                        </div>
                    )
                })
            }
        </div>
    )
}

export default ChessBoard
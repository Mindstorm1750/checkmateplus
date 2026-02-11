import { legalMoves, pieceToSvg } from "@/lib/chess.helpers";
import type { Board, Move, Square } from "@/types/Chess.type"
import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion"
import { usePreferences } from "@/lib/PreferencesContext";
import { BOARD_THEMES } from "@/lib/board.theme";

type chessBoardProps = {
    moveCallback: (move: Move) => void,
    boardState: Board,
    whitePerspective?: Boolean,
}

const ChessBoard = ({ moveCallback, boardState, whitePerspective = true }: chessBoardProps) => {
    const [squareClicked, setSquareClicked] = useState<Square | null>();

    // Used in layoutId to animate pieces. Identifies pieces as they move from boardState to boardState.
    const [pieceOrigins, setPieceOrigins] = useState<Map<Square, Square>>(new Map());

    const { preferences } = usePreferences();
    const { lightSquare, lightSquareSelected, darkSquare, darkSquareSelected } = BOARD_THEMES[preferences.boardColor];

    const squaresView = whitePerspective ? boardState.squares : boardState.squares.toReversed();
    const moves = legalMoves(boardState);

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

    /**
     * The squares and the pieces are done in two seperate <div> components so that the pieces are always rendered "on top" of the squares.
     * An unfortunate downside is having to do 2 maps.
     */
    return (
        <div className='relative w-full aspect-square'>
            {/* Just the squares */}
            <div className='grid grid-cols-8 grid-rows-8 w-full aspect-square absolute inset-0'>
                {
                    [...Array(64).keys()].map((index) => {
                        const square = whitePerspective ? index : 63 - index;
                        // changing perspective changes the index number of the same square. This is to correct for that.
                        const row = Math.floor(index / 8);
                        const column = index % 8;
                        const isLightSquare = (row + column) % 2 === 0;
                        const isClickedSquare = squareClicked === square;
                        const squareColourTheme = isClickedSquare ?
                            (isLightSquare ? lightSquareSelected : darkSquareSelected) :
                            (isLightSquare ? lightSquare : darkSquare);
                        // canMoveHere is used for the "dots" showing where a selected piece can move.
                        const canMoveHere = moves.filter((move) => move.from === squareClicked).some((move) => move.to === square);

                        return (
                            <div key={`square-${square}`}
                                className={`select-none ${squareColourTheme} relative flex items-center justify-center`}
                                onClick={() => handleClick(square as Square)}>
                                {canMoveHere && (
                                    <div className={`w-1/3 aspect-square rounded-full ${isLightSquare ? lightSquareSelected : darkSquareSelected}`} />
                                )}
                            </div>
                        )
                    })
                }
            </div>

            {/* Just the pieces */}
            <div className='grid grid-cols-8 grid-rows-8 w-full aspect-square absolute inset-0 pointer-events-none'>
                {
                    squaresView.map((piece, index) => {
                        const square = whitePerspective ? index : 63 - index;
                        const origin = pieceOrigins.get(square as Square) ?? square;

                        // for the fun animations
                        const randomAngle = Math.random() * 360;
                        const randomX = Math.cos(randomAngle) * 333;
                        const randomY = Math.sin(randomAngle) * 333;
                        // 333 is arbitrary. It's just a decent looking distance for the pieces to fly away

                        return (
                            <div
                                key={`piece-${square}`}
                                className='flex items-center justify-center relative'>
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
                                            exit={preferences.animationType === 'FUN' ? {
                                                x: randomX,
                                                y: randomY,
                                                rotate: 360,
                                                opacity: 0,
                                                scale: 0.5,
                                            } : {
                                                opacity: 0,
                                                scale: 1
                                            }}
                                            transition={{ duration: 0.3 }}
                                        />
                                    )}
                                </AnimatePresence>
                            </div>
                        )
                    })
                }
            </div>
        </div>
    )
}

export default ChessBoard
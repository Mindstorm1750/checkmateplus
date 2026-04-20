import { isMoveInMoves, legalMoves, pieceToSvg } from "@/lib/chess.helpers"
import type { Board, Move, Square } from "@/types/Chess.type"
import { useState } from "react"
import { motion, AnimatePresence } from "framer-motion"
import { usePreferences } from "@/lib/PreferencesProvider"
import { BOARD_THEMES } from "@/lib/board.theme"
import { Dialog, DialogContent, DialogHeader } from "./dialog"
import { DialogDescription } from "@radix-ui/react-dialog"
import type { MatchStateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage"
import { Button } from "./button"

export type ChessBoardProps = {
    moveCallback: (move: Move) => void,
    boardState: Board,
    whitePerspective?: boolean,
    isPlayersTurn: boolean, // used for stopping the player from making moves when it isn't their turn
    isGameOver: boolean, // used for clocks and also stopping player from making moves
    isCheck: boolean, // used to highlight a King in check with a red glow
    clockState?: Pick<MatchStateMessage, "whitePlayerTime" | "blackPlayerTime">,
    pieceOrigins: Map<Square, Square>
    lastMove?: Move, // used to highlight the most recent move played
    usernames?: Usernames,
}

export type Usernames = { whitePlayerName: string; blackPlayerName: string };

const ChessBoard = ({
                        moveCallback,
                        boardState,
                        whitePerspective = true,
                        isPlayersTurn,
                        isGameOver,
                        isCheck,
                        clockState = {whitePlayerTime: 0, blackPlayerTime: 0},
                        pieceOrigins,
                        lastMove,
                        usernames,
                        children
                    }: React.PropsWithChildren<ChessBoardProps>) => {
    const [squareClicked, setSquareClicked] = useState<Square | null>();
    const [askForPromotion, setAskForPromotion] = useState<boolean>(false);
    const [promotionSquare, setPromotionSquare] = useState<Square | null>();

    const {preferences} = usePreferences();
    const {lightSquare, lightSquareHighlighted, darkSquare, darkSquareHighlighted} = BOARD_THEMES[preferences.boardColour];

    const squaresView = whitePerspective ? boardState.squares : boardState.squares.toReversed();
    const moves = legalMoves(boardState);

    const isInteractable = isPlayersTurn && !isGameOver;

  const handleClick = (square: Square) => {
    if (!isInteractable) {
      return // it's not the player's turn
    }
    if (squareClicked === null || squareClicked === undefined) {
      // simply doing !squareClicked causes bugs with the zeroth square as !0 is true.
      setSquareClicked(square)
    } else if (square == squareClicked) {
      // unselect a square if clicked twice
      setSquareClicked(null)
    } else {
      const attemptedMove: Move = {
        from: squareClicked,
        to: square,
      }
      if (isMoveInMoves(attemptedMove, moves)) {
        moveCallback(attemptedMove)
        setSquareClicked(null)
      } else if (
        isMoveInMoves({ ...attemptedMove, promotion: "QUEEN" }, moves)
      ) {
        // attempted move is actually to promote a pawn
        // need to open a dialog
        setPromotionSquare(square)
        setAskForPromotion(true)
      } else {
        // move is illegal or invalid
        setSquareClicked(null)
      }
    }
  }

    /**
     * The squares and the pieces are done in two seperate <div> components so that the pieces are always rendered "on top" of the squares.
     * An unfortunate downside is having to do 2 maps.
     */
    return (
        <Dialog open={askForPromotion}>
            <div className='flex flex-col w-full'>
                <div className='flex flex-row items-center'>
                    <Clock topClock={true} time={whitePerspective ? clockState.blackPlayerTime : clockState.whitePlayerTime}
                           isActiveClock={!isPlayersTurn} isGameOver={isGameOver}/>
                    {usernames && <div className='w-1/4 flex justify-center'><p>{whitePerspective ? usernames.blackPlayerName : usernames.whitePlayerName}</p></div>}
                </div>
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
                                const isLastMoveSquare = lastMove && (square === lastMove.from || square === lastMove.to);
                                const squareColourTheme = isClickedSquare || isLastMoveSquare ?
                                    (isLightSquare ? lightSquareHighlighted : darkSquareHighlighted) :
                                    (isLightSquare ? lightSquare : darkSquare);
                                // canMoveHere is used for the "dots" showing where a selected piece can move.
                                const canMoveHere = moves.filter((move) => move.from === squareClicked).some((move) => move.to === square);

                                return (
                                    <div key={`square-${square}`}
                                         className={`select-none ${squareColourTheme} relative flex items-center justify-center`}
                                         onClick={() => handleClick(square as Square)}>
                                        {canMoveHere && (
                                            <div
                                                className={`w-1/3 aspect-square rounded-full ${isLightSquare ? lightSquareHighlighted : darkSquareHighlighted}`}/>
                                        )}
                                    </div>
                                )
                            })
                        }
                    </div>

          {/* Just the pieces */}
          <div className="pointer-events-none absolute inset-0 grid aspect-square w-full grid-cols-8 grid-rows-8">
            {squaresView.map((piece, index) => {
              const square = whitePerspective ? index : 63 - index
              const origin = pieceOrigins.get(square as Square)

                                // for the fun animations
                                const randomAngle = -135 + 45 * Math.random();
                                const randomX = Math.cos(randomAngle) * 333;
                                const randomY = Math.sin(randomAngle) * 333;
                                // 333 is arbitrary. It's just a decent looking distance for the pieces to fly away
                                const checkHighlight = isCheck && piece?.pieceType === 'KING' && piece.colour === boardState.sideToMove;

                                return (
                                    <div
                                        key={`piece-${square}`}
                                        className='flex items-center justify-center relative'>
                                        {checkHighlight && (
                                            <div
                                                className="absolute inset-0 pointer-events-none z-0"
                                                style={{ background: 'radial-gradient(circle, rgba(231,0,11,1) 0%, rgba(231,0,11,0) 100%)' }}
                                            />
                                        )}
                                        <AnimatePresence>
                                            {piece && (
                                                <motion.img
                                                    key={`${piece.colour}-${piece.pieceType}-${origin}`}
                                                    layoutId={`piece-${piece.colour}-${piece.pieceType}-${origin}`}
                                                    src={pieceToSvg(piece)}
                                                    alt={`${piece.colour} ${piece.pieceType}`}
                                                    className="w-4/5 h-4/5 pointer-events-none absolute"
                                                    draggable="false"
                                                    initial={{opacity: 0, scale: 1}}
                                                    animate={{opacity: 1, scale: 1}}
                                                    exit={preferences.animationType === 'FUN' ? {
                                                        x: randomX,
                                                        y: randomY,
                                                        rotate: 540,
                                                        opacity: 0,
                                                        scale: 0.5,
                                                    } : {
                                                        opacity: 0,
                                                        scale: 1
                                                    }}
                                                    transition={{duration: 0.3}}
                                                />
                                            )}
                                        </AnimatePresence>
                                    </div>
                                )
                            })
                        }
                    </div>
                </div>
                <div className="flex items-center justify-between">
                    <Clock topClock={false}
                           time={whitePerspective ? clockState.whitePlayerTime : clockState.blackPlayerTime}
                           isActiveClock={isPlayersTurn} isGameOver={isGameOver}/>
                    {usernames && <div className='flex w-1/4 justify-center'><p>{whitePerspective ? usernames.whitePlayerName : usernames.blackPlayerName}</p></div>}
                    <div className='ml-auto'>{children}</div>
                </div>
            </div>
            <DialogContent showCloseButton={false}>
                <DialogHeader>
                    <DialogDescription>
                        <p>Choose piece to promote to:</p>
                        <div className='flex gap-1 justify-left mt-4'>
                            {
                                (['QUEEN', 'ROOK', 'BISHOP', 'KNIGHT'] as const).map((pieceType) => (
                                    <Button
                                        key={pieceType}
                                        className='w-16 h-16 p-1'
                                        onClick={
                                            () => {
                                                if (squareClicked && promotionSquare) {
                                                    moveCallback({
                                                        from: squareClicked,
                                                        to: promotionSquare,
                                                        promotion: pieceType
                                                    });
                                                    setSquareClicked(null);
                                                    setAskForPromotion(false);
                                                }
                                            }
                                        }>
                                        <img
                                            alt={pieceType}
                                            src={pieceToSvg({
                                                pieceType,
                                                colour: boardState.squares[squareClicked!!]?.colour ?? 'WHITE'
                                            })}/>
                                    </Button>
                                ))
                            }
                        </div>
                    </DialogDescription>
                </DialogHeader>
            </DialogContent>
        </Dialog>
    )
}

const Clock = ({
  time,
  topClock,
  isActiveClock,
  isGameOver,
}: {
  time: number
  topClock: boolean
  isActiveClock: boolean
  isGameOver: boolean
}) => {
  const formatter = (str: string) => {
    return `${str.length < 2 ? "0" : ""}${str}`
  }
  const minutes = formatter(Math.floor(time / 60).toString())
  const seconds = formatter((time % 60).toString())

    return (
        <div
            className={`${topClock ? 'rounded-t-lg' : 'rounded-b-lg'} ${isActiveClock && !isGameOver && 'bg-green-900'} ${isGameOver && time <= 0 && 'bg-red-900'} flex justify-center items-center text-3xl border-2 text-center p-2 w-1/4`}>{`${minutes}:${seconds}`}</div>
    )
}

export default ChessBoard

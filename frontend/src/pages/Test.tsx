import Layout from "@/components/Layout"
import { Button } from "@/components/ui/button"
import ChessBoard from "@/components/ui/ChessBoard"
import { applyMove, fenToBoard, squareToAlgebraic, STARTING_FEN } from "@/lib/chess.helpers"
import type { Board, Move } from "@/types/Chess.type"
import { useState } from "react"

function Test() {
    const [whitePerspective, setWhitePerspective] = useState<Boolean>(true);
    const [boardState, setBoardState] = useState<Board>(fenToBoard(STARTING_FEN));
    
    // just for testing right now
    const [chessBoardMove, setChessBoardMove] = useState<Move | null>(null);

    const handleMove = (move: Move) => {
        setChessBoardMove(move);
        setBoardState(applyMove(boardState, move))
    }

    return (
        <Layout>
            <Button onClick={() => setWhitePerspective(!whitePerspective)}>Flip orientation</Button>
            <p>{chessBoardMove ? `Move: ${squareToAlgebraic(chessBoardMove.from)} to ${squareToAlgebraic(chessBoardMove.to)}${chessBoardMove.promotion ? ` promote to ${chessBoardMove.promotion}` : ''}` : 'No move yet'}</p>
            <ChessBoard boardState={boardState} moveCallback={handleMove} whitePerspective={whitePerspective} />
        </Layout>
    )

}

export default Test
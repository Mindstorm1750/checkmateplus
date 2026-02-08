import Layout from "@/components/Layout"
import { Button } from "@/components/ui/button"
import ChessBoard from "@/components/ui/ChessBoard"
import { fenToBoard, squareToAlgebraic, STARTING_FEN } from "@/lib/chess.helpers"
import type { Move } from "@/types/Chess.type"
import { useState } from "react"

function Test() {
    const [whitePerspective, setWhitePerspective] = useState<Boolean>(true);
    
    // just for testing right now
    const [chessBoardMove, setChessBoardMove] = useState<Move | null>(null);


    return (
        <Layout>
            <Button onClick={() => setWhitePerspective(!whitePerspective)}>Flip orientation</Button>
            <p>{chessBoardMove ? `Move: ${squareToAlgebraic(chessBoardMove.from)} to ${squareToAlgebraic(chessBoardMove.to)}${chessBoardMove.promotion && ` promote to ${chessBoardMove.promotion}`}` : 'No move yet'}</p>
            <ChessBoard boardState={fenToBoard(STARTING_FEN)} moveCallback={(move) => setChessBoardMove(move)} whitePerspective={whitePerspective} />
        </Layout>
    )

}

export default Test
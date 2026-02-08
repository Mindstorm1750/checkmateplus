import Layout from "@/components/Layout"
import { Button } from "@/components/ui/button"
import ChessBoard from "@/components/ui/ChessBoard"
import { fenToBoard, STARTING_FEN } from "@/lib/chess.helpers"
import { useState } from "react"

function Test() {
    const [whitePerspective, setWhitePerspective] = useState<Boolean>(true);

    return (
        <Layout>
            <Button onClick={() => setWhitePerspective(!whitePerspective)}>Flip orientation</Button>
            <ChessBoard boardState={fenToBoard(STARTING_FEN)} moveCallback={(move) => null} whitePerspective={whitePerspective} />
        </Layout>
    )

}

export default Test
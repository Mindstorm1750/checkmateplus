import Layout from "@/components/Layout"
import ChessBoard from "@/components/ui/ChessBoard"
import { fenToBoard, STARTING_FEN } from "@/lib/chess.helpers"
import { EMPTY_BOARD } from "@/types/Chess.type"

function Test() {
    return (
        <Layout>
            <ChessBoard boardState={fenToBoard(STARTING_FEN)} moveCallback={(move) => null} />
        </Layout>
    )

}

export default Test
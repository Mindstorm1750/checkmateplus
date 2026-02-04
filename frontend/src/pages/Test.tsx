import Layout from "@/components/Layout"
import ChessBoard from "@/components/ui/ChessBoard"
import { EMPTY_BOARD } from "@/types/Chess.type"

function Test() {
    return (
        <Layout>
            <ChessBoard boardState={EMPTY_BOARD} moveCallback={(move) => null} />
        </Layout>
    )

}

export default Test
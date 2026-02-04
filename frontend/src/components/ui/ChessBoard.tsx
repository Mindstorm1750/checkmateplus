import type { Board, Move } from "@/types/Chess.type"

const ChessBoard = ({ moveCallback, boardState }: { moveCallback: (move: Move) => null, boardState: Board }) => {

    return (<div className='grid grid-cols-8 w-full aspect-square'>
        {boardState.squares.map((piece, index) => {
            const row = Math.floor(index / 8);
            const column = index % 8;
            const isLightSquare = (row + column) % 2 === 0;

            return (
                <div className={`${isLightSquare ? 'bg-amber-200' : 'bg-amber-800'} ''`}>
                    {index}
                </div>
            )
        })
        }
    </div>)
}

export default ChessBoard
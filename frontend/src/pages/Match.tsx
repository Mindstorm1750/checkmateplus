import Layout from "@/components/Layout";
import ChessBoard, { type ChessBoardProps } from "@/components/ui/ChessBoard";
import { algebraicToSquare, fenToBoard, squareToAlgebraic } from "@/lib/chess.helpers";
import type { JoinMatchMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/JoinMatchMessage";
import type { MatchEventMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage";
import type { MatchStateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage";
import type { MoveMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MoveMessage";
import type { TimeUpdateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/TimeUpdateMessage";
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent";
import { EMPTY_BOARD, EMPTY_PIECE_ORIGINS, type Board, type Move, type Square } from "@/types/Chess.type";
import { Client, type IMessage } from "@stomp/stompjs";
import { useEffect, useRef, useState } from "react";
import { useLocation } from "react-router";
import SockJS from 'sockjs-client/dist/sockjs';

function Match() {
    /**
     * using browser History state property to store the match information.
     * Might be a temporary solution OR copy this data to a more persistent form like localStorage.
     */
    const { state }: { state: MatchFoundEvent } = useLocation();
    const { matchId, websocketUrl, playerId } = state; // TODO make playerId persistent
    const [boardState, setBoardState] = useState<Board>(EMPTY_BOARD);
    /**
     * pieceOrigins is used in ChessBoard to animate pieces via framer motion's layoutId. 
     * Identifies pieces as they move from boardState to boardState.
     */
    const [pieceOrigins, setPieceOrigins] = useState<Map<Square, Square>>(EMPTY_PIECE_ORIGINS);
    const [clockState, setClockState] = useState<ChessBoardProps['clockState']>();
    const [isPlayersTurn, setIsPlayersTurn] = useState<Boolean>(false);
    const [isPlayerWhite, setIsPlayerWhite] = useState<Boolean>(true);
    const [isGameOver, setIsGameOver] = useState<Boolean>(false);
    const [gameOverMessage, setGameOverMessage] = useState<String>();
    const stompClient = useRef<Client | null>(null);
    useEffect(() => {
        connect();
    }, [matchId]);

    const connect = () => {
        const socket = new SockJS(`https://${websocketUrl}/match`);
        // websocketUrl should really be http://localhost:8090 or http://127.0.0.1:8090 and '/match' is to be added by frontend here

        const client = new Client({
            webSocketFactory: () => socket,
            debug: (str: string) => console.log(str),
            onConnect: () => {

                // Subscribe to your specific user queue for status updates
                client.subscribe(`/topic/match/${matchId}/events`, (message: IMessage) => {
                    const body: MatchEventMessage = JSON.parse(message.body);
                    console.log("Got match event message: " + body.message);
                    if (body.eventType == 'MATCH_ENDED') {
                        // if the game has ended in checkmate, stalemate, or is a draw
                        setIsGameOver(true);
                        setGameOverMessage(`${body.message} MatchId: ${matchId}`);
                    }
                    if (body.eventType == 'TIME_EXPIRED') {
                        // if the game has ended due to time
                        setIsGameOver(true);
                        setGameOverMessage(`${body.message} MatchId: ${matchId}`);
                    }
                });

                client.subscribe(`/topic/match/${matchId}/time`, (message: IMessage) => {
                    const timeUpdate: TimeUpdateMessage = JSON.parse(message.body);
                    const { whitePlayerTime, blackPlayerTime } = timeUpdate;
                    setClockState({ whitePlayerTime, blackPlayerTime});
                });

                // TODO figure out if unneeded as it never seems to receive messages
                client.subscribe(`/user/queue/match/${matchId}/state`, (message: IMessage) => {
                    const match: MatchStateMessage = JSON.parse(message.body);
                    console.log("Got initial match state message fen: " + match.fen);
                    setBoardState(fenToBoard(match.fen));
                });

                // Subscribe to the match state. This will update our board
                client.subscribe(`/topic/match/${matchId}/state`, (message: IMessage) => {
                    const match: MatchStateMessage = JSON.parse(message.body);
                    console.log(`Is checkmate? ${match.isCheckmate}. Is stalemate? ${match.isStalemate}. Is draw? ${match.isDraw}`);
                    const newBoard = fenToBoard(match.fen)
                    const playerWithTurn = newBoard.sideToMove === 'WHITE' ? match.whitePlayer : match.blackPlayer;
                    if (playerWithTurn?.playerId === playerId) {
                        setIsPlayersTurn(true)
                    }
                    setBoardState(newBoard);
                    if (match.lastMove) {
                        const move = { from: algebraicToSquare(match.lastMove.from), to: algebraicToSquare(match.lastMove.to), promotion: match.lastMove.promotion };
                        setPieceOrigins(prev => {
                            const originOfSquareMovedFrom = prev.get(move.from);
                            if (originOfSquareMovedFrom === undefined || originOfSquareMovedFrom === null) {
                                // if pieceOrigins has already been updated with a move, the square we're moving from will have no origin (be undefined)
                                // therefore we should leave pieceOrigins as is.
                                return prev;
                            }
                            const newPieceOrigins = new Map(prev);
                            const origin = prev.get(move.from) ?? move.from;
                            const whiteCastledKingside = origin == 60 && move.to == 62;
                            const whiteCastledQueenside = origin == 60 && move.to == 58;
                            const blackCastledKingside = origin == 4 && move.to == 6;
                            const blackCastledQueenside = origin == 4 && move.to == 2;
                            if(whiteCastledKingside) {
                                newPieceOrigins.set(61, 63);
                                newPieceOrigins.delete(63);
                            }
                            if(whiteCastledQueenside) {
                                newPieceOrigins.set(59, 56);
                                newPieceOrigins.delete(56);
                            }
                            if(blackCastledKingside) {
                                newPieceOrigins.set(5, 7);
                                newPieceOrigins.delete(7);
                            }
                            if(blackCastledQueenside) {
                                newPieceOrigins.set(3, 0);
                                newPieceOrigins.delete(0);
                            }
                            newPieceOrigins.set(move.to, origin);
                            newPieceOrigins.delete(move.from);
                            return newPieceOrigins;
                        });
                    }
                    setIsPlayerWhite(match.whitePlayer?.playerId === playerId);
                    // This line corrects the player's board perspective every time a move is made. 
                    // Unfortunately, this disturbs the functionality of a "flip perspective" button.
                    // Hence why there currently isn't one.
                });

                joinGame();
            }
        });

        client.activate();
        stompClient.current = client;
    }

    const joinGame = () => {
        if (stompClient.current?.connected) {
            const joinRequest: JoinMatchMessage = { playerId, matchId: matchId, playerName: `UserWhoJoinedAt${Date.now()}` }; // TODO use player's displayName (or playerId if types/classes have changed)

            stompClient.current.publish({
                destination: `/app/match/${matchId}/join`,
                body: JSON.stringify(joinRequest)
            });
        }
    };

    const handleMove: (move: Move) => void = (move) => {
        if (stompClient.current?.connected) {
            setIsPlayersTurn(false);
            const moveRequest: MoveMessage = {
                matchId,
                playerId,
                from: squareToAlgebraic(move.from),
                to: squareToAlgebraic(move.to),
                promotion: move.promotion
            }

            stompClient.current.publish({
                destination: `/app/match/${matchId}/move`,
                body: JSON.stringify(moveRequest)
            });
        }
    }

    return (
        <Layout>
            <div className='w-full flex justify-center'>
                <div className='w-3/4'>
                    {isGameOver && gameOverMessage && <p>{gameOverMessage}</p>}
                    <ChessBoard
                        isInteractable={isPlayersTurn && !isGameOver}
                        boardState={boardState}
                        clockState={clockState}
                        moveCallback={handleMove}
                        whitePerspective={isPlayerWhite}
                        pieceOrigins={pieceOrigins} />
                </div>
            </div>
        </Layout>
    )
}

export default Match
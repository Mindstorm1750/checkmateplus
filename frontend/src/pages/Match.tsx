import Layout from "@/components/Layout";
import ChessBoard, { type ChessBoardProps } from "@/components/ui/ChessBoard";
import { algebraicToSquare, fenToBoard, squareToAlgebraic } from "@/lib/chess.helpers";
import type { JoinMatchMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/JoinMatchMessage";
import type { MatchEventMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage";
import type { MatchStateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage";
import type { MoveMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MoveMessage";
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent";
import { EMPTY_BOARD, type Board, type Move, type Square } from "@/types/Chess.type";
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
    const [pieceOrigins, setPieceOrigins] = useState<Map<Square, Square>>(new Map());
    const [clockState, setClockState] = useState<ChessBoardProps['clockState']>();
    const [isPlayersTurn, setIsPlayersTurn] = useState<Boolean>(false);
    const [isPlayerWhite, setIsPlayerWhite] = useState<Boolean>(true);
    const stompClient = useRef<Client | null>(null);
    useEffect(() => {
        connect();
    }, [matchId]);

    const connect = () => {
        const socket = new SockJS(`http://${websocketUrl}:8090/match`);
        // websocketUrl should really be http://localhost:8090 or http://127.0.0.1:8090 and '/match' is to be added by frontend here

        const client = new Client({
            webSocketFactory: () => socket,
            debug: (str: string) => console.log(str),
            onConnect: () => {

                // Subscribe to your specific user queue for status updates
                client.subscribe(`/topic/match/${matchId}/events`, (message: IMessage) => {
                    const body: MatchEventMessage = JSON.parse(message.body);
                    console.log("Got match event message: " + body.message);
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
                    console.log("Got match state message: " + match.fen);
                    const newBoard = fenToBoard(match.fen)
                    const playerWithTurn = newBoard.sideToMove === 'WHITE' ? match.whitePlayer : match.blackPlayer;
                    if (playerWithTurn?.playerId === playerId) {
                        setIsPlayersTurn(true)
                    }
                    setClockState({ whitePlayerTime: match.whitePlayerTime, blackPlayerTime: match.blackPlayerTime });
                    setBoardState(newBoard);
                    if (match.lastMove) {
                        const move = { from: algebraicToSquare(match.lastMove.from), to: algebraicToSquare(match.lastMove.to), promotion: match.lastMove.promotion };
                        setPieceOrigins(prev => {
                            const newPieceOrigins = new Map(prev);
                            const origin = prev.get(move.from) ?? move.from;
                            newPieceOrigins.set(move.to, origin);
                            newPieceOrigins.delete(move.from);
                            return newPieceOrigins;
                        });
                    }
                    setIsPlayerWhite(match.whitePlayer?.playerId === playerId);
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
                    <ChessBoard
                        isInteractable={isPlayersTurn}
                        boardState={boardState}
                        clockState={clockState}
                        moveCallback={handleMove}
                        whitePerspective={isPlayerWhite}
                        pieceOrigins={pieceOrigins}
                        setPieceOrigins={setPieceOrigins} />
                </div>
            </div>
        </Layout>
    )
}

export default Match
import Layout from "@/components/Layout";
import ChessBoard from "@/components/ui/ChessBoard";
import { fenToBoard, squareToAlgebraic, STARTING_FEN } from "@/lib/chess.helpers";
import type { JoinMatchMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/JoinMatchMessage";
import type { MatchEventMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage";
import type { MatchStateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage";
import type { MoveMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MoveMessage";
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest";
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent";
import { EMPTY_BOARD, type Board, type Move } from "@/types/Chess.type";
import { Client, Stomp, type IMessage } from "@stomp/stompjs";
import { useRef, useState } from "react";
import { useLocation } from "react-router";
import SockJS from 'sockjs-client/dist/sockjs';

function Match() {
    const [boardState, setBoardState] = useState<Board>(fenToBoard(STARTING_FEN));
    const [isPlayersTurn, setIsPlayersTurn] = useState<Boolean>(true); // TODO initialise false and update it from the initial match state
    const stompClient = useRef<Client | null>(null);
    const playerId = 'yoooo'; // TODO actually use a persistent and unique playerId;
    /**
     * using browser History state property to store the match information.
     * Might be a temporary solution OR copy this data to a more persistent form like localStorage.
     */
    const { state }: { state: MatchFoundEvent } = useLocation();
    const { matchId, websocketUrl } = state;

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

                // Subscribe to match found events
                client.subscribe(`/user/queue/match/${matchId}/state`, (message: IMessage) => {
                    const match: MatchStateMessage = JSON.parse(message.body);
                    console.log("Got match state message: ");
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

    connect();

    return (
        <Layout>
            <h1>{`MatchId: ${matchId}, WebsocketUrl: ${websocketUrl}`}</h1>;
            <ChessBoard isInteractable={isPlayersTurn} boardState={boardState} moveCallback={handleMove} />
        </Layout>
    )
}

export default Match
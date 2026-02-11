import Layout from "@/components/Layout";
import type { JoinMatchMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/JoinMatchMessage";
import type { MatchEventMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchEventMessage";
import type { MatchStateMessage } from "@/lib/stompMessageTypes/GameServer/models/messages/MatchStateMessage";
import type { JoinRequest } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/JoinRequest";
import type { MatchFoundEvent } from "@/lib/stompMessageTypes/MatchMakingService/models/messages/MatchFoundEvent";
import { Client, Stomp, type IMessage } from "@stomp/stompjs";
import { useRef } from "react";
import { useLocation } from "react-router";
import SockJS from 'sockjs-client/dist/sockjs';

function Match() {
    /**
     * using browser History state property to store the match information.
     * Might be a temporary solution OR copy this data to a more persistent form like localStorage.
     */
    const { state }: { state: MatchFoundEvent } = useLocation();

    const stompClient = useRef<Client | null>(null);

    const connect = () => {
        const socket = new SockJS(`http://${state.websocketUrl}:8090/match`);
        // websocketUrl should really be http://localhost:8090 or http://127.0.0.1:8090 and '/match' is to be added by frontend here

        const client = new Client({
            webSocketFactory: () => socket,
            debug: (str: string) => console.log(str),
            onConnect: () => {

                // Subscribe to your specific user queue for status updates
                client.subscribe(`/topic/match/${state.matchId}/events`, (message: IMessage) => {
                    const body: MatchEventMessage = JSON.parse(message.body);
                    console.log("Got match event message:" + { body });
                });

                // Subscribe to match found events
                client.subscribe(`/user/queue/match/${state.matchId}/state`, (message: IMessage) => {
                    const match: MatchStateMessage = JSON.parse(message.body);
                    console.log("Got match state message: " + { match });
                });

                joinGame();
            }
        });

        client.activate();
        stompClient.current = client;
    }

    const joinGame = () => {
        if (stompClient.current?.connected) {
            const joinRequest: JoinMatchMessage = { playerId: 'yoooooo', matchId: state.matchId, playerName: `UserWhoJoinedAt${Date.now()}` }; // TODO use player's displayName (or playerId if types/classes have changed)

            stompClient.current.publish({
                destination: `/match/${state.matchId}/join`,
                body: JSON.stringify(joinRequest)
            });
        }
    };

    connect();

    return (
        <Layout>
            <h1>{`MatchId: ${state.matchId}, WebsocketUrl: ${state.websocketUrl}`}</h1>;
        </Layout>
    )
}

export default Match
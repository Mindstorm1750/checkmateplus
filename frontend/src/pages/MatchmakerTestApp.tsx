import React, { useState, useRef } from 'react';
import { Client, type IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client/dist/sockjs';
import { useJwt } from '@/lib/JwtProvider';

// 1. Define types for your messages
interface MatchFoundEvent {
  matchId: string;
  websocketUrl: string;
  opponentName: string;
}

interface QueueStatus {
  status: string;
  message: string;
}

const MatchmakerTestApp: React.FC = () => {
  const [connected, setConnected] = useState<boolean>(false);
  const [playerName, setPlayerName] = useState<string>('');
  const [status, setStatus] = useState<string>('Disconnected');
  const [logs, setLogs] = useState<string[]>([]);
  const [matchData, setMatchData] = useState<MatchFoundEvent | null>(null);
  const jwtContext = useJwt();

  const stompClient = useRef<Client | null>(null);

  const addLog = (msg: string) => {
    setLogs(prev => [`[${new Date().toLocaleTimeString()}] ${msg}`, ...prev]);
  };

  const connect = () => {
    // Note: use the URL of your Spring Boot WebSocket endpoint
    const socket = new SockJS(import.meta.env.VITE_BASE_URL + 'matchmaking');

    const client = new Client({
      webSocketFactory: () => socket,
      connectHeaders: {
        Authorization: `Bearer ${jwtContext.jwt}`
      },
      debug: (str: string) => addLog(str),
      onConnect: () => {
        setConnected(true);
        setStatus('Connected');
        addLog('STOMP connected');

        // Subscribe to your specific user queue for status updates
        client.subscribe('/user/queue/status', (message: IMessage) => {
          const body: QueueStatus = JSON.parse(message.body);
          setStatus(`Status: ${body.status}`);
          addLog(`Queue Update: ${body.message}`);
        });

        // Subscribe to match found events
        client.subscribe('/user/queue/match', (message: IMessage) => {
          const match: MatchFoundEvent = JSON.parse(message.body);
          setMatchData(match);
          setStatus('MATCH FOUND!');
          addLog(`Match Created: ${match.matchId}`);
        });
      },
      onStompError: (frame: { headers: { [x: string]: string; }; }) => {
        addLog('Broker Error: ' + frame.headers['message']);
      },
    });

    client.activate();
    stompClient.current = client;
  };

  const joinQueue = () => {
    if (stompClient.current?.connected) {
      const joinRequest = { displayName: playerName || 'Unknown Player' };
      addLog(`Sending Join Request for: ${joinRequest.displayName}`);

      stompClient.current.publish({
        destination: "/app/join",
        body: JSON.stringify(joinRequest)
      });
    }
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'Arial' }}>
      <h1>Matchmaking Test Tool</h1>
      <div style={{ marginBottom: '20px' }}>
        <input
          placeholder="Display Name"
          value={playerName}
          onChange={(e) => setPlayerName(e.target.value)}
          style={{ padding: '8px', marginRight: '10px' }}
        />
        <button onClick={connect} disabled={connected}>Connect</button>
        <button onClick={joinQueue} disabled={!connected || !!matchData}>Join Queue</button>
      </div>

      <div style={{ display: 'flex', gap: '20px' }}>
        <div style={{ flex: 1, padding: '15px', background: '#f0f0f0' }}>
          <h3>State: {status}</h3>
          {matchData && (
            <div style={{ background: '#d4edda', padding: '10px' }}>
              <p><strong>Opponent:</strong> {matchData.opponentName}</p>
              <p><strong>Server:</strong> {matchData.websocketUrl}</p>
            </div>
          )}
        </div>
        <div style={{ flex: 1, height: '300px', overflowY: 'auto', background: '#333', color: '#fff', padding: '10px' }}>
          {logs.map((log, i) => <div key={i} style={{ fontSize: '12px' }}>{log}</div>)}
        </div>
      </div>
    </div>
  );
};

export default MatchmakerTestApp
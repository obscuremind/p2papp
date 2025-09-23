const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8081 });
const rooms = new Map(); // streamId -> Map(clientId, ws)

function send(ws, obj) { try { ws.send(JSON.stringify(obj)); } catch(_){} }

wss.on('connection', (ws) => {
  ws.on('message', (raw) => {
    let msg = {};
    try { msg = JSON.parse(raw); } catch(e) { return; }

    if (msg.type === 'join' && msg.streamId && msg.senderId) {
      ws.streamId = msg.streamId;
      ws.senderId = msg.senderId;
      if (!rooms.has(ws.streamId)) rooms.set(ws.streamId, new Map());
      const room = rooms.get(ws.streamId);
      room.set(ws.senderId, ws);
      for (const [pid, peer] of room) {
        if (peer !== ws && peer.readyState === 1) {
          send(peer, { type:'negotiate', streamId:ws.streamId, senderId:'server', payload:{ to: ws.senderId }});
        }
      }
      send(ws, { type:'joinAck', streamId: ws.streamId, senderId: 'server' });
      return;
    }

    if (!ws.streamId || !rooms.has(ws.streamId)) return;
    const room = rooms.get(ws.streamId);
    if (msg.payload && msg.payload.to) {
      const peer = room.get(msg.payload.to);
      if (peer && peer.readyState === 1) peer.send(raw.toString());
    }
  });

  ws.on('close', () => {
    if (ws.streamId && rooms.has(ws.streamId)) {
      const room = rooms.get(ws.streamId);
      room.delete(ws.senderId);
      if (room.size === 0) rooms.delete(ws.streamId);
    }
  });
});

console.log('Signaling on ws://0.0.0.0:8081');

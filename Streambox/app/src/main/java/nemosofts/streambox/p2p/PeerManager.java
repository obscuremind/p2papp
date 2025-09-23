package nemosofts.streambox.p2p;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class PeerManager {

    private static PeerManager INSTANCE;

    public static PeerManager getInstance(Context ctx, String signalingUrl, String streamId) {
        if (INSTANCE == null) {
            INSTANCE = new PeerManager(ctx.getApplicationContext(), signalingUrl, streamId);
        }
        return INSTANCE;
    }

    private final String signalingUrl;
    private final String streamId;
    private final String selfId = UUID.randomUUID().toString();

    private final OkHttpClient http = new OkHttpClient();
    private WebSocket ws;
    private final Gson gson = new Gson();

    private PeerConnectionFactory factory;
    private EglBase eglBase;

    private final Map<String, PeerConnection> conns = new ConcurrentHashMap<>();
    private final Map<String, DataChannel> chans = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRtts = new ConcurrentHashMap<>();
    private final Map<String, Long> sent = new ConcurrentHashMap<>();
    private final Map<String, Long> recv = new ConcurrentHashMap<>();
    private final Map<String, String> country = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<SegmentCallback>> waiting = new ConcurrentHashMap<>();
    private final List<Runnable> peersChangedListeners = new CopyOnWriteArrayList<>();
    private Runnable statsListener;

    private PeerManager(Context ctx, String signalingUrl, String streamId) {
        this.signalingUrl = signalingUrl;
        this.streamId = streamId;
        initPeerFactory(ctx);
        connectSignaling();
        startPinger();
    }

    public void setOnPeersChanged(Runnable r) { peersChangedListeners.add(r); }
    public void setOnStatsUpdate(Runnable r) { this.statsListener = r; }

    private void initPeerFactory(Context ctx) {
        eglBase = EglBase.create();
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(ctx).createInitializationOptions());

        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();
    }

    private void connectSignaling() {
        Request req = new Request.Builder().url(signalingUrl).build();
        ws = http.newWebSocket(req, new WebSocketListener() {
            @Override public void onOpen(WebSocket webSocket, okhttp3.Response response) {
                send(new Sig("join", streamId, selfId, null));
            }
            @Override public void onMessage(WebSocket webSocket, String text) {
                Sig s = gson.fromJson(text, Sig.class);
                if (s == null) return;
                if ("negotiate".equals(s.type) && s.payload != null) {
                    startNegotiation(s.payload.to);
                    return;
                }
                if (s.senderId != null && s.senderId.equals(selfId)) return;
                switch (s.type) {
                    case "offer": onOffer(s); break;
                    case "answer": onAnswer(s); break;
                    case "candidate": onCandidate(s); break;
                    default: break;
                }
            }
        });
    }

    private void send(Sig s) { if (ws != null) ws.send(gson.toJson(s)); }

    private PeerConnection createPc(String peerId, boolean initiator) {
        List<PeerConnection.IceServer> ice = new ArrayList<>();
        ice.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        ice.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration cfg = new PeerConnection.RTCConfiguration(ice);
        cfg.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        PeerConnection pc = factory.createPeerConnection(cfg, new PeerConnection.Observer() {
            @Override public void onIceCandidate(IceCandidate candidate) {
                    // pass

                send(new Sig("candidate", streamId, selfId, new Payload(peerId, null, candidate, null)));
            }
            @Override public void onConnectionChange(PeerConnection.PeerConnectionState newState) {
                if (newState == PeerConnection.PeerConnectionState.DISCONNECTED ||
                    newState == PeerConnection.PeerConnectionState.FAILED ||
                    newState == PeerConnection.PeerConnectionState.CLOSED) {
                    conns.remove(peerId);
                    chans.remove(peerId);
                    notifyPeersChanged();
                }
            }
            @Override public void onDataChannel(DataChannel dc) { setupChannel(peerId, dc); }
            @Override public void onSignalingChange(PeerConnection.SignalingState newState) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {}
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {}
                @Override public void onIceCandidatesRemoved(IceCandidate[] candidates) {}
            @Override public void onAddStream(org.webrtc.MediaStream stream) {}
            @Override public void onRemoveStream(org.webrtc.MediaStream stream) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(org.webrtc.RtpReceiver r, org.webrtc.MediaStream[] ms) {}
        });

        if (initiator) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = true;
            DataChannel dc = pc.createDataChannel("seg", init);
            setupChannel(peerId, dc);
        }
        return pc;
    }

    private void setupChannel(String peerId, DataChannel dc) {
        chans.put(peerId, dc);
        dc.registerObserver(new DataChannel.Observer() {
            @Override public void onBufferedAmountChange(long previousAmount) {}
            @Override public void onStateChange() {}
            @Override public void onMessage(DataChannel.Buffer buffer) {
                ByteBuffer buf = buffer.data;
                byte[] bytes = new byte[buf.remaining()];
                buf.get(bytes);
                handleDcMessage(peerId, bytes);
            }
        });
        notifyPeersChanged();
    }

    private void startNegotiation(String otherId) {
        PeerConnection pc = createPc(otherId, true);
        conns.put(otherId, pc);
        MediaConstraints mc = new MediaConstraints();
        pc.createOffer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sd) {
                pc.setLocalDescription(this, sd);
                send(new Sig("offer", streamId, selfId, new Payload(otherId, sd.description, null, null)));
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String s) { Log.e("SDP", "createOffer failed: " + s); }
            @Override public void onSetFailure(String s) {}
        }, mc);
    }

    private void onOffer(Sig s) {
        String pid = s.senderId;
        PeerConnection pc = conns.get(pid);
        if (pc == null) {
            pc = createPc(pid, false);
            conns.put(pid, pc);
        }
        pc.setRemoteDescription(new SimpleSdpObs("setRemote(offer)"),
                new SessionDescription(SessionDescription.Type.OFFER, s.payload.sdp));
        pc.createAnswer(new SdpObserver() {
            @Override public void onCreateSuccess(SessionDescription sd) {
                pc.setLocalDescription(this, sd);
                send(new Sig("answer", streamId, selfId, new Payload(pid, sd.description, null, null)));
            }
            @Override public void onSetSuccess() {}
            @Override public void onCreateFailure(String err) { Log.e("SDP", "createAnswer failed: " + err); }
            @Override public void onSetFailure(String s) {}
        }, new MediaConstraints());
    }

    private void onAnswer(Sig s) {
        String pid = s.senderId;
        PeerConnection pc = conns.get(pid);
        if (pc != null) {
            pc.setRemoteDescription(new SimpleSdpObs("setRemote(answer)"),
                    new SessionDescription(SessionDescription.Type.ANSWER, s.payload.sdp));
        }
    }

    private void onCandidate(Sig s) {
        String pid = s.senderId;
        PeerConnection pc = conns.get(pid);
        if (pc != null && s.payload.cand != null) {
            pc.addIceCandidate(s.payload.cand);
        }
    }

    private void notifyPeersChanged() { for (Runnable r : peersChangedListeners) r.run(); }

    private void startPinger() {
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                long now = System.currentTimeMillis();
                for (Map.Entry<String, DataChannel> e : chans.entrySet()) {
                    String pid = e.getKey();
                    DataChannel dc = e.getValue();
                    if (dc.state() != DataChannel.State.OPEN) continue;
                    MsgPing ping = new MsgPing(now);
                    byte[] b = gson.toJson(ping).getBytes();
                    dc.send(new DataChannel.Buffer(ByteBuffer.wrap(b), false));
                }
            }
        }, 1000, 2000);
    }

    public interface SegmentCallback { void onResult(boolean ok, byte[] bytes); }

    public void requestSegment(SegmentKey key, long timeoutMs, SegmentCallback cb) {
        String best = null;
        long bestRtt = Long.MAX_VALUE;
        for (String pid : chans.keySet()) {
            DataChannel dc = chans.get(pid);
            if (dc.state() != DataChannel.State.OPEN) continue;
            long r = lastRtts.getOrDefault(pid, 9999L);
            if (r < bestRtt) { bestRtt = r; best = pid; }
        }
        if (best == null) { cb.onResult(false, null); return; }

        waiting.computeIfAbsent(key.uri, k -> new CopyOnWriteArrayList<>()).add(cb);

        DataChannel dc = chans.get(best);
        MsgNeed need = new MsgNeed(key.uri);
        byte[] b = gson.toJson(need).getBytes();
        dc.send(new DataChannel.Buffer(ByteBuffer.wrap(b), false));

        new java.util.Timer(true).schedule(new java.util.TimerTask() {
            @Override public void run() {
                CopyOnWriteArrayList<SegmentCallback> list = waiting.remove(key.uri);
                if (list != null) for (SegmentCallback c : list) c.onResult(false, null);
            }
        }, timeoutMs);
    }

    private void handleDcMessage(String from, byte[] bytes) {
        String s = new String(bytes);
        if (s.startsWith("{\"type\":\"ping\"")) {
            MsgPing ping = new Gson().fromJson(s, MsgPing.class);
            DataChannel dc = chans.get(from);
            if (dc != null && dc.state() == DataChannel.State.OPEN) {
                MsgPong pong = new MsgPong(ping.ts);
                dc.send(new DataChannel.Buffer(ByteBuffer.wrap(new Gson().toJson(pong).getBytes()), false));
            }
        } else if (s.startsWith("{\"type\":\"pong\"")) {
            MsgPong pong = new Gson().fromJson(s, MsgPong.class);
            long rtt = Math.max(1, System.currentTimeMillis() - pong.ts);
            lastRtts.put(from, rtt);
            if (statsListener != null) statsListener.run();
        } else if (s.startsWith("{\"type\":\"need\"")) {
            MsgNeed need = new Gson().fromJson(s, MsgNeed.class);
            byte[] payload = HttpFetch.fetchBytes(need.uri);
            DataChannel dc = chans.get(from);
            if (dc != null && dc.state() == DataChannel.State.OPEN && payload != null) {
                MsgPiece piece = new MsgPiece(payload);
                sent.merge(from, (long) payload.length, Long::sum);
                dc.send(new DataChannel.Buffer(ByteBuffer.wrap(new Gson().toJson(piece).getBytes()), false));
                if (statsListener != null) statsListener.run();
            }
        } else if (s.startsWith("{\"type\":\"piece\"")) {
            MsgPiece piece = new Gson().fromJson(s, MsgPiece.class);
            recv.merge(from, (long) piece.bytes.length, Long::sum);
            // deliver to all waiting callbacks (simple model)
            for (Map.Entry<String, CopyOnWriteArrayList<SegmentCallback>> e :
                    new HashMap<>(waiting).entrySet()) {
                CopyOnWriteArrayList<SegmentCallback> list = waiting.remove(e.getKey());
                if (list != null) for (SegmentCallback c : list) c.onResult(true, piece.bytes);
            }
            if (statsListener != null) statsListener.run();
        } else if (s.startsWith("{\"type\":\"hello\"")) {
            MsgHello h = new Gson().fromJson(s, MsgHello.class);
            country.put(from, h.country == null ? "??" : h.country);
            notifyPeersChanged();
        }
    }

    private static class Sig {
        String type;
        String streamId;
        String senderId;
        Payload payload;
        Sig(String type, String streamId, String senderId, Payload payload) {
            this.type = type; this.streamId = streamId; this.senderId = senderId; this.payload = payload;
        }
    }
    private static class Payload {
        String to;
        String sdp;
        IceCandidate cand;
        String msg;
        Payload(String to, String sdp, IceCandidate cand, String msg) {
            this.to = to; this.sdp = sdp; this.cand = cand; this.msg = msg;
        }
    }

    private static class MsgPing { String type="ping"; long ts; MsgPing(long ts){this.ts=ts;} }
    private static class MsgPong { String type="pong"; long ts; MsgPong(long ts){this.ts=ts;} }
    private static class MsgNeed { String type="need"; String uri; MsgNeed(String uri){this.uri=uri;} }
    private static class MsgPiece { String type="piece"; byte[] bytes; MsgPiece(byte[] bytes){this.bytes=bytes;} }
    private static class MsgHello { String type="hello"; String country; }

    static class HttpFetch {
        static byte[] fetchBytes(String url) {
            try {
                okhttp3.Request req = new okhttp3.Request.Builder().url(url).build();
                okhttp3.Response res = new OkHttpClient().newCall(req).execute();
                if (!res.isSuccessful() || res.body() == null) return null;
                return res.body().bytes();
            } catch (Exception e) {
                return null;
            }
        }
    }

    private static class SimpleSdpObs implements SdpObserver {
        private final String tag;
        SimpleSdpObs(String tag) { this.tag = tag; }
        @Override public void onCreateSuccess(SessionDescription sessionDescription) {}
        @Override public void onSetSuccess() {}
        @Override public void onCreateFailure(String s) { Log.e("SDP", tag + " onCreateFailure " + s); }
        @Override public void onSetFailure(String s) { Log.e("SDP", tag + " onSetFailure " + s); }
    }

        public int getPeerCount() { return chans.size(); }
        public long getAverageRtt() {
            if (lastRtts.isEmpty()) return 0;
            long sum = 0;
            int n = 0;
            for (Long v : lastRtts.values()) { sum += v; n++; }
            return n == 0 ? 0 : sum / n;
        }
        public long getTotalRecv() {
            long sum = 0;
            for (Long v : recv.values()) sum += v;
            return sum;
        }
        public long getTotalSent() {
            long sum = 0;
            for (Long v : sent.values()) sum += v;
            return sum;
        }
        public java.util.Map<String,Integer> getCountryCounts() {
            java.util.Map<String,Integer> map = new java.util.HashMap<>();
            for (String c : country.values()) {
                map.put(c, map.getOrDefault(c, 0) + 1);
            }
            return map;
        }
    
}

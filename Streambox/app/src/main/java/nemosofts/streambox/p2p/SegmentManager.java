package nemosofts.streambox.p2p;

import android.content.Context;

import androidx.media3.datasource.DataSpec;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SegmentManager {

    private static final long P2P_TIMEOUT_MS = 800;

    private final PeerManager peerManager;
    private final Map<String, byte[]> cache = new ConcurrentHashMap<>();
    private final Stats stats = new Stats();
    private StatsListener statsListener;

    public SegmentManager(Context ctx, PeerManager peerManager) {
        this.peerManager = peerManager;
        this.peerManager.setOnStatsUpdate(this::onPeerStats);
        this.peerManager.setOnPeersChanged(this::notifyStats);
    }

    public void setStatsListener(StatsListener l) { this.statsListener = l; }

    public CompletableFuture<InputStream> fetchFromPeers(SegmentKey key, DataSpec spec) {
        String k = key.uri;
        if (cache.containsKey(k)) {
            byte[] data = cache.get(k);
            return CompletableFuture.completedFuture(new ByteArrayInputStream(data));
        }

        CompletableFuture<InputStream> fut = new CompletableFuture<>();
        long start = System.nanoTime();

        peerManager.requestSegment(key, P2P_TIMEOUT_MS, (ok, bytes) -> {
            if (ok && bytes != null && bytes.length > 0) {
                cache.put(k, bytes);
                stats.p2pBytes += bytes.length;
                stats.segmentLatencyMs = (System.nanoTime() - start) / 1_000_000;
                notifyStats();
                fut.complete(new ByteArrayInputStream(bytes));
            } else {
                fut.completeExceptionally(new RuntimeException("P2P timeout"));
            }
        });

        return fut;
    }

    public void onHttpFallback(SegmentKey key) {
        stats.httpSegments++;
        notifyStats();
    }

    public void onP2PSuccess(SegmentKey key, long bytes) {
        stats.p2pSegments++;
        notifyStats();
    }

    private void onPeerStats() { notifyStats(); }

    private void notifyStats() {
        if (statsListener == null) return;
        stats.peerCount = peerManager.getPeerCount();
        stats.avgRttMs = peerManager.getAverageRtt();
        stats.recvBytes = peerManager.getTotalRecv();
        stats.sentBytes = peerManager.getTotalSent();
        stats.geoByCountry = peerManager.getCountryCounts();
        statsListener.onStats(stats);
    }

    public interface StatsListener { void onStats(Stats s); }

    public static class Stats {
        public long p2pBytes = 0;
        public long sentBytes = 0;
        public long recvBytes = 0;
        public long httpSegments = 0;
        public long p2pSegments = 0;
        public int peerCount = 0;
        public long avgRttMs = 0;
        public long segmentLatencyMs = 0;
        public Map<String, Integer> geoByCountry = new ConcurrentHashMap<>();
    }
}

package nemosofts.streambox.p2p;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.TransferListener;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class P2PDataSource implements DataSource {

    private final HttpDataSource.Factory httpFactory;
    private final SegmentManager segmentManager;

    private DataSpec dataSpec;
    private InputStream stream;
    private long bytesRemaining = C.LENGTH_UNSET;
    private DataSource httpFallbackDs;

    public P2PDataSource(HttpDataSource.Factory httpFactory, SegmentManager segmentManager) {
        this.httpFactory = httpFactory;
        this.segmentManager = segmentManager;
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        this.dataSpec = dataSpec;
        Uri uri = dataSpec.uri;

        String path = uri.getPath() == null ? "" : uri.getPath();
        boolean looksLikeMediaSegment =
                path.endsWith(".ts") || path.endsWith(".aac") || path.endsWith(".mp4") ||
                path.endsWith(".m4s") || path.contains("seg") || path.contains("chunk");

        if (!looksLikeMediaSegment) {
            httpFallbackDs = httpFactory.createDataSource();
            long len = httpFallbackDs.open(dataSpec);
            bytesRemaining = len;
            stream = new DataSourceInputStream(httpFallbackDs, len);
            return len;
        }

        SegmentKey key = SegmentKey.fromUri(uri);
        CompletableFuture<InputStream> fut = segmentManager.fetchFromPeers(key, dataSpec);
        try {
            stream = fut.get(900, TimeUnit.MILLISECONDS);
            bytesRemaining = stream.available() > 0 ? stream.available() : C.LENGTH_UNSET;
            segmentManager.onP2PSuccess(key, bytesRemaining);
            return bytesRemaining;
        } catch (Exception e) {
            httpFallbackDs = httpFactory.createDataSource();
            long len = httpFallbackDs.open(dataSpec);
            bytesRemaining = len;
            stream = new DataSourceInputStream(httpFallbackDs, len);
            segmentManager.onHttpFallback(key);
            return len;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int readLength) throws IOException {
        int r = stream.read(buffer, offset, readLength);
        return (r == -1) ? C.RESULT_END_OF_INPUT : r;
    }

    @Nullable @Override public Uri getUri() { return dataSpec != null ? dataSpec.uri : null; }

    @Override public void close() throws IOException {
        if (stream != null) stream.close();
        if (httpFallbackDs != null) httpFallbackDs.close();
        stream = null;
        httpFallbackDs = null;
    }

    @Override public void addTransferListener(TransferListener transferListener) {}
}

package nemosofts.streambox.p2p;

import androidx.media3.datasource.DataSource;

import java.io.IOException;
import java.io.InputStream;

final class DataSourceInputStream extends InputStream {
    private final DataSource ds;
    private final long length;
    private long read = 0;

    DataSourceInputStream(DataSource ds, long length) {
        this.ds = ds;
        this.length = length;
    }

    @Override public int read() throws IOException {
        byte[] b = new byte[1];
        int r = read(b, 0, 1);
        return r == -1 ? -1 : (b[0] & 0xff);
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        int r = ds.read(b, off, len);
        if (r == -1) return -1;
        read += r;
        return r;
    }

    @Override public int available() {
        if (length < 0) return 0;
        long rem = length - read;
        return (int) Math.max(0, Math.min(rem, Integer.MAX_VALUE));
    }

    @Override public void close() throws IOException {
        ds.close();
    }
}

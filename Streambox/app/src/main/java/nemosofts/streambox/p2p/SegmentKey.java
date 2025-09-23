package nemosofts.streambox.p2p;

import android.net.Uri;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SegmentKey {
    public final String streamId;
    public final String rendition;
    public final long seq;
    public final String uri;

    private SegmentKey(String streamId, String rendition, long seq, String uri) {
        this.streamId = streamId;
        this.rendition = rendition;
        this.seq = seq;
        this.uri = uri;
    }

    public static SegmentKey fromUri(Uri u) {
        String path = u.getPath() == null ? "" : u.getPath();
        String last = u.getLastPathSegment() == null ? path : u.getLastPathSegment();

        long seq = -1;
        Pattern[] ps = new Pattern[] {
                Pattern.compile(".*?(\\d+)\\.ts$"),
                Pattern.compile(".*?seg[-_](\\d+)"),
                Pattern.compile(".*?chunk[-_](\\d+)"),
                Pattern.compile(".*?index(\\d+)")
        };
        for (Pattern p : ps) {
            Matcher m = p.matcher(last);
            if (m.matches()) {
                try { seq = Long.parseLong(m.group(1)); break; } catch (Exception ignored) {}
            }
        }

        String sid = "s" + Math.abs(path.hashCode());
        String rendition = path.replaceAll(".*/(.*?)$", "$1");
        return new SegmentKey(sid, rendition, seq, u.toString());
    }

    @Override public int hashCode() { return (streamId + "|" + rendition + "|" + seq + "|" + uri).hashCode(); }
    @Override public boolean equals(Object o) {
        if (!(o instanceof SegmentKey)) return false;
        SegmentKey s = (SegmentKey) o;
        return streamId.equals(s.streamId) && rendition.equals(s.rendition) && seq == s.seq && uri.equals(s.uri);
    }

    @Override public String toString() { return streamId + ":" + rendition + ":" + seq; }
}

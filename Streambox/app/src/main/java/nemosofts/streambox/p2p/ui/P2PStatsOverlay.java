package nemosofts.streambox.p2p.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import nemosofts.streambox.p2p.SegmentManager;

import java.util.Locale;
import java.util.Map;

public class P2PStatsOverlay extends FrameLayout {
    private final TextView tv;
    private boolean visible = true;

    public P2PStatsOverlay(Context c, AttributeSet a) {
        super(c, a);
        tv = new TextView(c);
        tv.setTextSize(12f);
        tv.setPadding(16,16,16,16);
        tv.setBackgroundColor(0xAA000000);
        tv.setTextColor(0xFFFFFFFF);
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.END;
        lp.setMargins(16,16,16,16);
        addView(tv, lp);
        setVisibility(View.VISIBLE);
    }

    public void toggle() {
        visible = !visible;
        setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void render(SegmentManager.Stats s) {
        StringBuilder geo = new StringBuilder();
        if (s.geoByCountry != null && !s.geoByCountry.isEmpty()) {
            for (Map.Entry<String,Integer> e : s.geoByCountry.entrySet()) {
                geo.append(e.getKey()).append("(").append(e.getValue()).append(") ");
            }
        }
        String txt = String.format(Locale.US,
                "Peers: %d\nP2P seg: %d  HTTP seg: %d\nSent: %s  Recv: %s\nAvg RTT: %d ms  Seg Lat: %d ms\nCountries: %s",
                s.peerCount, s.p2pSegments, s.httpSegments,
                human(s.sentBytes), human(s.recvBytes),
                s.avgRttMs, s.segmentLatencyMs,
                geo.toString().trim()
        );
        tv.setText(txt);
    }

    private String human(long b) {
        if (b < 1024) return b + " B";
        int exp = (int) (Math.log(b) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format(Locale.US, "%.1f %sB", b / Math.pow(1024, exp), pre);
    }
}

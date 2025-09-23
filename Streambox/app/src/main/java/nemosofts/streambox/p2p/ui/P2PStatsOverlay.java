package nemosofts.streambox.p2p.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import nemosofts.streambox.p2p.SegmentManager;

public class P2PStatsOverlay extends FrameLayout {
    private final LinearLayout card;
    private final TextView statusBadge;
    private final TextView headingView;
    private final TextView peerView;
    private final TextView downValue;
    private final TextView upValue;
    private final TextView latencyValue;
    private final TextView segmentValue;
    private final TextView geoView;
    private final TextView statusView;
    private boolean visible = true;

    public P2PStatsOverlay(Context c, AttributeSet a) {
        super(c, a);
        setClipToPadding(false);

        card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(20), dp(24), dp(20));
        card.setGravity(Gravity.START);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(18));
        bg.setColor(0xE6101216);
        card.setBackground(bg);
        card.setElevation(dp(6));

        LinearLayout headerRow = new LinearLayout(c);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(16));

        statusBadge = buildBadge(c);
        headingView = buildTitle(c);
        headerRow.addView(statusBadge);
        headerRow.addView(headingView);

        peerView = buildLabel(c);

        LinearLayout metricRow = new LinearLayout(c);
        metricRow.setOrientation(LinearLayout.HORIZONTAL);
        metricRow.setGravity(Gravity.CENTER_VERTICAL);
        metricRow.setPadding(0, dp(8), 0, dp(16));

        MetricBlock downloadBlock = new MetricBlock(c, "Download");
        MetricBlock uploadBlock = new MetricBlock(c, "Upload");
        MetricBlock latencyBlock = new MetricBlock(c, "Latency");
        MetricBlock segmentBlock = new MetricBlock(c, "Segments");

        downValue = downloadBlock.value;
        upValue = uploadBlock.value;
        latencyValue = latencyBlock.value;
        segmentValue = segmentBlock.value;

        metricRow.addView(downloadBlock.container);
        metricRow.addView(uploadBlock.container);
        metricRow.addView(latencyBlock.container);
        metricRow.addView(segmentBlock.container);
        ((LinearLayout.LayoutParams) segmentBlock.container.getLayoutParams()).setMargins(0, 0, 0, 0);

        geoView = buildSubtle(c);
        statusView = buildSubtle(c);
        statusView.setTypeface(Typeface.create("sans-serif-medium", Typeface.ITALIC));

        card.addView(headerRow);
        card.addView(peerView);
        card.addView(metricRow);
        card.addView(geoView);
        card.addView(statusView);

        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM | Gravity.END;
        int margin = dp(24);
        lp.setMargins(margin, margin, margin, margin);
        addView(card, lp);

        setVisibility(View.VISIBLE);
    }

    public void toggle() {
        visible = !visible;
        setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void render(SegmentManager.Stats s) {
        boolean hasPeers = s.peerCount > 0;

        statusBadge.setText(hasPeers ? "LIVE" : "IDLE");
        statusBadge.setBackground(buildBadgeBackground(hasPeers));

        headingView.setText("P2P NETWORK");
        headingView.setTextColor(hasPeers ? 0xFFFFFFFF : 0xFFB0BEC5);

        peerView.setText(String.format(Locale.US, "Peers connected: %d", s.peerCount));
        peerView.setTextColor(hasPeers ? 0xFF9BE89E : 0xFFFFC7C7);

        downValue.setText(human(s.recvBytes));
        downValue.setTextColor(0xFFE3F2FD);

        upValue.setText(human(s.sentBytes));
        upValue.setTextColor(0xFFE3F2FD);

        latencyValue.setText(String.format(Locale.US, "%d / %d ms", s.avgRttMs, s.segmentLatencyMs));
        latencyValue.setTextColor(colorForLatency(s.avgRttMs, s.segmentLatencyMs));

        segmentValue.setText(String.format(Locale.US, "%d P2P / %d HTTP", s.p2pSegments, s.httpSegments));
        segmentValue.setTextColor(0xFFE3F2FD);

        String geoText = buildGeoSummary(s.geoByCountry);
        geoView.setText(String.format(Locale.US, "Regions: %s", geoText));
        geoView.setTextColor(0xFF90A4AE);

        statusView.setText(buildStatusLine(s));
        statusView.setTextColor(determineStatusColor(s));
    }

    private TextView buildTitle(Context c) {
        TextView tv = new TextView(c);
        tv.setTextSize(20f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setTextColor(0xFFFFFFFF);
        tv.setPadding(dp(12), 0, 0, 0);
        return tv;
    }

    private TextView buildLabel(Context c) {
        TextView tv = new TextView(c);
        tv.setTextSize(14f);
        tv.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tv.setTextColor(0xFFB0FFC8);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private TextView buildSubtle(Context c) {
        TextView tv = new TextView(c);
        tv.setTextSize(13f);
        tv.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        tv.setTextColor(0xFFB0BEC5);
        tv.setPadding(0, 0, 0, dp(6));
        return tv;
    }

    private TextView buildBadge(Context c) {
        TextView tv = new TextView(c);
        tv.setTextSize(14f);
        tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        tv.setPadding(dp(12), dp(4), dp(12), dp(4));
        tv.setTextColor(0xFF0A1014);
        tv.setGravity(Gravity.CENTER);
        tv.setBackground(buildBadgeBackground(false));
        return tv;
    }

    private GradientDrawable buildBadgeBackground(boolean active) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(12));
        drawable.setColor(active ? 0xFF7CFC5B : 0xFF546E7A);
        return drawable;
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }

    private CharSequence buildGeoSummary(Map<String, Integer> geoByCountry) {
        if (geoByCountry == null || geoByCountry.isEmpty()) {
            return "--";
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(geoByCountry.entrySet());
        Collections.sort(entries, Comparator.comparingInt(Map.Entry<String, Integer>::getValue).reversed());
        StringBuilder sb = new StringBuilder();
        int shown = 0;
        for (Map.Entry<String, Integer> e : entries) {
            if (shown == 3) break;
            if (sb.length() > 0) sb.append("  ");
            sb.append(e.getKey()).append(" ").append(e.getValue());
            shown++;
        }
        if (entries.size() > shown) {
            sb.append("  +").append(entries.size() - shown);
        }
        return sb.toString();
    }

    private int colorForLatency(long avgRtt, long segmentLatency) {
        long worst = Math.max(avgRtt, segmentLatency);
        if (worst <= 150) return 0xFF7CFC5B;
        if (worst <= 400) return 0xFFFFD54F;
        return 0xFFFF8A65;
    }

    private CharSequence buildStatusLine(SegmentManager.Stats s) {
        if (s.peerCount == 0) {
            return "Waiting for peers...";
        }
        if (s.segmentLatencyMs > 600) {
            return String.format(Locale.US, "Peers slow (%d ms) - fallback likely", s.segmentLatencyMs);
        }
        if (s.p2pSegments > 0) {
            return String.format(Locale.US, "Streaming via peers (%d pieces)", s.p2pSegments);
        }
        return "Peers connected - warming up";
    }

    private int determineStatusColor(SegmentManager.Stats s) {
        if (s.peerCount == 0) return 0xFFFFC107;
        if (s.segmentLatencyMs > 600) return 0xFFFF8A65;
        return 0xFF7CFC5B;
    }

    private String human(long b) {
        if (b <= 0) return "0 B";
        if (b < 1024) return b + " B";
        int exp = (int) (Math.log(b) / Math.log(1024));
        String pre = "KMGTPE".charAt(Math.min(exp - 1, 5)) + "";
        return String.format(Locale.US, "%.1f %sB", b / Math.pow(1024, exp), pre);
    }

    private static class MetricBlock {
        final LinearLayout container;
        final TextView value;

        MetricBlock(Context c, String label) {
            container = new LinearLayout(c);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(0, 0, dpStatic(c, 16), 0);
            container.setLayoutParams(params);

            value = new TextView(c);
            value.setTextSize(22f);
            value.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            value.setTextColor(0xFFFFFFFF);
            value.setPadding(0, 0, 0, dpStatic(c, 4));

            TextView labelView = new TextView(c);
            labelView.setText(label);
            labelView.setTextSize(12f);
            labelView.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            labelView.setTextColor(0xFF90A4AE);
            labelView.setAllCaps(true);
            labelView.setLetterSpacing(0.12f);

            container.addView(value);
            container.addView(labelView);
        }

        private static int dpStatic(Context c, int value) {
            float density = c.getResources().getDisplayMetrics().density;
            return Math.round(value * density);
        }
    }
}

package nemosofts.streambox.Util.ui;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import nemosofts.streambox.R;

/**
 * Applies a consistent STB inspired presentation layer across the application's activities.
 */
public final class StbUiCoordinator {

    private static final Typeface HEADING = Typeface.create("sans-serif-medium", Typeface.BOLD);
    private static final Typeface BODY = Typeface.create("sans-serif", Typeface.NORMAL);
    private static final Set<String> SKIP_ACTIVITIES = new HashSet<>(Arrays.asList(
            "PlayerLiveActivity",
            "PlayerMovieActivity",
            "PlayerSingleURLActivity",
            "PlayerEpisodesActivity",
            "SplashActivity",
            "DialogActivity",
            "InterstitialActivity"
    ));

    private StbUiCoordinator() {
    }

    public static void install(@NonNull Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
                if (shouldSkip(activity)) {
                    return;
                }
                activity.getWindow().getDecorView().post(() -> apply(activity));
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                if (shouldSkip(activity)) {
                    return;
                }
                activity.getWindow().getDecorView().post(() -> apply(activity));
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
            }
        });
    }

    private static boolean shouldSkip(@NonNull Activity activity) {
        return SKIP_ACTIVITIES.contains(activity.getClass().getSimpleName());
    }

    private static void apply(@NonNull Activity activity) {
        View content = activity.findViewById(android.R.id.content);
        if (!(content instanceof ViewGroup)) {
            return;
        }
        ViewGroup container = (ViewGroup) content;
        if (container.getChildCount() == 0) {
            return;
        }
        View root = container.getChildAt(0);
        if (Boolean.TRUE.equals(root.getTag(R.id.stb_style_tag))) {
            return;
        }

        decorateWindow(activity);
        styleRootContainer(root);
        traverse(root, activity);
    }

    private static void decorateWindow(@NonNull Activity activity) {
        Window window = activity.getWindow();
        int statusColor = ContextCompat.getColor(activity, R.color.stb_status_bar);
        int navColor = ContextCompat.getColor(activity, R.color.stb_navigation_bar);
        window.setStatusBarColor(statusColor);
        window.setNavigationBarColor(navColor);
    }

    private static void styleRootContainer(@NonNull View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            if (!(group.getBackground() instanceof ColorDrawable)) {
                group.setBackgroundResource(R.drawable.stb_screen_background);
            } else {
                group.setBackgroundResource(R.drawable.stb_screen_background);
            }
            int padding = root.getResources().getDimensionPixelSize(R.dimen.stb_screen_padding);
            if (group.getPaddingLeft() == 0 && group.getPaddingTop() == 0
                    && group.getPaddingRight() == 0 && group.getPaddingBottom() == 0) {
                group.setPadding(padding, padding, padding, padding);
            }
        } else {
            root.setBackgroundResource(R.drawable.stb_screen_background);
        }
    }

    private static void traverse(@NonNull View view, @NonNull Context context) {
        if (Boolean.TRUE.equals(view.getTag(R.id.stb_style_tag))) {
            return;
        }
        if (view.getId() == R.id.rl_top && view instanceof ViewGroup) {
            styleHeader((ViewGroup) view);
        }
        if (view instanceof RecyclerView) {
            styleRecycler((RecyclerView) view, context);
        } else if (view instanceof TextView && !(view instanceof Button)
                && !(view instanceof EditText) && !(view instanceof CheckBox)) {
            styleText((TextView) view, context);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                traverse(group.getChildAt(i), context);
            }
        }
        view.setTag(R.id.stb_style_tag, Boolean.TRUE);
    }

    private static void styleHeader(@NonNull ViewGroup header) {
        header.setBackgroundResource(R.drawable.stb_header_background);
        int horizontal = header.getResources().getDimensionPixelSize(R.dimen.stb_header_horizontal_padding);
        int vertical = header.getResources().getDimensionPixelSize(R.dimen.stb_header_vertical_padding);
        header.setPadding(horizontal, vertical, horizontal, vertical);
        ViewCompat.setElevation(header, header.getResources().getDimension(R.dimen.stb_header_elevation));
    }

    private static void styleRecycler(@NonNull RecyclerView recyclerView, @NonNull Context context) {
        recyclerView.setClipToPadding(false);
        recyclerView.setVerticalScrollBarEnabled(false);
        int horizontal = context.getResources().getDimensionPixelSize(R.dimen.stb_list_horizontal_padding);
        int vertical = context.getResources().getDimensionPixelSize(R.dimen.stb_list_vertical_padding);
        if (recyclerView.getPaddingLeft() == 0 && recyclerView.getPaddingRight() == 0) {
            recyclerView.setPadding(horizontal, recyclerView.getPaddingTop() + vertical,
                    horizontal, recyclerView.getPaddingBottom() + vertical);
        }
    }

    private static void styleText(@NonNull TextView textView, @NonNull Context context) {
        float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
        float sp = textView.getTextSize() / scaledDensity;
        if (sp >= 18f || textView.getTypeface() != null && textView.getTypeface().isBold()) {
            textView.setTypeface(HEADING);
            textView.setTextColor(ContextCompat.getColor(context, R.color.stb_text_heading));
            textView.setLetterSpacing(0.05f);
        } else {
            textView.setTypeface(BODY);
            textView.setTextColor(ContextCompat.getColor(context, R.color.stb_text_body));
        }
    }
}

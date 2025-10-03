package nemosofts.streambox.Util;

import android.content.Context;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.SupabaseClientBuilder;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.realtime.Realtime;

public class SupabaseClient {
    private static SupabaseClient instance;
    private static io.github.jan.supabase.SupabaseClient supabaseClient;

    private static final String SUPABASE_URL = "https://ylwmqaynzoayjeyfponb.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inlsd21xYXluem9heWpleWZwb25iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk0ODc1NjksImV4cCI6MjA3NTA2MzU2OX0.2Wg3WSqPb4AHnt5pttWccLVgASpQ2_3rO33JEB7JHMU";

    private SupabaseClient() {}

    public static SupabaseClient getInstance() {
        if (instance == null) {
            instance = new SupabaseClient();
            initializeClient();
        }
        return instance;
    }

    private static void initializeClient() {
        if (supabaseClient == null) {
            supabaseClient = new SupabaseClientBuilder(SUPABASE_URL, SUPABASE_KEY)
                    .install(new Postgrest())
                    .install(new Realtime())
                    .build();
        }
    }

    public io.github.jan.supabase.SupabaseClient getClient() {
        if (supabaseClient == null) {
            initializeClient();
        }
        return supabaseClient;
    }

    public String getDeviceId(Context context) {
        SharedPref sharedPref = new SharedPref(context);
        android.provider.Settings.Secure.getString(
            context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
        return android.provider.Settings.Secure.getString(
            context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
    }
}

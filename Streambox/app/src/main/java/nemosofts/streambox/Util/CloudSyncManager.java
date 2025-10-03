package nemosofts.streambox.Util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CloudSyncManager {
    private static final String TAG = "CloudSyncManager";
    private static CloudSyncManager instance;
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private String userId;
    private String currentProfileId;

    private static final String SUPABASE_URL = "https://ylwmqaynzoayjeyfponb.supabase.co";
    private static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inlsd21xYXluem9heWpleWZwb25iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTk0ODc1NjksImV4cCI6MjA3NTA2MzU2OX0.2Wg3WSqPb4AHnt5pttWccLVgASpQ2_3rO33JEB7JHMU";

    private CloudSyncManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static CloudSyncManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CloudSyncManager.class) {
                if (instance == null) {
                    instance = new CloudSyncManager(context);
                }
            }
        }
        return instance;
    }

    public interface SyncCallback {
        void onSuccess(String result);
        void onError(String error);
    }

    private String getDeviceId() {
        return android.provider.Settings.Secure.getString(
            context.getContentResolver(),
            android.provider.Settings.Secure.ANDROID_ID
        );
    }

    public void initializeUser(String xtreamUsername, String serverUrl, String profileName, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                String deviceId = getDeviceId();

                JSONObject userJson = new JSONObject();
                userJson.put("device_id", deviceId);
                userJson.put("xtream_username", xtreamUsername);
                userJson.put("xtream_server_url", serverUrl);
                userJson.put("profile_name", profileName);

                String endpoint = SUPABASE_URL + "/rest/v1/users";
                String result = makePostRequest(endpoint, userJson.toString());

                JSONArray resultArray = new JSONArray(result);
                if (resultArray.length() > 0) {
                    JSONObject userObject = resultArray.getJSONObject(0);
                    userId = userObject.getString("id");

                    mainHandler.post(() -> callback.onSuccess(userId));
                } else {
                    mainHandler.post(() -> callback.onError("Failed to create user"));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing user", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void addToFavorites(String contentType, String contentId, String streamId,
                              String name, String logoUrl, String categoryId, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (userId == null) {
                    mainHandler.post(() -> callback.onError("User not initialized"));
                    return;
                }

                JSONObject favJson = new JSONObject();
                favJson.put("user_id", userId);
                if (currentProfileId != null) {
                    favJson.put("profile_id", currentProfileId);
                }
                favJson.put("content_type", contentType);
                favJson.put("content_id", contentId);
                favJson.put("stream_id", streamId);
                favJson.put("name", name);
                favJson.put("logo_url", logoUrl);
                favJson.put("category_id", categoryId);

                String endpoint = SUPABASE_URL + "/rest/v1/favorites";
                String result = makePostRequest(endpoint, favJson.toString());

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error adding to favorites", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void removeFromFavorites(String contentId, String streamId, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (userId == null) {
                    mainHandler.post(() -> callback.onError("User not initialized"));
                    return;
                }

                String endpoint = SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId
                    + "&content_id=eq." + contentId + "&stream_id=eq." + streamId;
                String result = makeDeleteRequest(endpoint);

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error removing from favorites", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getFavorites(String contentType, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (userId == null) {
                    mainHandler.post(() -> callback.onError("User not initialized"));
                    return;
                }

                String endpoint = SUPABASE_URL + "/rest/v1/favorites?user_id=eq." + userId;
                if (contentType != null && !contentType.isEmpty()) {
                    endpoint += "&content_type=eq." + contentType;
                }
                endpoint += "&order=added_at.desc";

                String result = makeGetRequest(endpoint);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting favorites", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void updateWatchHistory(String contentType, String contentId, String streamId,
                                   String name, String logoUrl, long lastPosition, long duration,
                                   String episodeId, int seasonNumber, int episodeNumber,
                                   SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (userId == null) {
                    mainHandler.post(() -> callback.onError("User not initialized"));
                    return;
                }

                JSONObject historyJson = new JSONObject();
                historyJson.put("user_id", userId);
                if (currentProfileId != null) {
                    historyJson.put("profile_id", currentProfileId);
                }
                historyJson.put("content_type", contentType);
                historyJson.put("content_id", contentId);
                historyJson.put("stream_id", streamId);
                historyJson.put("name", name);
                historyJson.put("logo_url", logoUrl);
                historyJson.put("last_position", lastPosition);
                historyJson.put("duration", duration);
                historyJson.put("episode_id", episodeId);
                historyJson.put("season_number", seasonNumber);
                historyJson.put("episode_number", episodeNumber);

                String endpoint = SUPABASE_URL + "/rest/v1/watch_history";
                String result = makePostRequest(endpoint, historyJson.toString());

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error updating watch history", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getWatchHistory(SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (userId == null) {
                    mainHandler.post(() -> callback.onError("User not initialized"));
                    return;
                }

                String endpoint = SUPABASE_URL + "/rest/v1/watch_history?user_id=eq." + userId
                    + "&order=last_watched.desc&limit=50";

                String result = makeGetRequest(endpoint);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting watch history", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void getResumePosition(String contentId, String streamId, String episodeId, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (userId == null) {
                    mainHandler.post(() -> callback.onError("User not initialized"));
                    return;
                }

                String endpoint = SUPABASE_URL + "/rest/v1/watch_history?user_id=eq." + userId
                    + "&content_id=eq." + contentId + "&stream_id=eq." + streamId;
                if (episodeId != null && !episodeId.isEmpty()) {
                    endpoint += "&episode_id=eq." + episodeId;
                }

                String result = makeGetRequest(endpoint);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Error getting resume position", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private String makeGetRequest(String url) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
            .get()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            throw new Exception("Empty response");
        }
    }

    private String makePostRequest(String url, String jsonBody) throws Exception {
        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json"));

        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .post(body)
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            throw new Exception("Empty response");
        }
    }

    private String makeDeleteRequest(String url) throws Exception {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
            .delete()
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return response.body().string();
            }
            return "Success";
        }
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCurrentProfileId(String profileId) {
        this.currentProfileId = profileId;
    }

    public String getUserId() {
        return userId;
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

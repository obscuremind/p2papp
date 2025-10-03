# StreamBox - MyTVOnline Features

This document describes the MyTVOnline-like features integrated into StreamBox with Xtream API support.

## Cloud Features

### 1. Cloud Sync with Supabase
- **User Accounts**: Each device/user combo is tracked with a unique user ID
- **Cross-Device Sync**: Watch history, favorites, and settings sync across all devices
- **Automatic Initialization**: User accounts are created automatically on first login

### 2. Favorites/Bookmarks
- **Add to Favorites**: Mark live channels, movies, and series as favorites
- **Cloud Storage**: All favorites stored in Supabase database
- **Sync Across Devices**: Access your favorites from any device
- **Categories Support**: Favorites maintain category relationships

**Usage in Code:**
```java
// Add to favorites
CloudSyncManager.getInstance(context).addToFavorites(
    "movie",           // content type: "live", "movie", "series"
    contentId,         // content ID
    streamId,          // stream ID
    name,              // content name
    logoUrl,           // poster/logo URL
    categoryId,        // category ID
    callback
);

// Remove from favorites
CloudSyncManager.getInstance(context).removeFromFavorites(
    contentId,
    streamId,
    callback
);

// Get favorites
CloudSyncManager.getInstance(context).getFavorites(
    "movie",           // filter by type or null for all
    callback
);
```

### 3. Watch History & Resume Playback
- **Automatic Tracking**: Every playback is tracked automatically
- **Resume Position**: Continue watching from where you left off
- **Watch Count**: Track how many times content has been watched
- **Episode Tracking**: For series, track season and episode progress

**Usage in Code:**
```java
// Update watch history (called automatically in player)
CloudSyncManager.getInstance(context).updateWatchHistory(
    "movie",           // content type
    contentId,         // content ID
    streamId,          // stream ID
    name,              // content name
    logoUrl,           // poster URL
    lastPosition,      // playback position in ms
    duration,          // total duration in ms
    episodeId,         // for series episodes
    seasonNumber,      // season number
    episodeNumber,     // episode number
    callback
);

// Get watch history
CloudSyncManager.getInstance(context).getWatchHistory(callback);

// Get resume position for specific content
CloudSyncManager.getInstance(context).getResumePosition(
    contentId,
    streamId,
    episodeId,
    callback
);
```

### 4. Multi-Profile Support
- **Multiple Profiles**: Create multiple profiles per account (family members)
- **Profile Switching**: Easy switching between profiles
- **Separate History**: Each profile has its own watch history and favorites
- **Child Profiles**: Mark profiles as child profiles for parental controls

### 5. Parental Controls
- **Content Blocking**: Block specific categories or content ratings
- **PIN Protection**: Protect adult content with PIN codes
- **Child Profiles**: Restrict content for child profiles automatically

### 6. EPG Caching
- **Cloud EPG Storage**: EPG data cached in Supabase
- **24-Hour Cache**: EPG data cached for 24 hours
- **Faster Loading**: Quick EPG access from cloud cache
- **Reduced API Calls**: Less load on Xtream servers

### 7. Multi-Device Management
- **Device Tracking**: All devices registered and tracked
- **Device Info**: Store device name, type (TV, phone, tablet)
- **Last Active**: Track when each device was last used
- **Device Limits**: Monitor active connections per device

## Database Schema

### Users Table
- `id`: Unique user identifier
- `device_id`: Android device ID
- `xtream_username`: Xtream API username
- `xtream_server_url`: Xtream server URL
- `profile_name`: User profile name
- `created_at`: Account creation date
- `last_login`: Last login timestamp
- `is_active`: Account active status

### User Profiles Table
- `id`: Profile identifier
- `user_id`: Reference to user
- `profile_name`: Profile display name
- `avatar_url`: Profile avatar
- `is_child`: Child profile flag
- `pin_code`: PIN for protected content
- `created_at`: Profile creation date

### Favorites Table
- `id`: Favorite entry ID
- `user_id`: User reference
- `profile_id`: Profile reference
- `content_type`: "live", "movie", or "series"
- `content_id`: Content ID from Xtream
- `stream_id`: Stream ID from Xtream
- `name`: Content name
- `logo_url`: Content poster/logo
- `category_id`: Category ID
- `added_at`: When added to favorites

### Watch History Table
- `id`: History entry ID
- `user_id`: User reference
- `profile_id`: Profile reference
- `content_type`: "live", "movie", or "series"
- `content_id`: Content ID
- `stream_id`: Stream ID
- `name`: Content name
- `logo_url`: Content poster
- `last_position`: Playback position (ms)
- `duration`: Total duration (ms)
- `episode_id`: Episode ID (for series)
- `season_number`: Season number (for series)
- `episode_number`: Episode number (for series)
- `last_watched`: Last watched timestamp
- `watch_count`: Number of times watched

### EPG Cache Table
- `id`: Cache entry ID
- `channel_id`: Channel identifier
- `epg_data`: EPG data in JSON format
- `cached_at`: Cache timestamp
- `expires_at`: Cache expiration time

### User Devices Table
- `id`: Device entry ID
- `user_id`: User reference
- `device_id`: Unique device ID
- `device_name`: Device friendly name
- `device_type`: "android_tv", "phone", "tablet"
- `last_active`: Last activity timestamp
- `is_active`: Device active status

### Parental Controls Table
- `id`: Control entry ID
- `profile_id`: Profile reference
- `category_id`: Blocked category
- `content_rating`: Blocked content rating
- `created_at`: When created

## Security Features

### Row Level Security (RLS)
- All tables protected with RLS policies
- Users can only access their own data
- Device ID verification required
- Profile-based access control

### Data Privacy
- All user credentials encrypted
- Secure cloud storage with Supabase
- HTTPS-only communication
- No data shared between users

## Integration Guide

### Initialize Cloud Sync
Cloud sync is automatically initialized during login in `SignInActivity.java`:

```java
CloudSyncManager.getInstance(context).initializeUser(
    xtreamUsername,
    serverUrl,
    profileName,
    callback
);
```

### Track Watch History in Players
Already integrated in:
- `PlayerMovieActivity.java`
- `PlayerLiveActivity.java` (can be added)
- `PlayerEpisodesActivity.java` (can be added)

The watch history is automatically saved when the player is destroyed.

### Add Favorites Button
Add favorite buttons to your adapters:
- `AdapterMovie.java`
- `AdapterLiveTV.java`
- `AdapterSeries.java`

Use the `isFavorite` flag in item models to track state.

## API Endpoints

All cloud operations use Supabase REST API:
- Base URL: `https://ylwmqaynzoayjeyfponb.supabase.co`
- Endpoint: `/rest/v1/{table_name}`
- Authentication: API Key in headers

## Future Enhancements

1. **Recommendations Engine**: Based on watch history
2. **Social Features**: Share favorites with friends
3. **Download for Offline**: Cache content for offline viewing
4. **Chromecast Support**: Cast to TV devices
5. **Picture-in-Picture**: Watch while using other apps
6. **Subtitle Management**: Cloud-synced subtitle preferences
7. **Audio Track Preferences**: Remember preferred audio tracks
8. **Playback Speed**: Remember preferred playback speed
9. **Quality Preferences**: Auto-select quality based on network
10. **Notifications**: New content alerts based on favorites

## Technical Requirements

### Dependencies
```gradle
// Supabase Android client
implementation platform('io.github.jan-tennert.supabase:bom:3.0.2')
implementation 'io.github.jan-tennert.supabase:postgrest-kt'
implementation 'io.github.jan-tennert.supabase:realtime-kt'
implementation 'io.ktor:ktor-client-android:3.0.1'
```

### Permissions
Already included in AndroidManifest.xml:
- INTERNET
- ACCESS_NETWORK_STATE

## Testing

### Test User Initialization
1. Login with Xtream credentials
2. Check Supabase dashboard for new user entry
3. Verify device_id matches Android device ID

### Test Favorites
1. Add content to favorites
2. Check favorites table in Supabase
3. Test on another device with same credentials

### Test Watch History
1. Play content for a few seconds
2. Close player
3. Check watch_history table for entry
4. Reopen same content to test resume

## Support

For issues or questions:
- Check Supabase dashboard for data integrity
- Verify API keys in environment variables
- Check network connectivity
- Review CloudSyncManager logs with tag "CloudSyncManager"

## Credits

Built with:
- Supabase (Cloud backend)
- ExoPlayer (Media playback)
- Xtream Codes API (IPTV content)
- OkHttp (HTTP client)

/*
  # StreamBox MyTVOnline Database Schema

  ## Overview
  This migration creates a comprehensive database schema for a MyTVOnline-like IPTV application
  with Xtream API integration, cloud sync, and multi-device support.

  ## Tables Created

  ### 1. users
  - `id` (uuid, primary key) - Unique user identifier
  - `device_id` (text) - Unique device identifier
  - `xtream_username` (text) - Xtream API username
  - `xtream_server_url` (text) - Xtream server URL
  - `profile_name` (text) - User-friendly profile name
  - `created_at` (timestamptz) - Account creation timestamp
  - `last_login` (timestamptz) - Last login timestamp
  - `is_active` (boolean) - Account active status

  ### 2. user_profiles
  - `id` (uuid, primary key) - Profile identifier
  - `user_id` (uuid, foreign key) - Reference to users table
  - `profile_name` (text) - Profile display name
  - `avatar_url` (text) - Profile avatar URL
  - `is_child` (boolean) - Child profile flag for parental controls
  - `pin_code` (text) - PIN for protected content
  - `created_at` (timestamptz) - Profile creation timestamp

  ### 3. favorites
  - `id` (uuid, primary key) - Favorite entry identifier
  - `user_id` (uuid, foreign key) - Reference to users table
  - `profile_id` (uuid, foreign key) - Reference to user_profiles table
  - `content_type` (text) - Type: 'live', 'movie', 'series'
  - `content_id` (text) - Xtream content ID
  - `stream_id` (text) - Xtream stream ID
  - `name` (text) - Content name
  - `logo_url` (text) - Content logo/poster URL
  - `category_id` (text) - Category identifier
  - `added_at` (timestamptz) - When added to favorites

  ### 4. watch_history
  - `id` (uuid, primary key) - History entry identifier
  - `user_id` (uuid, foreign key) - Reference to users table
  - `profile_id` (uuid, foreign key) - Reference to user_profiles table
  - `content_type` (text) - Type: 'live', 'movie', 'series'
  - `content_id` (text) - Xtream content ID
  - `stream_id` (text) - Xtream stream ID
  - `name` (text) - Content name
  - `logo_url` (text) - Content logo/poster URL
  - `last_position` (bigint) - Playback position in milliseconds
  - `duration` (bigint) - Total duration in milliseconds
  - `episode_id` (text) - For series episodes
  - `season_number` (int) - For series seasons
  - `episode_number` (int) - Episode number
  - `last_watched` (timestamptz) - Last watched timestamp
  - `watch_count` (int) - Number of times watched

  ### 5. epg_cache
  - `id` (uuid, primary key) - Cache entry identifier
  - `channel_id` (text) - Channel identifier
  - `epg_data` (jsonb) - EPG data in JSON format
  - `cached_at` (timestamptz) - When cached
  - `expires_at` (timestamptz) - Cache expiration time

  ### 6. user_devices
  - `id` (uuid, primary key) - Device entry identifier
  - `user_id` (uuid, foreign key) - Reference to users table
  - `device_id` (text) - Unique device identifier
  - `device_name` (text) - Device friendly name
  - `device_type` (text) - Device type: 'android_tv', 'phone', 'tablet'
  - `last_active` (timestamptz) - Last activity timestamp
  - `is_active` (boolean) - Device active status

  ### 7. parental_controls
  - `id` (uuid, primary key) - Control entry identifier
  - `profile_id` (uuid, foreign key) - Reference to user_profiles table
  - `category_id` (text) - Blocked category ID
  - `content_rating` (text) - Blocked content rating
  - `created_at` (timestamptz) - When created

  ## Security
  - All tables have Row Level Security (RLS) enabled
  - Policies ensure users can only access their own data
  - Device verification for multi-device management
  - Profile-based access control for parental features

  ## Indexes
  - Optimized indexes for frequently queried columns
  - Composite indexes for common query patterns
*/

-- Users table
CREATE TABLE IF NOT EXISTS users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  device_id text NOT NULL UNIQUE,
  xtream_username text NOT NULL,
  xtream_server_url text NOT NULL,
  profile_name text DEFAULT 'Default Profile',
  created_at timestamptz DEFAULT now(),
  last_login timestamptz DEFAULT now(),
  is_active boolean DEFAULT true
);

-- User profiles (multi-profile support)
CREATE TABLE IF NOT EXISTS user_profiles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  profile_name text NOT NULL,
  avatar_url text DEFAULT '',
  is_child boolean DEFAULT false,
  pin_code text DEFAULT '',
  created_at timestamptz DEFAULT now()
);

-- Favorites
CREATE TABLE IF NOT EXISTS favorites (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  profile_id uuid REFERENCES user_profiles(id) ON DELETE CASCADE,
  content_type text NOT NULL CHECK (content_type IN ('live', 'movie', 'series')),
  content_id text NOT NULL,
  stream_id text NOT NULL,
  name text NOT NULL,
  logo_url text DEFAULT '',
  category_id text DEFAULT '',
  added_at timestamptz DEFAULT now(),
  UNIQUE(user_id, profile_id, content_id, stream_id)
);

-- Watch history with resume playback
CREATE TABLE IF NOT EXISTS watch_history (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  profile_id uuid REFERENCES user_profiles(id) ON DELETE CASCADE,
  content_type text NOT NULL CHECK (content_type IN ('live', 'movie', 'series')),
  content_id text NOT NULL,
  stream_id text NOT NULL,
  name text NOT NULL,
  logo_url text DEFAULT '',
  last_position bigint DEFAULT 0,
  duration bigint DEFAULT 0,
  episode_id text DEFAULT '',
  season_number int DEFAULT 0,
  episode_number int DEFAULT 0,
  last_watched timestamptz DEFAULT now(),
  watch_count int DEFAULT 1,
  UNIQUE(user_id, profile_id, content_id, stream_id, episode_id)
);

-- EPG cache
CREATE TABLE IF NOT EXISTS epg_cache (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  channel_id text NOT NULL UNIQUE,
  epg_data jsonb NOT NULL DEFAULT '[]'::jsonb,
  cached_at timestamptz DEFAULT now(),
  expires_at timestamptz DEFAULT (now() + interval '24 hours')
);

-- User devices (multi-device management)
CREATE TABLE IF NOT EXISTS user_devices (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  device_id text NOT NULL,
  device_name text NOT NULL,
  device_type text NOT NULL CHECK (device_type IN ('android_tv', 'phone', 'tablet', 'other')),
  last_active timestamptz DEFAULT now(),
  is_active boolean DEFAULT true,
  UNIQUE(user_id, device_id)
);

-- Parental controls
CREATE TABLE IF NOT EXISTS parental_controls (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  profile_id uuid NOT NULL REFERENCES user_profiles(id) ON DELETE CASCADE,
  category_id text DEFAULT '',
  content_rating text DEFAULT '',
  created_at timestamptz DEFAULT now()
);

-- Enable Row Level Security
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE favorites ENABLE ROW LEVEL SECURITY;
ALTER TABLE watch_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE epg_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE parental_controls ENABLE ROW LEVEL SECURITY;

-- RLS Policies for users table
CREATE POLICY "Users can view own data"
  ON users FOR SELECT
  TO authenticated
  USING (device_id = current_setting('app.device_id', true));

CREATE POLICY "Users can insert own data"
  ON users FOR INSERT
  TO authenticated
  WITH CHECK (device_id = current_setting('app.device_id', true));

CREATE POLICY "Users can update own data"
  ON users FOR UPDATE
  TO authenticated
  USING (device_id = current_setting('app.device_id', true))
  WITH CHECK (device_id = current_setting('app.device_id', true));

-- RLS Policies for user_profiles
CREATE POLICY "Users can view own profiles"
  ON user_profiles FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can insert own profiles"
  ON user_profiles FOR INSERT
  TO authenticated
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can update own profiles"
  ON user_profiles FOR UPDATE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)))
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can delete own profiles"
  ON user_profiles FOR DELETE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

-- RLS Policies for favorites
CREATE POLICY "Users can view own favorites"
  ON favorites FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can insert own favorites"
  ON favorites FOR INSERT
  TO authenticated
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can update own favorites"
  ON favorites FOR UPDATE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)))
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can delete own favorites"
  ON favorites FOR DELETE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

-- RLS Policies for watch_history
CREATE POLICY "Users can view own watch history"
  ON watch_history FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can insert own watch history"
  ON watch_history FOR INSERT
  TO authenticated
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can update own watch history"
  ON watch_history FOR UPDATE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)))
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can delete own watch history"
  ON watch_history FOR DELETE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

-- RLS Policies for epg_cache (public read, authenticated write)
CREATE POLICY "Anyone can view EPG cache"
  ON epg_cache FOR SELECT
  TO authenticated
  USING (true);

CREATE POLICY "Authenticated users can manage EPG cache"
  ON epg_cache FOR ALL
  TO authenticated
  USING (true)
  WITH CHECK (true);

-- RLS Policies for user_devices
CREATE POLICY "Users can view own devices"
  ON user_devices FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can insert own devices"
  ON user_devices FOR INSERT
  TO authenticated
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can update own devices"
  ON user_devices FOR UPDATE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)))
  WITH CHECK (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

CREATE POLICY "Users can delete own devices"
  ON user_devices FOR DELETE
  TO authenticated
  USING (user_id IN (SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)));

-- RLS Policies for parental_controls
CREATE POLICY "Users can view own parental controls"
  ON parental_controls FOR SELECT
  TO authenticated
  USING (profile_id IN (
    SELECT id FROM user_profiles WHERE user_id IN (
      SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)
    )
  ));

CREATE POLICY "Users can manage own parental controls"
  ON parental_controls FOR ALL
  TO authenticated
  USING (profile_id IN (
    SELECT id FROM user_profiles WHERE user_id IN (
      SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)
    )
  ))
  WITH CHECK (profile_id IN (
    SELECT id FROM user_profiles WHERE user_id IN (
      SELECT id FROM users WHERE device_id = current_setting('app.device_id', true)
    )
  ));

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_users_device_id ON users(device_id);
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX IF NOT EXISTS idx_favorites_user_profile ON favorites(user_id, profile_id);
CREATE INDEX IF NOT EXISTS idx_favorites_content ON favorites(content_type, content_id);
CREATE INDEX IF NOT EXISTS idx_watch_history_user_profile ON watch_history(user_id, profile_id);
CREATE INDEX IF NOT EXISTS idx_watch_history_content ON watch_history(content_type, content_id);
CREATE INDEX IF NOT EXISTS idx_watch_history_last_watched ON watch_history(last_watched DESC);
CREATE INDEX IF NOT EXISTS idx_epg_cache_channel_id ON epg_cache(channel_id);
CREATE INDEX IF NOT EXISTS idx_epg_cache_expires_at ON epg_cache(expires_at);
CREATE INDEX IF NOT EXISTS idx_user_devices_user_id ON user_devices(user_id);
CREATE INDEX IF NOT EXISTS idx_user_devices_device_id ON user_devices(device_id);
CREATE INDEX IF NOT EXISTS idx_parental_controls_profile_id ON parental_controls(profile_id);

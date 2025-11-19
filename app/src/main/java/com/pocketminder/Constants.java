package com.pocketminder;

/**
 * Application-wide constants
 */
public class Constants {

    // Location Settings
    public static final int LOCATION_UPDATE_INTERVAL_MS = 30000; // 30 seconds
    public static final int FASTEST_UPDATE_INTERVAL_MS = 15000; // 15 seconds
    public static final float PROXIMITY_THRESHOLD_METERS = 200.0f; // 200 meters
    public static final float PROXIMITY_CLEAR_THRESHOLD_METERS = 600.0f; // 3x threshold
    public static final int SUPERMARKET_SEARCH_RADIUS_METERS = 2000; // 2km

    // WorkManager Settings
    public static final int WORK_MANAGER_INTERVAL_MINUTES = 15;
    public static final int WORK_MANAGER_INITIAL_DELAY_MINUTES = 1;
    public static final String WORK_MANAGER_LOCATION_CHECK = "location_check_work";

    // Notification Settings
    public static final int NOTIFICATION_ID_FOREGROUND = 1001;
    public static final int NOTIFICATION_ID_REMINDER = 2001;
    public static final String CHANNEL_ID_LOCATION = "location_tracking";
    public static final String CHANNEL_ID_REMINDER = "shopping_reminder";
    public static final long NOTIFICATION_COOLDOWN_MS = 900000; // 15 minutes

    // API Settings
    public static final String GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com/maps/api/place";
    public static final String PLACE_TYPE_SUPERMARKET = "grocery_or_supermarket";
    public static final int API_TIMEOUT_MS = 10000; // 10 seconds
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final int RETRY_DELAY_MS = 2000;

    // Database Settings
    public static final String DATABASE_NAME = "pocket_minder.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_SHOPPING_LIST = "shopping_list";
    public static final String TABLE_SUPERMARKET_CACHE = "supermarket_cache";

    // Preferences Keys
    public static final String PREF_NAME = "pocket_minder_prefs";
    public static final String PREF_TRACKING_ENABLED = "tracking_enabled";
    public static final String PREF_LAST_NOTIFICATION_TIME = "last_notification_time";
    public static final String PREF_LAST_NOTIFIED_SUPERMARKET = "last_notified_supermarket";
    public static final String PREF_NOTIFICATION_RADIUS = "notification_radius";
    public static final String PREF_FIRST_RUN = "first_run";
    public static final String PREF_USE_GEOFENCING = "use_geofencing";

    // Intent Actions
    public static final String ACTION_MARK_PURCHASED = "com.pocketminder.ACTION_MARK_PURCHASED";
    public static final String ACTION_VIEW_LIST = "com.pocketminder.ACTION_VIEW_LIST";
    public static final String ACTION_NAVIGATE = "com.pocketminder.ACTION_NAVIGATE";

    // Cache Settings
    public static final long CACHE_EXPIRY_MS = 3600000; // 1 hour

    private Constants() {
        // Prevent instantiation
    }
}

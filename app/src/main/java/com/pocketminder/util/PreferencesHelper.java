package com.pocketminder.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class for managing shared preferences
 */
public class PreferencesHelper {
    private static final String PREF_NAME = "pocket_minder_prefs";
    private static final String KEY_TRACKING_ENABLED = "tracking_enabled";
    private static final String KEY_LAST_NOTIFICATION_TIME = "last_notification_time";
    private static final String KEY_LAST_NOTIFIED_SUPERMARKET = "last_notified_supermarket";
    private static final String KEY_NOTIFICATION_RADIUS = "notification_radius";
    private static final String KEY_FIRST_RUN = "first_run";
    private static final String KEY_USE_GEOFENCING = "use_geofencing";
    private static final String KEY_DEVELOPER_MODE = "developer_mode";
    private static final String KEY_DEV_PROXIMITY_RANGE = "dev_proximity_range";
    private static final String KEY_NOTIFICATION_COOLDOWN_HOURS = "notification_cooldown_hours";
    private static final String KEY_LAST_MANUAL_FETCH = "last_manual_fetch";
    private static final long MANUAL_FETCH_COOLDOWN_MS = 3600000; // 1 hour

    private final SharedPreferences preferences;

    public PreferencesHelper(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isTrackingEnabled() {
        return preferences.getBoolean(KEY_TRACKING_ENABLED, true);
    }

    public void setTrackingEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_TRACKING_ENABLED, enabled).apply();
    }

    public long getLastNotificationTime() {
        return preferences.getLong(KEY_LAST_NOTIFICATION_TIME, 0);
    }

    public void setLastNotificationTime(long timestamp) {
        preferences.edit().putLong(KEY_LAST_NOTIFICATION_TIME, timestamp).apply();
    }

    public String getLastNotifiedSupermarket() {
        return preferences.getString(KEY_LAST_NOTIFIED_SUPERMARKET, "");
    }

    public void setLastNotifiedSupermarket(String supermarketName) {
        preferences.edit().putString(KEY_LAST_NOTIFIED_SUPERMARKET, supermarketName).apply();
    }

    public int getNotificationRadius() {
        return preferences.getInt(KEY_NOTIFICATION_RADIUS, 200);
    }

    public void setNotificationRadius(int radius) {
        preferences.edit().putInt(KEY_NOTIFICATION_RADIUS, radius).apply();
    }

    public boolean isFirstRun() {
        return preferences.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRun(boolean firstRun) {
        preferences.edit().putBoolean(KEY_FIRST_RUN, firstRun).apply();
    }

    public boolean isGeofencingEnabled() {
        return preferences.getBoolean(KEY_USE_GEOFENCING, true);
    }

    public void setGeofencingEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_USE_GEOFENCING, enabled).apply();
    }

    /**
     * Check if a specific store type is enabled for notifications
     * @param storeType The store type identifier (e.g., "99speedmart", "kkmart", "caring")
     * @return true if enabled, default is true (all stores enabled by default)
     */
    public boolean isStoreTypeEnabled(String storeType) {
        return preferences.getBoolean("store_" + storeType, true);
    }

    /**
     * Enable or disable notifications for a specific store type
     * @param storeType The store type identifier (e.g., "99speedmart", "kkmart", "caring")
     * @param enabled true to enable, false to disable
     */
    public void setStoreTypeEnabled(String storeType, boolean enabled) {
        preferences.edit().putBoolean("store_" + storeType, enabled).apply();
    }

    /**
     * Check if developer mode is enabled
     * Developer mode enables testing features like extended proximity range
     * @return true if developer mode is enabled
     */
    public boolean isDeveloperModeEnabled() {
        return preferences.getBoolean(KEY_DEVELOPER_MODE, false);
    }

    /**
     * Enable or disable developer mode
     * @param enabled true to enable, false to disable
     */
    public void setDeveloperModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DEVELOPER_MODE, enabled).apply();
    }

    /**
     * Get the proximity range for notifications in meters
     * In developer mode, this can be extended to test notifications from farther away
     * @return proximity range in meters (default: 200m, dev mode: 5000m)
     */
    public int getProximityRange() {
        if (isDeveloperModeEnabled()) {
            return preferences.getInt(KEY_DEV_PROXIMITY_RANGE, 5000); // 5km in dev mode
        }
        return getNotificationRadius(); // Normal mode: use setting (default 200m)
    }

    /**
     * Set the proximity range for developer mode testing
     * @param rangeMeters range in meters (e.g., 5000 for 5km)
     */
    public void setDevProximityRange(int rangeMeters) {
        preferences.edit().putInt(KEY_DEV_PROXIMITY_RANGE, rangeMeters).apply();
    }

    /**
     * Get notification cooldown duration in hours
     * @return cooldown duration in hours (default: 4 hours)
     */
    public int getNotificationCooldownHours() {
        return preferences.getInt(KEY_NOTIFICATION_COOLDOWN_HOURS, 4);
    }

    /**
     * Set notification cooldown duration in hours
     * @param hours cooldown duration in hours
     */
    public void setNotificationCooldownHours(int hours) {
        preferences.edit().putInt(KEY_NOTIFICATION_COOLDOWN_HOURS, hours).apply();
    }

    /**
     * Get notification cooldown duration in milliseconds
     * @return cooldown duration in milliseconds
     */
    public long getNotificationCooldownMs() {
        return getNotificationCooldownHours() * 3600000L; // hours to milliseconds
    }

    /**
     * Get last notification time for a specific store
     * @param placeId The Google Place ID of the store
     * @return timestamp in milliseconds, 0 if never notified
     */
    public long getStoreLastNotificationTime(String placeId) {
        return preferences.getLong("store_notif_" + placeId, 0);
    }

    /**
     * Set last notification time for a specific store
     * @param placeId The Google Place ID of the store
     * @param timestamp timestamp in milliseconds
     */
    public void setStoreLastNotificationTime(String placeId, long timestamp) {
        preferences.edit().putLong("store_notif_" + placeId, timestamp).apply();
    }

    /**
     * Check if cooldown has passed for a specific store
     * @param placeId The Google Place ID of the store
     * @return true if cooldown has passed and notification can be sent
     */
    public boolean canNotifyStore(String placeId) {
        long lastNotificationTime = getStoreLastNotificationTime(placeId);
        if (lastNotificationTime == 0) {
            return true; // Never notified, can notify
        }
        long cooldownMs = getNotificationCooldownMs();
        long timeSinceLastNotification = System.currentTimeMillis() - lastNotificationTime;
        return timeSinceLastNotification >= cooldownMs;
    }

    /**
     * Clear all store notification history (useful for testing or reset)
     */
    public void clearAllStoreNotificationHistory() {
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith("store_notif_")) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    /**
     * Get last manual fetch timestamp
     * @return timestamp in milliseconds, 0 if never fetched manually
     */
    public long getLastManualFetch() {
        return preferences.getLong(KEY_LAST_MANUAL_FETCH, 0);
    }

    /**
     * Set last manual fetch timestamp
     * @param timestamp timestamp in milliseconds
     */
    public void setLastManualFetch(long timestamp) {
        preferences.edit().putLong(KEY_LAST_MANUAL_FETCH, timestamp).apply();
    }

    /**
     * Check if manual fetch cooldown has passed
     * @return true if cooldown has passed (can fetch again), false otherwise
     */
    public boolean canManualFetch() {
        long lastFetch = getLastManualFetch();
        if (lastFetch == 0) {
            return true; // Never fetched, can fetch
        }
        long timeSinceFetch = System.currentTimeMillis() - lastFetch;
        return timeSinceFetch >= MANUAL_FETCH_COOLDOWN_MS;
    }

    /**
     * Get remaining cooldown time for manual fetch in minutes
     * @return remaining minutes, 0 if can fetch
     */
    public long getManualFetchCooldownMinutes() {
        if (canManualFetch()) {
            return 0;
        }
        long lastFetch = getLastManualFetch();
        long timeSinceFetch = System.currentTimeMillis() - lastFetch;
        long remainingMs = MANUAL_FETCH_COOLDOWN_MS - timeSinceFetch;
        return (remainingMs / 60000) + 1; // Round up
    }
}

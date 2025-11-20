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
}

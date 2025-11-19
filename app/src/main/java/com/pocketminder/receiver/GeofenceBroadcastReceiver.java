package com.pocketminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.pocketminder.database.ShoppingListDBHelper;
import com.pocketminder.util.NotificationHelper;
import com.pocketminder.util.PreferencesHelper;

import java.util.List;

/**
 * Receiver for geofence transition events
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "GeofenceReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        if (geofencingEvent == null) {
            Log.e(TAG, "Geofencing event is null");
            return;
        }

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: " + geofencingEvent.getErrorCode());
            return;
        }

        // Get the transition type
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Only handle ENTER transitions
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            if (triggeringGeofences != null && !triggeringGeofences.isEmpty()) {
                handleGeofenceEnter(context, triggeringGeofences.get(0));
            }
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "Exited geofence");
        }
    }

    /**
     * Handle entering a geofence (near supermarket)
     */
    private void handleGeofenceEnter(Context context, Geofence geofence) {
        String placeId = geofence.getRequestId();
        Log.d(TAG, "Entered geofence: " + placeId);

        // Check if there are unpurchased items
        ShoppingListDBHelper dbHelper = ShoppingListDBHelper.getInstance(context);
        int unpurchasedCount = dbHelper.getUnpurchasedItemsCount();

        if (unpurchasedCount == 0) {
            Log.d(TAG, "No items to buy");
            return;
        }

        // Check notification cooldown
        PreferencesHelper preferencesHelper = new PreferencesHelper(context);
        long lastNotificationTime = preferencesHelper.getLastNotificationTime();
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastNotificationTime < 900000) { // 15 minutes
            Log.d(TAG, "Notification cooldown active");
            return;
        }

        // Send notification
        NotificationHelper notificationHelper = new NotificationHelper(context);
        notificationHelper.showShoppingReminder("Nearby Supermarket", unpurchasedCount);

        preferencesHelper.setLastNotificationTime(currentTime);

        Log.d(TAG, "Shopping reminder sent");
    }
}

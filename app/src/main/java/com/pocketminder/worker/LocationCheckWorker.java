package com.pocketminder.worker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Tasks;
import com.pocketminder.api.GooglePlacesAPI;
import com.pocketminder.database.ShoppingListDBHelper;
import com.pocketminder.model.Supermarket;
import com.pocketminder.service.LocationTrackingService;
import com.pocketminder.util.NotificationHelper;
import com.pocketminder.util.PreferencesHelper;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WorkManager worker for periodic location checks
 * This ensures the app continues working even after being killed
 */
public class LocationCheckWorker extends Worker {
    private static final String TAG = "LocationCheckWorker";
    private static final float PROXIMITY_THRESHOLD = 200; // 200 meters

    public LocationCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "LocationCheckWorker executing");

        try {
            Context context = getApplicationContext();

            // Check if tracking is enabled
            PreferencesHelper preferencesHelper = new PreferencesHelper(context);
            if (!preferencesHelper.isTrackingEnabled()) {
                Log.d(TAG, "Tracking disabled, skipping");
                return Result.success();
            }

            // Check if there are items in shopping list
            ShoppingListDBHelper dbHelper = ShoppingListDBHelper.getInstance(context);
            int unpurchasedCount = dbHelper.getUnpurchasedItemsCount();
            if (unpurchasedCount == 0) {
                Log.d(TAG, "No items in shopping list");
                return Result.success();
            }

            // Get current location
            Location location = getCurrentLocation(context);
            if (location == null) {
                Log.e(TAG, "Could not get current location");
                // Ensure service is running
                ensureServiceRunning(context);
                return Result.retry();
            }

            // Check for nearby supermarkets
            GooglePlacesAPI placesAPI = new GooglePlacesAPI(context);
            List<Supermarket> supermarkets = placesAPI.searchNearbySupermarkets(
                    location.getLatitude(),
                    location.getLongitude());

            // Check proximity and send notification
            for (Supermarket supermarket : supermarkets) {
                float distance = supermarket.distanceTo(location.getLatitude(), location.getLongitude());

                if (distance <= PROXIMITY_THRESHOLD) {
                    // Check if we recently notified (avoid spam)
                    long lastNotificationTime = preferencesHelper.getLastNotificationTime();
                    long timeSinceLastNotification = System.currentTimeMillis() - lastNotificationTime;

                    if (timeSinceLastNotification > TimeUnit.MINUTES.toMillis(15)) {
                        NotificationHelper notificationHelper = new NotificationHelper(context);
                        notificationHelper.showShoppingReminder(supermarket.getName(), unpurchasedCount);

                        preferencesHelper.setLastNotificationTime(System.currentTimeMillis());
                        preferencesHelper.setLastNotifiedSupermarket(supermarket.getName());

                        Log.d(TAG, "Sent reminder for " + supermarket.getName());
                    }
                    break;
                }
            }

            // Ensure foreground service is running
            ensureServiceRunning(context);

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Error in LocationCheckWorker", e);
            return Result.retry();
        }
    }

    /**
     * Get current location synchronously
     */
    private Location getCurrentLocation(Context context) {
        if (ActivityCompat.checkSelfPermission(context,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        try {
            FusedLocationProviderClient fusedLocationClient =
                    LocationServices.getFusedLocationProviderClient(context);
            return Tasks.await(fusedLocationClient.getLastLocation(), 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Error getting location", e);
            return null;
        }
    }

    /**
     * Ensure the foreground service is running
     */
    private void ensureServiceRunning(Context context) {
        try {
            Intent serviceIntent = new Intent(context, LocationTrackingService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Started LocationTrackingService");
        } catch (Exception e) {
            Log.e(TAG, "Error starting service", e);
        }
    }
}

package com.pocketminder.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.pocketminder.Constants;
import com.pocketminder.api.GooglePlacesAPI;
import com.pocketminder.database.ShoppingListDBHelper;
import com.pocketminder.database.SupermarketCacheManager;
import com.pocketminder.model.Supermarket;
import com.pocketminder.util.NotificationHelper;
import com.pocketminder.util.PreferencesHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Foreground service for continuous location tracking
 */
public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final int LOCATION_UPDATE_INTERVAL = 30000; // 30 seconds
    private static final int FASTEST_UPDATE_INTERVAL = 15000; // 15 seconds
    // PROXIMITY_THRESHOLD now dynamic - uses PreferencesHelper.getProximityRange()

    private final IBinder binder = new LocalBinder();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private NotificationHelper notificationHelper;
    private GooglePlacesAPI placesAPI;
    private ShoppingListDBHelper dbHelper;
    private SupermarketCacheManager cacheManager;
    private PreferencesHelper preferencesHelper;
    private ExecutorService executorService;

    private Location lastLocation;
    private List<Supermarket> nearbySupermarkets = new ArrayList<>();
    private Set<String> notifiedSupermarkets = new HashSet<>();
    private boolean isTracking = false;

    public class LocalBinder extends Binder {
        public LocationTrackingService getService() {
            return LocationTrackingService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        notificationHelper = new NotificationHelper(this);
        placesAPI = new GooglePlacesAPI(this);
        dbHelper = ShoppingListDBHelper.getInstance(this);
        cacheManager = new SupermarketCacheManager(this);
        preferencesHelper = new PreferencesHelper(this);
        executorService = Executors.newSingleThreadExecutor();

        setupLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");

        // Start as foreground service
        startForeground(NotificationHelper.NOTIFICATION_ID_FOREGROUND,
                notificationHelper.createForegroundNotification());

        startLocationTracking();

        // Service will be restarted if killed by system
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Setup location callback for receiving location updates
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    onLocationChanged(location);
                }
            }
        };
    }

    /**
     * Start location tracking
     */
    public void startLocationTracking() {
        if (isTracking) {
            Log.d(TAG, "Already tracking location");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                LOCATION_UPDATE_INTERVAL)
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(false)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                Looper.getMainLooper());

        isTracking = true;
        Log.d(TAG, "Started location tracking");
    }

    /**
     * Stop location tracking
     */
    public void stopLocationTracking() {
        if (!isTracking) {
            return;
        }

        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        Log.d(TAG, "Stopped location tracking");
    }

    /**
     * Handle location updates
     */
    private void onLocationChanged(Location location) {
        lastLocation = location;
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());

        // Update notification with current location
        notificationHelper.updateForegroundNotification(
                String.format("Lat: %.4f, Lng: %.4f", location.getLatitude(), location.getLongitude()));

        // Check for nearby supermarkets in background thread
        executorService.execute(() -> checkForNearbySupermarkets(location));
    }

    /**
     * Check for nearby supermarkets and send notifications if needed
     */
    private void checkForNearbySupermarkets(Location currentLocation) {
        try {
            // Get unpurchased items count
            int unpurchasedCount = dbHelper.getUnpurchasedItemsCount();
            if (unpurchasedCount == 0) {
                Log.d(TAG, "No items in shopping list, skipping check");
                return;
            }

            // Get dynamic proximity range for notifications (200m normal, 5km in dev mode)
            int proximityThreshold = preferencesHelper.getProximityRange();
            boolean devMode = preferencesHelper.isDeveloperModeEnabled();

            if (devMode) {
                Log.d(TAG, "Developer mode: Using extended notification range: " + proximityThreshold + "m");
            }

            // STEP 1: Check cache first (10km coverage area)
            List<Supermarket> supermarkets = cacheManager.getCachedSupermarkets(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());

            if (supermarkets.isEmpty()) {
                // STEP 2: Cache miss - fetch from API with 10km coverage radius
                Log.d(TAG, "Cache miss - fetching from API with " + Constants.CACHE_COVERAGE_RADIUS_METERS + "m coverage");
                supermarkets = placesAPI.searchNearbySupermarkets(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude(),
                        Constants.CACHE_COVERAGE_RADIUS_METERS);

                // STEP 3: Store in cache for future use (reduces API calls by 99%)
                if (!supermarkets.isEmpty()) {
                    cacheManager.cacheSupermarkets(supermarkets,
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude());
                    Log.d(TAG, "Cached " + supermarkets.size() + " supermarkets for 7 days");
                }
            } else {
                Log.d(TAG, "Cache hit - using " + supermarkets.size() + " cached supermarkets (no API call!)");
            }

            nearbySupermarkets = supermarkets;

            // Check proximity to each supermarket
            for (Supermarket supermarket : supermarkets) {
                float distance = supermarket.distanceTo(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude());

                Log.d(TAG, "Distance to " + supermarket.getName() + ": " + distance + "m");

                // If within proximity threshold and not already notified
                if (distance <= proximityThreshold && !notifiedSupermarkets.contains(supermarket.getPlaceId())) {
                    // Check if this store type is enabled in user preferences
                    if (!isStoreEnabled(supermarket.getName())) {
                        Log.d(TAG, "Store type disabled for: " + supermarket.getName());
                        continue;
                    }

                    // Check if cooldown has passed for this specific store
                    if (!preferencesHelper.canNotifyStore(supermarket.getPlaceId())) {
                        long lastNotif = preferencesHelper.getStoreLastNotificationTime(supermarket.getPlaceId());
                        long cooldownHours = preferencesHelper.getNotificationCooldownHours();
                        long hoursAgo = (System.currentTimeMillis() - lastNotif) / 3600000L;
                        Log.d(TAG, "Cooldown active for " + supermarket.getName() +
                                " (notified " + hoursAgo + "h ago, cooldown: " + cooldownHours + "h)");
                        continue;
                    }

                    // Send notification on main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        notificationHelper.showShoppingReminder(
                                supermarket.getName(),
                                unpurchasedCount,
                                supermarket.getLatitude(),
                                supermarket.getLongitude());
                        notifiedSupermarkets.add(supermarket.getPlaceId());

                        // Save last notification time (global - for backwards compatibility)
                        preferencesHelper.setLastNotificationTime(System.currentTimeMillis());
                        preferencesHelper.setLastNotifiedSupermarket(supermarket.getName());

                        // Save per-store notification time (for cooldown tracking)
                        preferencesHelper.setStoreLastNotificationTime(supermarket.getPlaceId(),
                                System.currentTimeMillis());
                    });

                    Log.d(TAG, "Sent reminder for " + supermarket.getName());
                    break; // Only notify for the closest supermarket
                }
            }

            // Clear notified set if user has moved away from all supermarkets
            if (shouldClearNotifiedSet(supermarkets, currentLocation, proximityThreshold)) {
                notifiedSupermarkets.clear();
                Log.d(TAG, "Cleared notified supermarkets set");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error checking nearby supermarkets", e);
        }
    }

    /**
     * Check if we should clear the notified supermarkets set
     * (when user moves far from all previously notified supermarkets)
     */
    private boolean shouldClearNotifiedSet(List<Supermarket> supermarkets, Location currentLocation, int proximityThreshold) {
        if (notifiedSupermarkets.isEmpty()) {
            return false;
        }

        for (Supermarket supermarket : supermarkets) {
            if (notifiedSupermarkets.contains(supermarket.getPlaceId())) {
                float distance = supermarket.distanceTo(
                        currentLocation.getLatitude(),
                        currentLocation.getLongitude());
                if (distance <= proximityThreshold * 3) { // 3x threshold
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if a store is enabled in user preferences
     * Maps store names to store type preferences
     */
    private boolean isStoreEnabled(String storeName) {
        if (storeName == null) {
            return true;
        }

        String lowerName = storeName.toLowerCase();

        // Map store names to preference keys
        if (lowerName.contains("99 speedmart") || lowerName.contains("99speedmart")) {
            return preferencesHelper.isStoreTypeEnabled("99speedmart");
        } else if (lowerName.contains("kk mart") || lowerName.contains("kk super mart")) {
            return preferencesHelper.isStoreTypeEnabled("kkmart");
        } else if (lowerName.contains("caring")) {
            return preferencesHelper.isStoreTypeEnabled("caring");
        } else if (lowerName.contains("watsons")) {
            return preferencesHelper.isStoreTypeEnabled("watsons");
        } else if (lowerName.contains("guardian")) {
            return preferencesHelper.isStoreTypeEnabled("guardian");
        } else if (lowerName.contains("lotus")) {
            return preferencesHelper.isStoreTypeEnabled("lotus");
        } else if (lowerName.contains("jaya grocer")) {
            return preferencesHelper.isStoreTypeEnabled("jayagrocer");
        } else {
            // For all other stores (AEON, Village Grocer, etc.)
            return preferencesHelper.isStoreTypeEnabled("other");
        }
    }

    /**
     * Get current location
     */
    public Location getLastLocation() {
        return lastLocation;
    }

    /**
     * Get nearby supermarkets
     */
    public List<Supermarket> getNearbySupermarkets() {
        return nearbySupermarkets;
    }

    /**
     * Check if service is tracking
     */
    public boolean isTracking() {
        return isTracking;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopLocationTracking();
        executorService.shutdown();
    }
}

package com.pocketminder;

import android.app.Application;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pocketminder.worker.LocationCheckWorker;

import java.util.concurrent.TimeUnit;

/**
 * Custom Application class for initialization
 */
public class PocketMinderApplication extends Application {
    private static final String TAG = "PocketMinderApp";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Application created");

        // Initialize WorkManager for periodic location checks
        initializeWorkManager();
    }

    /**
     * Initialize WorkManager for background location checks
     * This ensures the app continues working even when killed
     */
    private void initializeWorkManager() {
        try {
            // Create periodic work request (every 15 minutes)
            PeriodicWorkRequest locationCheckRequest = new PeriodicWorkRequest.Builder(
                    LocationCheckWorker.class,
                    15, TimeUnit.MINUTES)
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .build();

            // Enqueue the work request
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "location_check_work",
                    ExistingPeriodicWorkPolicy.KEEP,
                    locationCheckRequest);

            Log.d(TAG, "WorkManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing WorkManager", e);
        }
    }
}

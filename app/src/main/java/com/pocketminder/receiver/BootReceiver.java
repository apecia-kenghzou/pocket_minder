package com.pocketminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pocketminder.service.LocationTrackingService;
import com.pocketminder.util.PreferencesHelper;
import com.pocketminder.worker.LocationCheckWorker;

import java.util.concurrent.TimeUnit;

/**
 * Broadcast receiver to restart service after device boot
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction())) {

            Log.d(TAG, "Boot completed, restarting services");

            PreferencesHelper preferencesHelper = new PreferencesHelper(context);
            if (!preferencesHelper.isTrackingEnabled()) {
                Log.d(TAG, "Tracking disabled, not starting service");
                return;
            }

            // Start foreground service
            Intent serviceIntent = new Intent(context, LocationTrackingService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }

            // Schedule WorkManager periodic task
            scheduleLocationCheckWork(context);

            Log.d(TAG, "Services restarted successfully");
        }
    }

    /**
     * Schedule periodic location check using WorkManager
     */
    private void scheduleLocationCheckWork(Context context) {
        PeriodicWorkRequest locationCheckRequest = new PeriodicWorkRequest.Builder(
                LocationCheckWorker.class,
                15, TimeUnit.MINUTES) // Check every 15 minutes
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "location_check_work",
                ExistingPeriodicWorkPolicy.KEEP,
                locationCheckRequest);

        Log.d(TAG, "Scheduled location check work");
    }
}

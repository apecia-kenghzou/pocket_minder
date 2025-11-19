package com.pocketminder.service;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.pocketminder.Constants;
import com.pocketminder.model.Supermarket;
import com.pocketminder.receiver.GeofenceBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing geofences around supermarkets
 * More battery-efficient than continuous location tracking
 */
public class GeofencingService {
    private static final String TAG = "GeofencingService";
    private static final long GEOFENCE_EXPIRATION_MS = 24 * 60 * 60 * 1000; // 24 hours

    private final Context context;
    private final GeofencingClient geofencingClient;
    private PendingIntent geofencePendingIntent;

    public GeofencingService(Context context) {
        this.context = context.getApplicationContext();
        this.geofencingClient = LocationServices.getGeofencingClient(context);
    }

    /**
     * Add geofences for nearby supermarkets
     */
    public void addGeofences(List<Supermarket> supermarkets, Location currentLocation) {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        // Create geofence list
        List<Geofence> geofenceList = new ArrayList<>();

        for (Supermarket supermarket : supermarkets) {
            // Only add geofences for nearby supermarkets (within 1km)
            float distance = supermarket.distanceTo(
                    currentLocation.getLatitude(),
                    currentLocation.getLongitude());

            if (distance <= 1000) { // 1km radius
                Geofence geofence = new Geofence.Builder()
                        .setRequestId(supermarket.getPlaceId())
                        .setCircularRegion(
                                supermarket.getLatitude(),
                                supermarket.getLongitude(),
                                Constants.PROXIMITY_THRESHOLD_METERS)
                        .setExpirationDuration(GEOFENCE_EXPIRATION_MS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                          Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setLoiteringDelay(30000) // 30 seconds
                        .build();

                geofenceList.add(geofence);
            }
        }

        if (geofenceList.isEmpty()) {
            Log.d(TAG, "No nearby supermarkets to geofence");
            return;
        }

        // Create geofencing request
        GeofencingRequest geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build();

        // Add geofences
        geofencingClient.addGeofences(geofencingRequest, getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofences added successfully: " + geofenceList.size());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add geofences", e);
                });
    }

    /**
     * Remove all geofences
     */
    public void removeGeofences() {
        geofencingClient.removeGeofences(getGeofencePendingIntent())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Geofences removed successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove geofences", e);
                });
    }

    /**
     * Get pending intent for geofence transitions
     */
    private PendingIntent getGeofencePendingIntent() {
        if (geofencePendingIntent != null) {
            return geofencePendingIntent;
        }

        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        geofencePendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        return geofencePendingIntent;
    }

    /**
     * Update geofences with new location
     */
    public void updateGeofences(List<Supermarket> supermarkets, Location currentLocation) {
        removeGeofences();
        addGeofences(supermarkets, currentLocation);
    }
}

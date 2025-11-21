package com.pocketminder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.pocketminder.api.GooglePlacesAPI;
import com.pocketminder.database.SupermarketCacheManager;
import com.pocketminder.model.Supermarket;
import com.pocketminder.util.PreferencesHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Settings Activity for user customization
 */
public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private Switch switchTracking;
    private Switch switchGeofencing;
    private Switch switchDeveloperMode;
    private SeekBar seekBarRadius;
    private SeekBar seekBarDevRange;
    private TextView tvRadiusValue;
    private TextView tvDevRangeValue;
    private LinearLayout layoutDevRange;
    private Spinner spinnerCooldown;
    private Button btnSave;
    private Button btnManualFetch;
    private TextView tvLastFetch;

    private PreferencesHelper preferencesHelper;
    private GooglePlacesAPI placesAPI;
    private SupermarketCacheManager cacheManager;
    private FusedLocationProviderClient fusedLocationClient;
    private ExecutorService executorService;

    private int[] cooldownOptions = {1, 2, 4, 8, 12, 24}; // hours

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        preferencesHelper = new PreferencesHelper(this);
        placesAPI = new GooglePlacesAPI(this);
        cacheManager = new SupermarketCacheManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        executorService = Executors.newSingleThreadExecutor();

        initializeViews();
        loadSettings();
        setupListeners();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void initializeViews() {
        switchTracking = findViewById(R.id.switchTracking);
        switchGeofencing = findViewById(R.id.switchGeofencing);
        switchDeveloperMode = findViewById(R.id.switchDeveloperMode);
        seekBarRadius = findViewById(R.id.seekBarRadius);
        seekBarDevRange = findViewById(R.id.seekBarDevRange);
        tvRadiusValue = findViewById(R.id.tvRadiusValue);
        tvDevRangeValue = findViewById(R.id.tvDevRangeValue);
        layoutDevRange = findViewById(R.id.layoutDevRange);
        spinnerCooldown = findViewById(R.id.spinnerCooldown);
        btnSave = findViewById(R.id.btnSave);
        btnManualFetch = findViewById(R.id.btnManualFetch);
        tvLastFetch = findViewById(R.id.tvLastFetch);

        // Setup cooldown spinner
        String[] cooldownLabels = new String[cooldownOptions.length];
        for (int i = 0; i < cooldownOptions.length; i++) {
            cooldownLabels[i] = cooldownOptions[i] + " hour" + (cooldownOptions[i] > 1 ? "s" : "");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, cooldownLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCooldown.setAdapter(adapter);
    }

    private void loadSettings() {
        switchTracking.setChecked(preferencesHelper.isTrackingEnabled());
        switchGeofencing.setChecked(preferencesHelper.isGeofencingEnabled());

        boolean devMode = preferencesHelper.isDeveloperModeEnabled();
        switchDeveloperMode.setChecked(devMode);
        layoutDevRange.setVisibility(devMode ? View.VISIBLE : View.GONE);

        int radius = preferencesHelper.getNotificationRadius();
        seekBarRadius.setProgress(radius - 100); // 100-500m range
        updateRadiusLabel(radius);

        int devRange = preferencesHelper.getProximityRange();
        if (devMode && devRange > 500) {
            seekBarDevRange.setProgress(devRange - 1000); // 1km-10km range
            updateDevRangeLabel(devRange);
        } else {
            seekBarDevRange.setProgress(4000); // Default 5km
            updateDevRangeLabel(5000);
        }

        // Load cooldown setting
        int cooldownHours = preferencesHelper.getNotificationCooldownHours();
        int spinnerPosition = 0;
        for (int i = 0; i < cooldownOptions.length; i++) {
            if (cooldownOptions[i] == cooldownHours) {
                spinnerPosition = i;
                break;
            }
        }
        spinnerCooldown.setSelection(spinnerPosition);

        // Update last fetch time display
        updateLastFetchDisplay();
    }

    private void updateLastFetchDisplay() {
        long lastFetch = preferencesHelper.getLastManualFetch();
        if (lastFetch == 0) {
            tvLastFetch.setText("Last refresh: Never");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dateStr = sdf.format(new Date(lastFetch));
            tvLastFetch.setText("Last refresh: " + dateStr);
        }
    }

    private void setupListeners() {
        seekBarRadius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int radius = progress + 100; // Convert to 100-500m
                updateRadiusLabel(radius);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarDevRange.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int range = progress + 1000; // Convert to 1km-10km (1000m-10000m)
                updateDevRangeLabel(range);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnSave.setOnClickListener(v -> saveSettings());

        // Info about geofencing
        switchGeofencing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this,
                        "Geofencing is more battery-efficient than continuous tracking",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Toggle developer mode UI
        switchDeveloperMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutDevRange.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (isChecked) {
                Toast.makeText(this,
                        "Developer mode: You'll get notifications from stores up to " +
                                (seekBarDevRange.getProgress() + 1000) + "m away",
                        Toast.LENGTH_LONG).show();
            }
        });

        // Manual fetch button
        btnManualFetch.setOnClickListener(v -> manualFetch());
    }

    private void updateRadiusLabel(int radius) {
        tvRadiusValue.setText(radius + "m");
    }

    private void updateDevRangeLabel(int range) {
        if (range >= 1000) {
            float rangeKm = range / 1000.0f;
            tvDevRangeValue.setText(String.format("%dm (%.1fkm)", range, rangeKm));
        } else {
            tvDevRangeValue.setText(range + "m");
        }
    }

    private void saveSettings() {
        preferencesHelper.setTrackingEnabled(switchTracking.isChecked());
        preferencesHelper.setGeofencingEnabled(switchGeofencing.isChecked());

        int radius = seekBarRadius.getProgress() + 100;
        preferencesHelper.setNotificationRadius(radius);

        // Save cooldown setting
        int selectedCooldownIndex = spinnerCooldown.getSelectedItemPosition();
        int cooldownHours = cooldownOptions[selectedCooldownIndex];
        preferencesHelper.setNotificationCooldownHours(cooldownHours);

        // Save developer mode settings
        boolean devMode = switchDeveloperMode.isChecked();
        preferencesHelper.setDeveloperModeEnabled(devMode);

        if (devMode) {
            int devRange = seekBarDevRange.getProgress() + 1000; // 1km-10km
            preferencesHelper.setDevProximityRange(devRange);
            Toast.makeText(this,
                    "Settings saved! Developer mode enabled with " + devRange + "m range",
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    private void manualFetch() {
        // Check cooldown first
        if (!preferencesHelper.canManualFetch()) {
            long remainingMinutes = preferencesHelper.getManualFetchCooldownMinutes();
            Toast.makeText(this,
                    "Please wait " + remainingMinutes + " minutes before refreshing again",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // Check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required to fetch stores", Toast.LENGTH_LONG).show();
            return;
        }

        // Show progress
        btnManualFetch.setEnabled(false);
        btnManualFetch.setText("Refreshing...");
        Toast.makeText(this, "Fetching stores from your location...", Toast.LENGTH_SHORT).show();

        // Get current location
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                // Location not available
                runOnUiThread(() -> {
                    btnManualFetch.setEnabled(true);
                    btnManualFetch.setText("Refresh Store Database");
                    Toast.makeText(this, "Could not get location. Please try again.", Toast.LENGTH_LONG).show();
                });
                return;
            }

            // Fetch stores in background
            executorService.execute(() -> {
                try {
                    Log.d(TAG, "Manual fetch from location: " + location.getLatitude() + ", " + location.getLongitude());

                    // Fetch from API (bypasses cache)
                    List<Supermarket> stores = placesAPI.searchNearbySupermarkets(
                            location.getLatitude(),
                            location.getLongitude(),
                            Constants.CACHE_COVERAGE_RADIUS_METERS);

                    if (!stores.isEmpty()) {
                        // Update cache
                        cacheManager.cacheSupermarkets(stores,
                                location.getLatitude(),
                                location.getLongitude());

                        // Update last fetch time
                        preferencesHelper.setLastManualFetch(System.currentTimeMillis());

                        // Show success
                        runOnUiThread(() -> {
                            btnManualFetch.setEnabled(true);
                            btnManualFetch.setText("Refresh Store Database");
                            updateLastFetchDisplay();
                            Toast.makeText(this,
                                    "✅ Found " + stores.size() + " stores and updated database!",
                                    Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Manual fetch successful: " + stores.size() + " stores");
                        });
                    } else {
                        // No stores found
                        runOnUiThread(() -> {
                            btnManualFetch.setEnabled(true);
                            btnManualFetch.setText("Refresh Store Database");
                            Toast.makeText(this,
                                    "No stores found within 10km. Try again from a different location.",
                                    Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Manual fetch found no stores");
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error during manual fetch", e);
                    runOnUiThread(() -> {
                        btnManualFetch.setEnabled(true);
                        btnManualFetch.setText("Refresh Store Database");
                        Toast.makeText(this,
                                "❌ Error fetching stores. Please check your internet connection.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location", e);
            btnManualFetch.setEnabled(true);
            btnManualFetch.setText("Refresh Store Database");
            Toast.makeText(this, "Failed to get location. Please try again.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

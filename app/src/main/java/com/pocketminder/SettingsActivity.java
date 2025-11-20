package com.pocketminder;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pocketminder.util.PreferencesHelper;

/**
 * Settings Activity for user customization
 */
public class SettingsActivity extends AppCompatActivity {
    private Switch switchTracking;
    private Switch switchGeofencing;
    private Switch switchDeveloperMode;
    private SeekBar seekBarRadius;
    private SeekBar seekBarDevRange;
    private TextView tvRadiusValue;
    private TextView tvDevRangeValue;
    private LinearLayout layoutDevRange;
    private Button btnSave;

    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        preferencesHelper = new PreferencesHelper(this);

        initializeViews();
        loadSettings();
        setupListeners();
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
        btnSave = findViewById(R.id.btnSave);
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

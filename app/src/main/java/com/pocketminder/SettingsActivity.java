package com.pocketminder;

import android.os.Bundle;
import android.widget.Button;
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
    private SeekBar seekBarRadius;
    private TextView tvRadiusValue;
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
        seekBarRadius = findViewById(R.id.seekBarRadius);
        tvRadiusValue = findViewById(R.id.tvRadiusValue);
        btnSave = findViewById(R.id.btnSave);
    }

    private void loadSettings() {
        switchTracking.setChecked(preferencesHelper.isTrackingEnabled());
        switchGeofencing.setChecked(preferencesHelper.isGeofencingEnabled());

        int radius = preferencesHelper.getNotificationRadius();
        seekBarRadius.setProgress(radius - 100); // 100-500m range
        updateRadiusLabel(radius);
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

        btnSave.setOnClickListener(v -> saveSettings());

        // Info about geofencing
        switchGeofencing.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this,
                        "Geofencing is more battery-efficient than continuous tracking",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateRadiusLabel(int radius) {
        tvRadiusValue.setText(radius + "m");
    }

    private void saveSettings() {
        preferencesHelper.setTrackingEnabled(switchTracking.isChecked());
        preferencesHelper.setGeofencingEnabled(switchGeofencing.isChecked());

        int radius = seekBarRadius.getProgress() + 100;
        preferencesHelper.setNotificationRadius(radius);

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

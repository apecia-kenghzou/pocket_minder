package com.pocketminder;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pocketminder.database.SupermarketCacheManager;
import com.pocketminder.util.PreferencesHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Activity for selecting which store types to receive notifications for
 * Dynamically shows stores found in cache - works in any country!
 */
public class StorePreferencesActivity extends AppCompatActivity {
    private static final String TAG = "StorePreferencesActivity";

    private LinearLayout storeListContainer;
    private Button btnSavePreferences;
    private Button btnSelectAll;
    private Button btnDeselectAll;
    private TextView tvNoStores;

    private PreferencesHelper preferencesHelper;
    private SupermarketCacheManager cacheManager;
    private Map<String, CheckBox> storeCheckboxes;
    private List<String> storeNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_preferences);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Store Notifications");

        preferencesHelper = new PreferencesHelper(this);
        cacheManager = new SupermarketCacheManager(this);
        storeCheckboxes = new HashMap<>();

        initializeViews();
        loadStoresAndPreferences();
        setupListeners();
    }

    private void initializeViews() {
        storeListContainer = findViewById(R.id.storeListContainer);
        btnSavePreferences = findViewById(R.id.btnSavePreferences);
        btnSelectAll = findViewById(R.id.btnSelectAll);
        btnDeselectAll = findViewById(R.id.btnDeselectAll);
        tvNoStores = findViewById(R.id.tvNoStores);
    }

    private void loadStoresAndPreferences() {
        // Get all unique store names from cache
        storeNames = cacheManager.getAllUniqueStoreNames();

        if (storeNames.isEmpty()) {
            // No stores in cache
            tvNoStores.setText("No stores found in cache.\n\nGo to Settings and tap \"Refresh Store Database\" to fetch stores from your location.");
            tvNoStores.setVisibility(android.view.View.VISIBLE);
            storeListContainer.setVisibility(android.view.View.GONE);
            btnSelectAll.setEnabled(false);
            btnDeselectAll.setEnabled(false);
            btnSavePreferences.setEnabled(false);
            Log.d(TAG, "No stores found in cache");
            return;
        }

        // Hide "no stores" message
        tvNoStores.setVisibility(android.view.View.GONE);
        storeListContainer.setVisibility(android.view.View.VISIBLE);
        btnSelectAll.setEnabled(true);
        btnDeselectAll.setEnabled(true);
        btnSavePreferences.setEnabled(true);

        // Create checkboxes dynamically for each store
        for (String storeName : storeNames) {
            CheckBox checkbox = new CheckBox(this);
            checkbox.setText(storeName);
            checkbox.setTextSize(16);
            checkbox.setPadding(16, 16, 16, 16);

            // Load preference for this store
            String storeKey = getStoreKey(storeName);
            boolean isEnabled = preferencesHelper.isStoreTypeEnabled(storeKey);
            checkbox.setChecked(isEnabled);

            // Add to container
            storeListContainer.addView(checkbox);
            storeCheckboxes.put(storeName, checkbox);
        }

        Log.d(TAG, "Loaded " + storeNames.size() + " stores dynamically");
    }

    private void setupListeners() {
        btnSavePreferences.setOnClickListener(v -> savePreferences());
        btnSelectAll.setOnClickListener(v -> toggleAllStores(true));
        btnDeselectAll.setOnClickListener(v -> toggleAllStores(false));
    }

    private void toggleAllStores(boolean enabled) {
        for (CheckBox checkbox : storeCheckboxes.values()) {
            checkbox.setChecked(enabled);
        }
        Toast.makeText(this, enabled ? "All stores selected" : "All stores deselected", Toast.LENGTH_SHORT).show();
    }

    private void savePreferences() {
        int enabledCount = 0;

        // Save each store preference
        for (Map.Entry<String, CheckBox> entry : storeCheckboxes.entrySet()) {
            String storeName = entry.getKey();
            CheckBox checkbox = entry.getValue();
            String storeKey = getStoreKey(storeName);

            preferencesHelper.setStoreTypeEnabled(storeKey, checkbox.isChecked());

            if (checkbox.isChecked()) {
                enabledCount++;
            }
        }

        if (enabledCount == 0) {
            Toast.makeText(this, "Please select at least one store", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "✅ Preferences saved! (" + enabledCount + " stores enabled)", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Saved preferences for " + enabledCount + " stores");
        finish();
    }

    /**
     * Convert store name to preference key
     * Examples: "99 Speedmart" -> "store_99_speedmart"
     *           "Walmart" -> "store_walmart"
     */
    private String getStoreKey(String storeName) {
        return "store_" + storeName.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

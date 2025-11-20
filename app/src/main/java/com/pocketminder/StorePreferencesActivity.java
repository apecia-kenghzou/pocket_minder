package com.pocketminder;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.pocketminder.util.PreferencesHelper;

/**
 * Activity for selecting which store types to receive notifications for
 */
public class StorePreferencesActivity extends AppCompatActivity {

    private CheckBox cb99Speedmart;
    private CheckBox cbKKMart;
    private CheckBox cbCaringPharmacy;
    private CheckBox cbWatsons;
    private CheckBox cbGuardian;
    private CheckBox cbLotus;
    private CheckBox cbJayaGrocer;
    private CheckBox cbOtherSupermarkets;
    private Button btnSavePreferences;

    private PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_preferences);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Store Notifications");

        preferencesHelper = new PreferencesHelper(this);

        initializeViews();
        loadPreferences();
        setupListeners();
    }

    private void initializeViews() {
        cb99Speedmart = findViewById(R.id.cb99Speedmart);
        cbKKMart = findViewById(R.id.cbKKMart);
        cbCaringPharmacy = findViewById(R.id.cbCaringPharmacy);
        cbWatsons = findViewById(R.id.cbWatsons);
        cbGuardian = findViewById(R.id.cbGuardian);
        cbLotus = findViewById(R.id.cbLotus);
        cbJayaGrocer = findViewById(R.id.cbJayaGrocer);
        cbOtherSupermarkets = findViewById(R.id.cbOtherSupermarkets);
        btnSavePreferences = findViewById(R.id.btnSavePreferences);
    }

    private void loadPreferences() {
        cb99Speedmart.setChecked(preferencesHelper.isStoreTypeEnabled("99speedmart"));
        cbKKMart.setChecked(preferencesHelper.isStoreTypeEnabled("kkmart"));
        cbCaringPharmacy.setChecked(preferencesHelper.isStoreTypeEnabled("caring"));
        cbWatsons.setChecked(preferencesHelper.isStoreTypeEnabled("watsons"));
        cbGuardian.setChecked(preferencesHelper.isStoreTypeEnabled("guardian"));
        cbLotus.setChecked(preferencesHelper.isStoreTypeEnabled("lotus"));
        cbJayaGrocer.setChecked(preferencesHelper.isStoreTypeEnabled("jayagrocer"));
        cbOtherSupermarkets.setChecked(preferencesHelper.isStoreTypeEnabled("other"));
    }

    private void setupListeners() {
        btnSavePreferences.setOnClickListener(v -> savePreferences());
    }

    private void savePreferences() {
        preferencesHelper.setStoreTypeEnabled("99speedmart", cb99Speedmart.isChecked());
        preferencesHelper.setStoreTypeEnabled("kkmart", cbKKMart.isChecked());
        preferencesHelper.setStoreTypeEnabled("caring", cbCaringPharmacy.isChecked());
        preferencesHelper.setStoreTypeEnabled("watsons", cbWatsons.isChecked());
        preferencesHelper.setStoreTypeEnabled("guardian", cbGuardian.isChecked());
        preferencesHelper.setStoreTypeEnabled("lotus", cbLotus.isChecked());
        preferencesHelper.setStoreTypeEnabled("jayagrocer", cbJayaGrocer.isChecked());
        preferencesHelper.setStoreTypeEnabled("other", cbOtherSupermarkets.isChecked());

        // Count how many stores are enabled
        int enabledCount = getEnabledStoresCount();

        if (enabledCount == 0) {
            Toast.makeText(this, "Please select at least one store type", Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, "Preferences saved! (" + enabledCount + " store types)", Toast.LENGTH_SHORT).show();
        finish();
    }

    private int getEnabledStoresCount() {
        int count = 0;
        if (cb99Speedmart.isChecked()) count++;
        if (cbKKMart.isChecked()) count++;
        if (cbCaringPharmacy.isChecked()) count++;
        if (cbWatsons.isChecked()) count++;
        if (cbGuardian.isChecked()) count++;
        if (cbLotus.isChecked()) count++;
        if (cbJayaGrocer.isChecked()) count++;
        if (cbOtherSupermarkets.isChecked()) count++;
        return count;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

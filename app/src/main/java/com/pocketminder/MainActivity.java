package com.pocketminder;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pocketminder.adapter.ShoppingListAdapter;
import com.pocketminder.database.ShoppingListDBHelper;
import com.pocketminder.model.ShoppingItem;
import com.pocketminder.model.Supermarket;
import com.pocketminder.service.LocationTrackingService;
import com.pocketminder.util.PreferencesHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity - Shopping List Management and Location Tracking Control
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private ListView listViewItems;
    private Button btnAddItem;
    private Button btnStartTracking;
    private Button btnStopTracking;
    private Button btnClearPurchased;
    private TextView tvStatus;
    private TextView tvLocationInfo;

    private ShoppingListAdapter adapter;
    private List<ShoppingItem> shoppingItems;
    private ShoppingListDBHelper dbHelper;
    private PreferencesHelper preferencesHelper;

    private LocationTrackingService locationService;
    private boolean serviceBound = false;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = ShoppingListDBHelper.getInstance(this);
        preferencesHelper = new PreferencesHelper(this);

        initializeViews();
        setupPermissionLauncher();
        checkPermissions();
        loadShoppingList();
        setupClickListeners();

        // Show first run dialog
        if (preferencesHelper.isFirstRun()) {
            showWelcomeDialog();
            preferencesHelper.setFirstRun(false);
        }
    }

    /**
     * Initialize UI views
     */
    private void initializeViews() {
        listViewItems = findViewById(R.id.listViewItems);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnStartTracking = findViewById(R.id.btnStartTracking);
        btnStopTracking = findViewById(R.id.btnStopTracking);
        btnClearPurchased = findViewById(R.id.btnClearPurchased);
        tvStatus = findViewById(R.id.tvStatus);
        tvLocationInfo = findViewById(R.id.tvLocationInfo);

        shoppingItems = new ArrayList<>();
        adapter = new ShoppingListAdapter(this, shoppingItems, this::onItemCheckedChanged, this::onItemDeleted);
        listViewItems.setAdapter(adapter);
    }

    /**
     * Setup permission launcher for Android 13+
     */
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        Log.d(TAG, "All permissions granted");
                        startTrackingService();
                        requestBatteryOptimizationExemption();
                    } else {
                        Log.e(TAG, "Some permissions denied");
                        Toast.makeText(this, "Location permissions are required for this app to work", Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Check and request necessary permissions
     */
    private void checkPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            // All permissions granted, start service if tracking was enabled
            if (preferencesHelper.isTrackingEnabled()) {
                startTrackingService();
            }
        }
    }

    /**
     * Request battery optimization exemption for better background performance
     */
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(TAG, "Error requesting battery optimization exemption", e);
                }
            }
        }
    }

    /**
     * Setup click listeners
     */
    private void setupClickListeners() {
        btnAddItem.setOnClickListener(v -> showAddItemDialog());

        btnStartTracking.setOnClickListener(v -> {
            checkPermissions();
            startTrackingService();
        });

        btnStopTracking.setOnClickListener(v -> stopTrackingService());

        btnClearPurchased.setOnClickListener(v -> clearPurchasedItems());
    }

    /**
     * Show dialog to add new item
     */
    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null);

        EditText etItemName = dialogView.findViewById(R.id.etItemName);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);

        builder.setView(dialogView)
                .setTitle("Add Shopping Item")
                .setPositiveButton("Add", (dialog, which) -> {
                    String itemName = etItemName.getText().toString().trim();
                    String quantityStr = etQuantity.getText().toString().trim();

                    if (TextUtils.isEmpty(itemName)) {
                        Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int quantity = 1;
                    if (!TextUtils.isEmpty(quantityStr)) {
                        try {
                            quantity = Integer.parseInt(quantityStr);
                        } catch (NumberFormatException e) {
                            quantity = 1;
                        }
                    }

                    ShoppingItem item = new ShoppingItem(itemName, quantity);
                    long id = dbHelper.addShoppingItem(item);
                    item.setId(id);

                    shoppingItems.add(0, item);
                    adapter.notifyDataSetChanged();

                    Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Load shopping list from database
     */
    private void loadShoppingList() {
        shoppingItems.clear();
        shoppingItems.addAll(dbHelper.getAllShoppingItems());
        adapter.notifyDataSetChanged();
        updateItemCount();
    }

    /**
     * Handle item checked/unchecked
     */
    private void onItemCheckedChanged(ShoppingItem item, boolean isChecked) {
        item.setPurchased(isChecked);
        dbHelper.updateShoppingItem(item);
        updateItemCount();
    }

    /**
     * Handle item deletion
     */
    private void onItemDeleted(ShoppingItem item) {
        dbHelper.deleteShoppingItem(item.getId());
        shoppingItems.remove(item);
        adapter.notifyDataSetChanged();
        updateItemCount();
        Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Clear all purchased items
     */
    private void clearPurchasedItems() {
        dbHelper.deletePurchasedItems();
        loadShoppingList();
        Toast.makeText(this, "Purchased items cleared", Toast.LENGTH_SHORT).show();
    }

    /**
     * Update item count display
     */
    private void updateItemCount() {
        int total = shoppingItems.size();
        int unpurchased = 0;
        for (ShoppingItem item : shoppingItems) {
            if (!item.isPurchased()) {
                unpurchased++;
            }
        }
        setTitle("Shopping List (" + unpurchased + "/" + total + ")");
    }

    /**
     * Start location tracking service
     */
    private void startTrackingService() {
        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        preferencesHelper.setTrackingEnabled(true);
        updateTrackingStatus(true);

        Toast.makeText(this, "Location tracking started", Toast.LENGTH_SHORT).show();
    }

    /**
     * Stop location tracking service
     */
    private void stopTrackingService() {
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }

        Intent serviceIntent = new Intent(this, LocationTrackingService.class);
        stopService(serviceIntent);

        preferencesHelper.setTrackingEnabled(false);
        updateTrackingStatus(false);

        Toast.makeText(this, "Location tracking stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * Update tracking status UI
     */
    private void updateTrackingStatus(boolean isTracking) {
        if (isTracking) {
            tvStatus.setText("Status: Tracking Active");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnStartTracking.setEnabled(false);
            btnStopTracking.setEnabled(true);
        } else {
            tvStatus.setText("Status: Tracking Stopped");
            tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnStartTracking.setEnabled(true);
            btnStopTracking.setEnabled(false);
            tvLocationInfo.setText("");
        }
    }

    /**
     * Service connection for binding to LocationTrackingService
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationTrackingService.LocalBinder binder = (LocationTrackingService.LocalBinder) service;
            locationService = binder.getService();
            serviceBound = true;

            // Update UI with current location
            updateLocationInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            locationService = null;
        }
    };

    /**
     * Update location information display
     */
    private void updateLocationInfo() {
        if (serviceBound && locationService != null) {
            Location location = locationService.getLastLocation();
            List<Supermarket> supermarkets = locationService.getNearbySupermarkets();

            if (location != null) {
                String locationText = String.format("Current Location:\nLat: %.4f, Lng: %.4f\n",
                        location.getLatitude(), location.getLongitude());

                if (!supermarkets.isEmpty()) {
                    locationText += "\nNearby Supermarkets: " + supermarkets.size();
                    Supermarket nearest = supermarkets.get(0);
                    float distance = nearest.distanceTo(location.getLatitude(), location.getLongitude());
                    locationText += String.format("\nNearest: %s (%.0fm)", nearest.getName(), distance);
                }

                tvLocationInfo.setText(locationText);
            }

            // Schedule next update
            tvLocationInfo.postDelayed(this::updateLocationInfo, 5000);
        }
    }

    /**
     * Show welcome dialog on first run
     */
    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Welcome to Pocket Minder!")
                .setMessage("This app will help you remember to buy items when you're near a supermarket.\n\n" +
                        "Features:\n" +
                        "• Add items to your shopping list\n" +
                        "• Get notified when you're near a supermarket\n" +
                        "• Works in the background even when app is closed\n\n" +
                        "Please grant location permissions to get started.")
                .setPositiveButton("Get Started", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadShoppingList();
        updateTrackingStatus(preferencesHelper.isTrackingEnabled());

        if (serviceBound && locationService != null) {
            updateLocationInfo();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
        }
    }
}

package com.pocketminder.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.pocketminder.Constants;
import com.pocketminder.MainActivity;
import com.pocketminder.database.ShoppingListDBHelper;
import com.pocketminder.model.ShoppingItem;

import java.util.List;

/**
 * Receiver for notification action buttons
 */
public class NotificationActionReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationAction";

    public static final String EXTRA_SUPERMARKET_LAT = "supermarket_lat";
    public static final String EXTRA_SUPERMARKET_LNG = "supermarket_lng";
    public static final String EXTRA_SUPERMARKET_NAME = "supermarket_name";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action == null) {
            return;
        }

        switch (action) {
            case Constants.ACTION_VIEW_LIST:
                handleViewList(context);
                break;

            case Constants.ACTION_NAVIGATE:
                handleNavigate(context, intent);
                break;

            case Constants.ACTION_MARK_PURCHASED:
                handleMarkAllPurchased(context);
                break;

            default:
                Log.w(TAG, "Unknown action: " + action);
        }
    }

    /**
     * Open app to view shopping list
     */
    private void handleViewList(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    /**
     * Open navigation to supermarket
     */
    private void handleNavigate(Context context, Intent intent) {
        double lat = intent.getDoubleExtra(EXTRA_SUPERMARKET_LAT, 0);
        double lng = intent.getDoubleExtra(EXTRA_SUPERMARKET_LNG, 0);
        String name = intent.getStringExtra(EXTRA_SUPERMARKET_NAME);

        if (lat != 0 && lng != 0) {
            // Open Google Maps for navigation
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng + "&mode=d");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mapIntent);
                Log.d(TAG, "Opened navigation to " + name);
            } else {
                // Fallback to browser
                Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(browserIntent);
            }
        }
    }

    /**
     * Mark all items as purchased
     */
    private void handleMarkAllPurchased(Context context) {
        ShoppingListDBHelper dbHelper = ShoppingListDBHelper.getInstance(context);
        List<ShoppingItem> items = dbHelper.getAllShoppingItems();

        for (ShoppingItem item : items) {
            if (!item.isPurchased()) {
                item.setPurchased(true);
                dbHelper.updateShoppingItem(item);
            }
        }

        Log.d(TAG, "Marked all items as purchased");
    }
}

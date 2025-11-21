package com.pocketminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.pocketminder.Constants;
import com.pocketminder.model.Supermarket;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache manager for supermarket locations
 * Coverage: 10km radius (fetch stores once, use as user moves around)
 * Expiry: 7 days (stores don't change often)
 * Reduces API calls by 99% and enables offline mode
 */
public class SupermarketCacheManager {
    private static final String TAG = "SupermarketCache";

    private static final String TABLE_CACHE = "supermarket_cache";
    private static final String COLUMN_PLACE_ID = "place_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_ADDRESS = "address";
    private static final String COLUMN_VICINITY = "vicinity";
    private static final String COLUMN_RATING = "rating";
    private static final String COLUMN_IS_OPEN = "is_open";
    private static final String COLUMN_SEARCH_LAT = "search_lat";
    private static final String COLUMN_SEARCH_LNG = "search_lng";
    private static final String COLUMN_CACHED_TIME = "cached_time";

    private final ShoppingListDBHelper dbHelper;

    public SupermarketCacheManager(Context context) {
        this.dbHelper = ShoppingListDBHelper.getInstance(context);
    }

    /**
     * Create cache table in database
     */
    public static void createTable(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_CACHE + " ("
                + COLUMN_PLACE_ID + " TEXT PRIMARY KEY, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_LATITUDE + " REAL NOT NULL, "
                + COLUMN_LONGITUDE + " REAL NOT NULL, "
                + COLUMN_ADDRESS + " TEXT, "
                + COLUMN_VICINITY + " TEXT, "
                + COLUMN_RATING + " REAL DEFAULT 0, "
                + COLUMN_IS_OPEN + " INTEGER DEFAULT 0, "
                + COLUMN_SEARCH_LAT + " REAL NOT NULL, "
                + COLUMN_SEARCH_LNG + " REAL NOT NULL, "
                + COLUMN_CACHED_TIME + " INTEGER NOT NULL"
                + ")";
        db.execSQL(CREATE_TABLE);

        // Create index for faster location-based queries
        String CREATE_INDEX = "CREATE INDEX IF NOT EXISTS idx_cache_location ON "
                + TABLE_CACHE + "(" + COLUMN_SEARCH_LAT + ", " + COLUMN_SEARCH_LNG + ")";
        db.execSQL(CREATE_INDEX);
    }

    /**
     * Get cached supermarkets for a location
     */
    public List<Supermarket> getCachedSupermarkets(double lat, double lng) {
        List<Supermarket> supermarkets = new ArrayList<>();

        // Check if we have cached results for this location (within 10km coverage area)
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_CACHE + " WHERE "
                + "ABS(" + COLUMN_SEARCH_LAT + " - ?) < " + Constants.CACHE_RADIUS_DEGREES + " AND "
                + "ABS(" + COLUMN_SEARCH_LNG + " - ?) < " + Constants.CACHE_RADIUS_DEGREES + " AND "
                + COLUMN_CACHED_TIME + " > ?";

        long expiryTime = System.currentTimeMillis() - Constants.CACHE_EXPIRY_MS;

        Cursor cursor = db.rawQuery(query, new String[]{
                String.valueOf(lat),
                String.valueOf(lng),
                String.valueOf(expiryTime)
        });

        if (cursor.moveToFirst()) {
            do {
                Supermarket supermarket = new Supermarket();
                supermarket.setPlaceId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLACE_ID)));
                supermarket.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
                supermarket.setLatitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)));
                supermarket.setLongitude(cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)));
                supermarket.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)));
                supermarket.setVicinity(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_VICINITY)));
                supermarket.setRating(cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RATING)));
                supermarket.setOpen(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_OPEN)) == 1);

                supermarkets.add(supermarket);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return supermarkets;
    }

    /**
     * Cache supermarket results
     */
    public void cacheSupermarkets(List<Supermarket> supermarkets, double searchLat, double searchLng) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long currentTime = System.currentTimeMillis();

        db.beginTransaction();
        try {
            for (Supermarket supermarket : supermarkets) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_PLACE_ID, supermarket.getPlaceId());
                values.put(COLUMN_NAME, supermarket.getName());
                values.put(COLUMN_LATITUDE, supermarket.getLatitude());
                values.put(COLUMN_LONGITUDE, supermarket.getLongitude());
                values.put(COLUMN_ADDRESS, supermarket.getAddress());
                values.put(COLUMN_VICINITY, supermarket.getVicinity());
                values.put(COLUMN_RATING, supermarket.getRating());
                values.put(COLUMN_IS_OPEN, supermarket.isOpen() ? 1 : 0);
                values.put(COLUMN_SEARCH_LAT, searchLat);
                values.put(COLUMN_SEARCH_LNG, searchLng);
                values.put(COLUMN_CACHED_TIME, currentTime);

                db.insertWithOnConflict(TABLE_CACHE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Check if we have valid cached data for a location
     */
    public boolean hasCachedData(double lat, double lng) {
        return !getCachedSupermarkets(lat, lng).isEmpty();
    }

    /**
     * Clear expired cache entries
     */
    public void clearExpiredCache() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long expiryTime = System.currentTimeMillis() - Constants.CACHE_EXPIRY_MS;

        int deleted = db.delete(TABLE_CACHE, COLUMN_CACHED_TIME + " < ?",
                new String[]{String.valueOf(expiryTime)});

        android.util.Log.d(TAG, "Cleared " + deleted + " expired cache entries");
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_CACHE, null, null);
    }

    /**
     * Get all unique store names from valid (non-expired) cache
     * Used for dynamic store preferences
     * @return List of unique store names sorted alphabetically
     */
    public List<String> getAllUniqueStoreNames() {
        List<String> storeNames = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        long expiryTime = System.currentTimeMillis() - Constants.CACHE_EXPIRY_MS;

        // Get distinct store names from valid cache entries
        String query = "SELECT DISTINCT " + COLUMN_NAME +
                " FROM " + TABLE_CACHE +
                " WHERE " + COLUMN_CACHED_TIME + " > ?" +
                " ORDER BY " + COLUMN_NAME + " ASC";

        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(expiryTime)});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) {
                    storeNames.add(name);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();

        android.util.Log.d(TAG, "Found " + storeNames.size() + " unique store names in cache");
        return storeNames;
    }

    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        CacheStats stats = new CacheStats();

        // Total cached entries
        Cursor totalCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CACHE, null);
        if (totalCursor.moveToFirst()) {
            stats.totalEntries = totalCursor.getInt(0);
        }
        totalCursor.close();

        // Valid entries (not expired)
        long expiryTime = System.currentTimeMillis() - Constants.CACHE_EXPIRY_MS;
        Cursor validCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CACHE +
                " WHERE " + COLUMN_CACHED_TIME + " > ?",
                new String[]{String.valueOf(expiryTime)});
        if (validCursor.moveToFirst()) {
            stats.validEntries = validCursor.getInt(0);
        }
        validCursor.close();

        stats.expiredEntries = stats.totalEntries - stats.validEntries;
        return stats;
    }

    /**
     * Cache statistics data class
     */
    public static class CacheStats {
        public int totalEntries;
        public int validEntries;
        public int expiredEntries;

        @Override
        public String toString() {
            return "CacheStats{total=" + totalEntries +
                   ", valid=" + validEntries +
                   ", expired=" + expiredEntries + "}";
        }
    }
}

package com.pocketminder.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.pocketminder.model.ShoppingItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Database helper for managing shopping list items
 */
public class ShoppingListDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "pocket_minder.db";
    private static final int DATABASE_VERSION = 2;

    // Table name and columns
    private static final String TABLE_SHOPPING_LIST = "shopping_list";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_ITEM_NAME = "item_name";
    private static final String COLUMN_QUANTITY = "quantity";
    private static final String COLUMN_IS_PURCHASED = "is_purchased";
    private static final String COLUMN_CREATED_TIMESTAMP = "created_timestamp";

    private static ShoppingListDBHelper instance;

    public static synchronized ShoppingListDBHelper getInstance(Context context) {
        if (instance == null) {
            instance = new ShoppingListDBHelper(context.getApplicationContext());
        }
        return instance;
    }

    private ShoppingListDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_SHOPPING_LIST + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ITEM_NAME + " TEXT NOT NULL, "
                + COLUMN_QUANTITY + " INTEGER DEFAULT 1, "
                + COLUMN_IS_PURCHASED + " INTEGER DEFAULT 0, "
                + COLUMN_CREATED_TIMESTAMP + " INTEGER NOT NULL"
                + ")";
        db.execSQL(CREATE_TABLE);

        // Create supermarket cache table
        SupermarketCacheManager.createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add cache table in version 2
            SupermarketCacheManager.createTable(db);
        }
    }

    /**
     * Add a new shopping item
     */
    public long addShoppingItem(ShoppingItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, item.getItemName());
        values.put(COLUMN_QUANTITY, item.getQuantity());
        values.put(COLUMN_IS_PURCHASED, item.isPurchased() ? 1 : 0);
        values.put(COLUMN_CREATED_TIMESTAMP, item.getCreatedTimestamp());

        long id = db.insert(TABLE_SHOPPING_LIST, null, values);
        item.setId(id);
        return id;
    }

    /**
     * Get all shopping items
     */
    public List<ShoppingItem> getAllShoppingItems() {
        List<ShoppingItem> items = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SHOPPING_LIST + " ORDER BY " + COLUMN_CREATED_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ShoppingItem item = new ShoppingItem();
                item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                item.setItemName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEM_NAME)));
                item.setQuantity(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_QUANTITY)));
                item.setPurchased(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_PURCHASED)) == 1);
                item.setCreatedTimestamp(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_TIMESTAMP)));
                items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return items;
    }

    /**
     * Get unpurchased items count
     */
    public int getUnpurchasedItemsCount() {
        String countQuery = "SELECT COUNT(*) FROM " + TABLE_SHOPPING_LIST + " WHERE " + COLUMN_IS_PURCHASED + " = 0";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    /**
     * Update shopping item
     */
    public int updateShoppingItem(ShoppingItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ITEM_NAME, item.getItemName());
        values.put(COLUMN_QUANTITY, item.getQuantity());
        values.put(COLUMN_IS_PURCHASED, item.isPurchased() ? 1 : 0);

        return db.update(TABLE_SHOPPING_LIST, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
    }

    /**
     * Delete shopping item
     */
    public void deleteShoppingItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SHOPPING_LIST, COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    /**
     * Delete all purchased items
     */
    public void deletePurchasedItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SHOPPING_LIST, COLUMN_IS_PURCHASED + " = 1", null);
    }

    /**
     * Clear all items
     */
    public void clearAllItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SHOPPING_LIST, null, null);
    }
}

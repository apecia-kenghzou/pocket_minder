package com.pocketminder;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.pocketminder.database.ShoppingListDBHelper;
import com.pocketminder.model.ShoppingItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for ShoppingListDBHelper
 */
@RunWith(RobolectricTestRunner.class)
public class ShoppingListDBHelperTest {

    private ShoppingListDBHelper dbHelper;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        dbHelper = ShoppingListDBHelper.getInstance(context);
        dbHelper.clearAllItems();
    }

    @After
    public void tearDown() {
        dbHelper.clearAllItems();
    }

    @Test
    public void testAddShoppingItem() {
        ShoppingItem item = new ShoppingItem("Milk", 2);
        long id = dbHelper.addShoppingItem(item);

        assertTrue("Item ID should be positive", id > 0);
        assertEquals("Item ID should be set", id, item.getId());
    }

    @Test
    public void testGetAllShoppingItems() {
        // Add multiple items
        dbHelper.addShoppingItem(new ShoppingItem("Milk", 2));
        dbHelper.addShoppingItem(new ShoppingItem("Bread", 1));
        dbHelper.addShoppingItem(new ShoppingItem("Eggs", 12));

        List<ShoppingItem> items = dbHelper.getAllShoppingItems();

        assertEquals("Should have 3 items", 3, items.size());
    }

    @Test
    public void testGetUnpurchasedItemsCount() {
        ShoppingItem item1 = new ShoppingItem("Milk", 2);
        ShoppingItem item2 = new ShoppingItem("Bread", 1);
        item2.setPurchased(true);

        dbHelper.addShoppingItem(item1);
        dbHelper.addShoppingItem(item2);

        int count = dbHelper.getUnpurchasedItemsCount();

        assertEquals("Should have 1 unpurchased item", 1, count);
    }

    @Test
    public void testUpdateShoppingItem() {
        ShoppingItem item = new ShoppingItem("Milk", 2);
        dbHelper.addShoppingItem(item);

        item.setItemName("Milk - Updated");
        item.setQuantity(3);
        item.setPurchased(true);

        int updated = dbHelper.updateShoppingItem(item);

        assertEquals("Should update 1 row", 1, updated);

        List<ShoppingItem> items = dbHelper.getAllShoppingItems();
        assertEquals("Item name should be updated", "Milk - Updated", items.get(0).getItemName());
        assertEquals("Quantity should be updated", 3, items.get(0).getQuantity());
        assertTrue("Item should be marked as purchased", items.get(0).isPurchased());
    }

    @Test
    public void testDeleteShoppingItem() {
        ShoppingItem item = new ShoppingItem("Milk", 2);
        long id = dbHelper.addShoppingItem(item);

        dbHelper.deleteShoppingItem(id);

        List<ShoppingItem> items = dbHelper.getAllShoppingItems();
        assertEquals("Should have 0 items after deletion", 0, items.size());
    }

    @Test
    public void testDeletePurchasedItems() {
        ShoppingItem item1 = new ShoppingItem("Milk", 2);
        ShoppingItem item2 = new ShoppingItem("Bread", 1);
        item1.setPurchased(true);

        dbHelper.addShoppingItem(item1);
        dbHelper.addShoppingItem(item2);

        dbHelper.deletePurchasedItems();

        List<ShoppingItem> items = dbHelper.getAllShoppingItems();
        assertEquals("Should have 1 item after deleting purchased", 1, items.size());
        assertFalse("Remaining item should not be purchased", items.get(0).isPurchased());
    }

    @Test
    public void testClearAllItems() {
        dbHelper.addShoppingItem(new ShoppingItem("Milk", 2));
        dbHelper.addShoppingItem(new ShoppingItem("Bread", 1));
        dbHelper.addShoppingItem(new ShoppingItem("Eggs", 12));

        dbHelper.clearAllItems();

        List<ShoppingItem> items = dbHelper.getAllShoppingItems();
        assertEquals("Should have 0 items after clearing all", 0, items.size());
    }
}

package com.pocketminder.model;

import java.io.Serializable;

/**
 * Model class representing a shopping list item
 */
public class ShoppingItem implements Serializable {
    private long id;
    private String itemName;
    private int quantity;
    private boolean isPurchased;
    private long createdTimestamp;

    public ShoppingItem() {
        this.createdTimestamp = System.currentTimeMillis();
        this.isPurchased = false;
        this.quantity = 1;
    }

    public ShoppingItem(String itemName, int quantity) {
        this();
        this.itemName = itemName;
        this.quantity = quantity;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    public void setCreatedTimestamp(long createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    @Override
    public String toString() {
        return itemName + (quantity > 1 ? " (x" + quantity + ")" : "");
    }
}

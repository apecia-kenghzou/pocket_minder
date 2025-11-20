package com.pocketminder.model;

import java.io.Serializable;

/**
 * Model class representing a supermarket location
 */
public class Supermarket implements Serializable {
    private String placeId;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private float rating;
    private boolean isOpen;
    private String vicinity;

    public Supermarket() {
    }

    public Supermarket(String placeId, String name, double latitude, double longitude) {
        this.placeId = placeId;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and Setters
    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public String getVicinity() {
        return vicinity;
    }

    public void setVicinity(String vicinity) {
        this.vicinity = vicinity;
    }

    /**
     * Calculate distance to a given location in meters
     */
    public float distanceTo(double lat, double lon) {
        float[] results = new float[1];
        android.location.Location.distanceBetween(latitude, longitude, lat, lon, results);
        return results[0];
    }

    @Override
    public String toString() {
        return name + " - " + (vicinity != null ? vicinity : address);
    }

    // Override equals and hashCode to properly deduplicate stores in HashSet
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supermarket that = (Supermarket) o;
        return placeId != null && placeId.equals(that.placeId);
    }

    @Override
    public int hashCode() {
        return placeId != null ? placeId.hashCode() : 0;
    }
}

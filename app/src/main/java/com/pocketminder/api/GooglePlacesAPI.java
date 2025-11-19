package com.pocketminder.api;

import android.content.Context;
import android.util.Log;

import com.pocketminder.model.Supermarket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for Google Places API integration
 */
public class GooglePlacesAPI {
    private static final String TAG = "GooglePlacesAPI";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_SUPERMARKET = "supermarket";
    private static final String TYPE_GROCERY = "grocery_or_supermarket";
    private static final int SEARCH_RADIUS = 2000; // 2km radius

    private final String apiKey;

    public GooglePlacesAPI(Context context) {
        // API key should be stored in gradle.properties and injected via BuildConfig
        this.apiKey = getApiKey(context);
    }

    /**
     * Get API key from resources or BuildConfig
     */
    private String getApiKey(Context context) {
        try {
            // This will be replaced by actual API key from gradle.properties
            android.content.pm.ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            android.content.pm.PackageManager.GET_META_DATA);
            Object value = ai.metaData.get("com.google.android.geo.API_KEY");
            return value != null ? value.toString() : "";
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key", e);
            return "";
        }
    }

    /**
     * Search for nearby supermarkets
     */
    public List<Supermarket> searchNearbySupermarkets(double latitude, double longitude) {
        List<Supermarket> supermarkets = new ArrayList<>();

        try {
            String urlString = PLACES_API_BASE + "/nearbysearch/json?"
                    + "location=" + latitude + "," + longitude
                    + "&radius=" + SEARCH_RADIUS
                    + "&type=" + TYPE_GROCERY
                    + "&key=" + URLEncoder.encode(apiKey, "UTF-8");

            String response = makeHttpRequest(urlString);
            supermarkets = parseSupermarketResponse(response);

            Log.d(TAG, "Found " + supermarkets.size() + " supermarkets nearby");
        } catch (Exception e) {
            Log.e(TAG, "Error searching supermarkets", e);
        }

        return supermarkets;
    }

    /**
     * Make HTTP GET request
     */
    private String makeHttpRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                Log.e(TAG, "HTTP error code: " + responseCode);
                return "";
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Parse JSON response from Google Places API
     */
    private List<Supermarket> parseSupermarketResponse(String jsonResponse) {
        List<Supermarket> supermarkets = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            String status = jsonObject.getString("status");

            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                Log.e(TAG, "API error status: " + status);
                return supermarkets;
            }

            if (!jsonObject.has("results")) {
                return supermarkets;
            }

            JSONArray results = jsonObject.getJSONArray("results");

            for (int i = 0; i < results.length(); i++) {
                JSONObject place = results.getJSONObject(i);
                Supermarket supermarket = new Supermarket();

                supermarket.setPlaceId(place.getString("place_id"));
                supermarket.setName(place.getString("name"));

                if (place.has("vicinity")) {
                    supermarket.setVicinity(place.getString("vicinity"));
                }

                if (place.has("rating")) {
                    supermarket.setRating((float) place.getDouble("rating"));
                }

                if (place.has("opening_hours")) {
                    JSONObject openingHours = place.getJSONObject("opening_hours");
                    if (openingHours.has("open_now")) {
                        supermarket.setOpen(openingHours.getBoolean("open_now"));
                    }
                }

                // Get location coordinates
                if (place.has("geometry")) {
                    JSONObject geometry = place.getJSONObject("geometry");
                    if (geometry.has("location")) {
                        JSONObject location = geometry.getJSONObject("location");
                        supermarket.setLatitude(location.getDouble("lat"));
                        supermarket.setLongitude(location.getDouble("lng"));
                    }
                }

                supermarkets.add(supermarket);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON response", e);
        }

        return supermarkets;
    }

    /**
     * Get place details by place ID
     */
    public Supermarket getPlaceDetails(String placeId) {
        try {
            String urlString = PLACES_API_BASE + "/details/json?"
                    + "place_id=" + URLEncoder.encode(placeId, "UTF-8")
                    + "&fields=name,rating,formatted_address,geometry,opening_hours"
                    + "&key=" + URLEncoder.encode(apiKey, "UTF-8");

            String response = makeHttpRequest(urlString);
            JSONObject jsonObject = new JSONObject(response);

            if (jsonObject.has("result")) {
                JSONObject result = jsonObject.getJSONObject("result");
                Supermarket supermarket = new Supermarket();
                supermarket.setPlaceId(placeId);
                supermarket.setName(result.optString("name"));
                supermarket.setAddress(result.optString("formatted_address"));

                if (result.has("geometry")) {
                    JSONObject geometry = result.getJSONObject("geometry");
                    if (geometry.has("location")) {
                        JSONObject location = geometry.getJSONObject("location");
                        supermarket.setLatitude(location.getDouble("lat"));
                        supermarket.setLongitude(location.getDouble("lng"));
                    }
                }

                return supermarket;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting place details", e);
        }

        return null;
    }
}

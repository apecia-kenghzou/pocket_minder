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
    private static final int DEFAULT_SEARCH_RADIUS = 2000; // 2km radius default

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
     * Search for nearby supermarkets with default radius (2km)
     */
    public List<Supermarket> searchNearbySupermarkets(double latitude, double longitude) {
        return searchNearbySupermarkets(latitude, longitude, DEFAULT_SEARCH_RADIUS);
    }

    /**
     * Search for nearby supermarkets with custom radius
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param radiusMeters Search radius in meters (max 50000 per Google API limits)
     */
    public List<Supermarket> searchNearbySupermarkets(double latitude, double longitude, int radiusMeters) {
        List<Supermarket> supermarkets = new ArrayList<>();

        // Google Places API has a max radius of 50km
        int searchRadius = Math.min(radiusMeters, 50000);

        try {
            String urlString = PLACES_API_BASE + "/nearbysearch/json?"
                    + "location=" + latitude + "," + longitude
                    + "&radius=" + searchRadius
                    + "&type=" + TYPE_GROCERY
                    + "&key=" + URLEncoder.encode(apiKey, "UTF-8");

            // Log API call details (mask the API key)
            String maskedUrl = urlString.replaceAll("key=[^&]+", "key=***MASKED***");
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Log.d(TAG, "API Request:");
            Log.d(TAG, "  Location: " + latitude + ", " + longitude);
            Log.d(TAG, "  Radius: " + searchRadius + "m");
            Log.d(TAG, "  URL: " + maskedUrl);
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

            String response = makeHttpRequest(urlString);

            if (response.isEmpty()) {
                Log.e(TAG, "❌ Empty response from Google API");
                return supermarkets;
            }

            supermarkets = parseSupermarketResponse(response);

            Log.d(TAG, "✅ Found " + supermarkets.size() + " supermarkets within " + searchRadius + "m");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error searching supermarkets", e);
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
            Log.d(TAG, "HTTP Response Code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                String jsonResponse = response.toString();
                Log.d(TAG, "Raw API Response (first 500 chars): " + jsonResponse.substring(0, Math.min(500, jsonResponse.length())));

                return jsonResponse;
            } else {
                // Try to read error response
                BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()));
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorResponse.append(line);
                }
                errorReader.close();

                Log.e(TAG, "❌ HTTP error code: " + responseCode);
                Log.e(TAG, "❌ Error response: " + errorResponse.toString());
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

            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            Log.d(TAG, "API Response Status: " + status);

            // Check for error messages
            if (jsonObject.has("error_message")) {
                String errorMessage = jsonObject.getString("error_message");
                Log.e(TAG, "❌ API Error Message: " + errorMessage);
            }

            // Detailed status explanations
            switch (status) {
                case "OK":
                    Log.d(TAG, "✅ API returned results successfully");
                    break;
                case "ZERO_RESULTS":
                    Log.w(TAG, "⚠️ ZERO_RESULTS: No places found matching your criteria");
                    Log.w(TAG, "   This is normal if there are no stores in the area");
                    return supermarkets;
                case "REQUEST_DENIED":
                    Log.e(TAG, "❌ REQUEST_DENIED: API key is invalid or not authorized");
                    Log.e(TAG, "   Check: API key in gradle.properties");
                    Log.e(TAG, "   Check: Places API is enabled in Google Cloud Console");
                    Log.e(TAG, "   Check: Billing is enabled");
                    return supermarkets;
                case "INVALID_REQUEST":
                    Log.e(TAG, "❌ INVALID_REQUEST: Missing required parameters");
                    return supermarkets;
                case "OVER_QUERY_LIMIT":
                    Log.e(TAG, "❌ OVER_QUERY_LIMIT: You've exceeded your API quota");
                    return supermarkets;
                case "UNKNOWN_ERROR":
                    Log.e(TAG, "❌ UNKNOWN_ERROR: Server error, try again");
                    return supermarkets;
                default:
                    Log.e(TAG, "❌ Unknown status: " + status);
                    return supermarkets;
            }

            if (!jsonObject.has("results")) {
                Log.w(TAG, "⚠️ No 'results' field in response");
                return supermarkets;
            }

            JSONArray results = jsonObject.getJSONArray("results");
            Log.d(TAG, "📍 Processing " + results.length() + " results from API");

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

                // Log each store found
                Log.d(TAG, String.format("  [%d] %s (%.4f, %.4f)",
                        i + 1, supermarket.getName(),
                        supermarket.getLatitude(), supermarket.getLongitude()));
            }

            Log.d(TAG, "✅ Successfully parsed " + supermarkets.size() + " supermarkets");
            Log.d(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (JSONException e) {
            Log.e(TAG, "❌ Error parsing JSON response", e);
            Log.e(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
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

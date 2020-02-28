package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

class WeatherHandler {

    private static final String TAG = "snowpaws_wh";

    private static final double LOC_CERTAINTY = 0.1d;

    // Interface to send updates to host activity
    interface WeatherReceivedListener {
        void onWeatherReceived(LatLng latLng, String response, boolean isMetric);
    }

    private WeatherReceivedListener mHostListener;

    WeatherHandler(WeatherReceivedListener listener) {
        setHostListener(listener);
    }

    private void setHostListener(WeatherReceivedListener listener) {
        mHostListener = listener;
    }

    boolean updateWeather(Context context, LatLng latLng, boolean isMetric) {
        // Generate URL and request OWM data
        try {
            SharedPreferences sharedPref = context.getSharedPreferences(
                    context.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

            // Decide whether to update current weather data
            JSONObject lastWeather = new JSONObject(
                    sharedPref.getString("last_weather_json", "{}"));

            if (lastWeather.toString().equals("{}") || lastWeather.length() == 0) {
                Log.d(TAG,
                        "Fetching new weather data: Last weather data does not exist.");
            } else {
                // Embed dummy data in exceptional circumstances
                if (!lastWeather.has("lat_lng")) {
                    lastWeather.put("lat_lng", new JSONObject(
                            "{"
                            + "\"latitude\":\"0.00\","
                            + "\"longitude\":\"0.00\""
                            + "}"
                    ));
                }
                if (!lastWeather.has("is_metric")) {
                    lastWeather.put("is_metric", !isMetric);
                }

                Log.d(TAG, "Checking data relevancy.\n"
                        + "LatLng comparison:\n"
                        + "sys:  " + new DecimalFormat("#.###").format(latLng.latitude)
                        + " " + new DecimalFormat("#.###").format(latLng.longitude) + "\n"
                        + "json: " + new DecimalFormat("#.###").format(lastWeather.getJSONObject("lat_lng").getDouble("latitude"))
                        + " " + new DecimalFormat("#.###").format(lastWeather.getJSONObject("lat_lng").getDouble("longitude")));

                // Request new data if the location has changed
                if (isMetric != lastWeather.getBoolean("is_metric")) {
                    Log.d(TAG, "Units of measurement differ and an update will be retrieved.");
                } else if (Math.abs(latLng.latitude - lastWeather.getJSONObject("lat_lng").getDouble("latitude")) < LOC_CERTAINTY
                && Math.abs(latLng.longitude - lastWeather.getJSONObject("lat_lng").getDouble("longitude")) < LOC_CERTAINTY) {
                    Log.d(TAG, "Location differs significantly enough to warrant an update.");

                    // Don't request new data if the current data was received in the last 3 hours
                    long timestamp = lastWeather.getJSONArray("list").getJSONObject(0)
                            .getLong("dt") * 1000;

                    Log.d(TAG, "Checking data recency.\n"
                            + "Timestamp comparison:\n"
                            + "sys:  " + System.currentTimeMillis() + "\njson:  " + timestamp);

                    if (System.currentTimeMillis() - timestamp < 36000000) {
                        Log.d(TAG, "Will not get new weather data. LastWeather data too recent:\n"
                                + (System.currentTimeMillis() - timestamp));
                        return false;
                    }

                    // Use the coordinates from the last weather data if the location hasn't changed
                    Log.d(TAG, "Last weather data for this location exists, and is outdated.");
                    latLng = new LatLng(lastWeather.getJSONObject("city").getJSONObject("coord")
                            .getDouble("lat"),
                            lastWeather.getJSONObject("city").getJSONObject("coord")
                                    .getDouble("lon"));
                }
            }

            // Fetch the complete weather data
            getWeather(context, latLng, isMetric);

        } catch (JSONException ex) {
            ex.printStackTrace();
        }
        return true;
    }

    private void getWeather(Context context, LatLng latLng, boolean isMetric) {

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Generate URL and request queue
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = OpenWeatherMapIntegration.getOWMURL(context, latLng);
        Log.d(TAG, "URL:\n" + url);

        // Generate and post the request.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                (response) -> {
                    Log.d(TAG, "stringRequest.onResponse");

                    mHostListener.onWeatherReceived(latLng, response, isMetric);

                    // Embed the current latitude/longitude into the JSON
                    try {
                        JSONObject json = new JSONObject(response);
                        json.put("lat_lng", new JSONObject(
                                "{"
                                + "\"latitude\":\"" + latLng.latitude + "\","
                                + "\"longitude\":\"" + latLng.longitude + "\""
                                + "}"
                        ));
                        json.put("is_metric", isMetric);
                        response = json.toString();
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }

                    // Save the weather dictionary to local data
                    sharedEditor.putString("last_weather_json", response);
                    sharedEditor.apply();
                },
                (ex) -> {
                    Log.println(Log.ERROR, TAG, "stringRequest.onErrorResponse");

                    // olive oil didn't work
                    ex.printStackTrace();
                });

        queue.add(stringRequest);
    }
}

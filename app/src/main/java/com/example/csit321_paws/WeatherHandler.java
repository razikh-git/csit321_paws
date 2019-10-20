package com.example.csit321_paws;

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
    public interface WeatherForecastReceivedListener {
        void onWeatherForecastReceived(LatLng latLng, String response);
    }

    private WeatherForecastReceivedListener mListener;

    WeatherHandler(WeatherForecastReceivedListener listener) {
        setWeatherForecastReceivedListener(listener);
    }

    private void setWeatherForecastReceivedListener(WeatherForecastReceivedListener listener) {
        mListener = listener;
    }

    boolean updateLatestWeatherForecast(Context ctx, LatLng latLng) {
        // Generate URL and request OWM data.
        try {
            SharedPreferences sharedPref = ctx.getSharedPreferences(
                    ctx.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

            Log.d(TAG, "updateLatestWeatherForecast()");

            // Decide whether to update current weather data.
            JSONObject lastWeather = new JSONObject(
                    sharedPref.getString("last_weather_json", "{}"));

            if (lastWeather == null || lastWeather.toString().equals("{}") || lastWeather.length() == 0) {
                Log.d(TAG,
                        "Fetching new weather data: Last weather data does not exist.");
            } else {
                // Embed dummy data in exceptional circumstances.
                if (!lastWeather.has("lat_lng")) {
                    lastWeather.put("lat_lng", new JSONObject(
                            "{"
                            + "\"latitude\":\"0.00\","
                            + "\"longitude\":\"0.00\""
                            + "}"
                    ));
                }

                Log.d(TAG, "Checking data relevancy.\n"
                        + "LatLng comparison:\n"
                        + "sys:  " + new DecimalFormat("#.###").format(latLng.latitude)
                        + " " + new DecimalFormat("#.###").format(latLng.longitude) + "\n"
                        + "json: " + new DecimalFormat("#.###").format(lastWeather.getJSONObject("lat_lng").getDouble("latitude"))
                        + " " + new DecimalFormat("#.###").format(lastWeather.getJSONObject("lat_lng").getDouble("longitude")));

                // Request new data if the location has changed.
                if (Math.abs(latLng.latitude - lastWeather.getJSONObject("lat_lng").getDouble("latitude")) < LOC_CERTAINTY
                && Math.abs(latLng.latitude - lastWeather.getJSONObject("lat_lng").getDouble("longitude")) < LOC_CERTAINTY) {
                    Log.d(TAG, "Location differs significantly enough to warrant an update.");

                    // Don't request new data if the current data was received in the last 3 hours.
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

                    // Use the coordinates from the last weather data if the location hasn't changed.
                    Log.d(TAG, "Last weather data for this location exists, and is outdated.");
                    latLng = new LatLng(lastWeather.getJSONObject("city").getJSONObject("coord")
                            .getDouble("lat"),
                            lastWeather.getJSONObject("city").getJSONObject("coord")
                                    .getDouble("lon"));
                }
            }

            // Fetch the complete weather data.
            getWeatherForecast(ctx, latLng);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void getWeatherForecast(Context ctx, LatLng latLng) {

        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        Log.d(TAG, "getWeatherForecast(ctx, [ " + latLng.latitude + " " + latLng.longitude + " ] )");

        // Generate URL and request queue.
        RequestQueue queue = Volley.newRequestQueue(ctx);
        String url = ctx.getResources().getString(R.string.app_url_owm_weather_root)
                + "data/2.5/"
                + "forecast"
                + "?lat=" + latLng.latitude + "&lon=" + latLng.longitude
                + "&units=" + sharedPref.getString("units", "metric")
                + "&lang=" + ctx.getResources().getConfiguration().locale.getDisplayLanguage()
                + "&mode=" + "json"
                + "&appid=" + ctx.getResources().getString(R.string.open_weather_maps_key);

        // Generate and post the request.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                (response) -> {
                    Log.d(TAG, "stringRequest.onResponse");

                    mListener.onWeatherForecastReceived(latLng, response);

                    // Embed the current latitude/longitude into the JSON.
                    try {
                        JSONObject json = new JSONObject(response);
                        json.put("lat_lng", new JSONObject(
                                "{"
                                + "\"latitude\":\"" + latLng.latitude + "\","
                                + "\"longitude\":\"" + latLng.longitude + "\""
                                + "}"
                        ));
                        response = json.toString();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    // Save the weather dictionary to local data.
                    sharedEditor.putString("last_weather_json", response);
                    sharedEditor.apply();
                },
                (error) -> {
                    Log.println(Log.ERROR, TAG, "stringRequest.onErrorResponse");

                    // olive oil didn't work
                    error.printStackTrace();
                });

        queue.add(stringRequest);
    }

}

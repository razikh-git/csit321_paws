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

public class WeatherHandler {

    // Interface to send updates to host activity
    public interface WeatherForecastReceivedListener {
        void onWeatherForecastReceived(LatLng latLng, String response);
    }

    public static WeatherHandler.WeatherForecastReceivedListener mListener;

    static void updateLatestWeatherForecast(Context ctx, LatLng latLng) {
        // Generate URL and request OWM data.
        try {
            SharedPreferences sharedPref = ctx.getSharedPreferences(
                    ctx.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

            // Decide whether to update current weather data.
            JSONObject lastWeather = new JSONObject(
                    sharedPref.getString("last_weather_json", "{}"));

            if (lastWeather == null || lastWeather.toString().equals("{}") || lastWeather.length() == 0) {
                Log.println(Log.DEBUG, "snowpaws_pawsapi",
                        "Last weather data does not exist.");
            } else {
                Log.println(Log.DEBUG, "snowpaws_pawsapi",
                        "Checking data recency.");
                // Don't request new data if the current data was received in the last 3 hours.
                long timestamp = lastWeather.getJSONArray("list").getJSONObject(0)
                        .getLong("dt") * 1000;

                // TODO resolve this, data is always considered outdated
                if (System.currentTimeMillis() - timestamp < 36000000) {
                    Log.println(Log.DEBUG, "snowpaws_pawsapi",
                            "Will not get new weather data. LastWeather data too recent:\n"
                                    + (System.currentTimeMillis() - timestamp));
                    return;
                }
                Log.println(Log.DEBUG, "snowpaws_pawsapi",
                        "Last weather data exists and is outdated.");
                // Use the coordinates from the last weather data.
                latLng = new LatLng(lastWeather.getJSONObject("city").getJSONObject("coord")
                        .getDouble("lat"),
                        lastWeather.getJSONObject("city").getJSONObject("coord")
                                .getDouble("lon"));
            }

            // Fetch the complete weather data.
            getWeatherForecast(ctx, latLng);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static void getWeatherForecast(Context ctx, LatLng latLng) {

        SharedPreferences sharedPref = ctx.getSharedPreferences(
                ctx.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

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
                    Log.println(Log.DEBUG, "snowpaws_pawsapi", "stringRequest.onResponse");

                    mListener.onWeatherForecastReceived(latLng, response);

                    // Save the weather dictionary to local data.
                    sharedEditor.putString("last_weather_json", response);
                    sharedEditor.apply();
                },
                (error) -> {
                    Log.println(Log.ERROR, "snowpaws_pawsapi", "stringRequest.onErrorResponse");

                    // olive oil didn't work
                    error.printStackTrace();
                });

        queue.add(stringRequest);
    }

}
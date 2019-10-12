package com.example.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

final class PAWSAPI {
    private PAWSAPI() {}

    static final Double MS_TO_KMH = 3.6d;
    static final Double MS_TO_MPH = 2.237d;

    static void updateLatestWeatherForecast(Context ctx, double lat, double lng) {
        // Generate URL and request OWM data.
        try {
            SharedPreferences sharedPref = ctx.getSharedPreferences(
                    ctx.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
            SharedPreferences.Editor sharedEditor = sharedPref.edit();

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
                lat = lastWeather.getJSONObject("city").getJSONObject("coord")
                        .getDouble("lat");
                lng = lastWeather.getJSONObject("city").getJSONObject("coord")
                        .getDouble("lon");
            }

            // Generate URL and request queue.
            RequestQueue queue = Volley.newRequestQueue(ctx);
            String url = ctx.getResources().getString(R.string.app_url_owm_weather_root)
                    + "data/2.5/"
                    + "forecast"
                    + "?lat=" + lat + "&lon=" + lng
                    + "&units=" + sharedPref.getString("units", "metric")
                    + "&lang=" + ctx.getResources().getConfiguration().locale.getDisplayLanguage()
                    + "&mode=" + "json"
                    + "&appid=" + ctx.getResources().getString(R.string.open_weather_maps_key);

            // Generate and post the request.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    (response) -> {
                        Log.println(Log.DEBUG, "snowpaws_pawsapi", "stringRequest.onResponse");

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
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static Drawable getWeatherDrawable(Context ctx, String icon) {
        switch (icon) {
            case "01":
                return ctx.getDrawable(R.drawable.w01);
            case "01d":
                return ctx.getDrawable(R.drawable.w01d);
            case "01n":
                return ctx.getDrawable(R.drawable.w01n);
            case "02d":
                return ctx.getDrawable(R.drawable.w02d);
            case "02n":
                return ctx.getDrawable(R.drawable.w02n);
            case "03d":
                return ctx.getDrawable(R.drawable.w03d);
            case "03n":
                return ctx.getDrawable(R.drawable.w03n);
            case "04d":
                return ctx.getDrawable(R.drawable.w04d);
            case "04n":
                return ctx.getDrawable(R.drawable.w04n);
            case "9d":
                return ctx.getDrawable(R.drawable.w09d);
            case "09n":
                return ctx.getDrawable(R.drawable.w09n);
            case "10d":
                return ctx.getDrawable(R.drawable.w10d);
            case "10n":
                return ctx.getDrawable(R.drawable.w10n);
            case "11d":
                return ctx.getDrawable(R.drawable.w11d);
            case "11n":
                return ctx.getDrawable(R.drawable.w11n);
            case "13d":
                return ctx.getDrawable(R.drawable.w13d);
            case "13n":
                return ctx.getDrawable(R.drawable.w13n);
            case "50d":
                return ctx.getDrawable(R.drawable.w50d);
            case "50n":
                return ctx.getDrawable(R.drawable.w50n);
            default:
                return null;
        }
    }
}

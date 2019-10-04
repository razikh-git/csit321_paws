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

public final class PAWSAPI {
    private PAWSAPI() {}

    public static final void updateLatestWeatherForecast(Context ctx, double lat, double lng) {

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
                long timestamp = lastWeather.getJSONArray("list").getJSONObject(0).getLong("dt");

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
                lat = lastWeather.getJSONObject("city").getJSONObject("coord").getDouble("lat");
                lng = lastWeather.getJSONObject("city").getJSONObject("coord").getDouble("lon");
            }

            // Generate URL and request queue.
            RequestQueue queue = Volley.newRequestQueue(ctx);
            String url = ctx.getResources().getString(R.string.app_url_owm_api_root)
                    + "data/2.5/"
                    + "forecast"
                    + "?lat=" + lat + "&lon=" + lng + "&units=" + sharedPref.getString("units", "metric")
                    + "&lang=" + ctx.getResources().getConfiguration().locale.getDisplayLanguage()
                    + "&mode=" + "json"
                    + "&appid=" + ctx.getResources().getString(R.string.owm_default_api_key);

            // Generate and post the request.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    (response) -> {
                        Log.println(Log.DEBUG, "snowpaws_pawsapi", "stringRequest.onResponse");

                        // Save the weather dictionary to local data.
                        sharedEditor.putString("last_weather_json", response);
                        sharedEditor.apply();

                    },
                    (error) -> {
                        Log.println(Log.DEBUG, "snowpaws_pawsapi", "stringRequest.onErrorResponse");

                        // olive oil didn't work
                        error.printStackTrace();
            });

            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static Drawable getWeatherDrawable(Context ctx, String icon) {
        Log.println(Log.DEBUG, "snowpaws_pawsapi", "getWeatherDrawable");
        Drawable drawable = null;
        switch (icon) {
            case "01": {
                drawable = ctx.getDrawable(R.drawable.w01);
                break;
            }
            case "01d": {
                drawable = ctx.getDrawable(R.drawable.w01d);
                break;
            }
            case "01n": {
                drawable = ctx.getDrawable(R.drawable.w01n);
                break;
            }
            case "02d": {
                drawable = ctx.getDrawable(R.drawable.w02d);
                break;
            }
            case "02n": {
                drawable = ctx.getDrawable(R.drawable.w02n);
                break;
            }
            case "03d": {
                drawable = ctx.getDrawable(R.drawable.w03d);
                break;
            }
            case "03n": {
                drawable = ctx.getDrawable(R.drawable.w03n);
                break;
            }
            case "04d": {
                drawable = ctx.getDrawable(R.drawable.w04d);
                break;
            }
            case "04n": {
                drawable = ctx.getDrawable(R.drawable.w04n);
                break;
            }
            case "9d": {
                drawable = ctx.getDrawable(R.drawable.w09d);
                break;
            }
            case "09n": {
                drawable = ctx.getDrawable(R.drawable.w09n);
                break;
            }
            case "10d": {
                drawable = ctx.getDrawable(R.drawable.w10d);
                break;
            }
            case "10n": {
                drawable = ctx.getDrawable(R.drawable.w10n);
                break;
            }
            case "11d": {
                drawable = ctx.getDrawable(R.drawable.w11d);
                break;
            }
            case "11n": {
                drawable = ctx.getDrawable(R.drawable.w11n);
                break;
            }
            case "13d": {
                drawable = ctx.getDrawable(R.drawable.w13d);
                break;
            }
            case "13n": {
                drawable = ctx.getDrawable(R.drawable.w13n);
                break;
            }
            case "50d": {
                drawable = ctx.getDrawable(R.drawable.w50d);
                break;
            }
            case "50n": {
                drawable = ctx.getDrawable(R.drawable.w50n);
                break;
            }
        }
        return drawable;
    }
}

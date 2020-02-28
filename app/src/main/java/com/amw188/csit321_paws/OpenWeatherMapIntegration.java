package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

public class OpenWeatherMapIntegration {
    public static final String API_KEY = "442dabf9a4b790d1198c03fbd0d31550";
    public static final String API_URL = "http://api.openweathermap.org/data/2.5";

    public static String getOWMURL(Context context, LatLng latLng) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        final boolean isMetric = sharedPref.getString(
                "units", "metric").equals("metric");
        return context.getResources().getString(R.string.app_url_owm_weather_root)
                + "data/2.5/"
                + "forecast"
                + "?lat=" + latLng.latitude + "&lon=" + latLng.longitude
                + "&units=" + (isMetric ? "metric" : "imperial")
                + "&lang=" + context.getResources().getConfiguration().locale.getDisplayLanguage()
                + "&mode=" + "json"
                + "&appid=" + context.getResources().getString(R.string.open_weather_maps_key);
    }
}

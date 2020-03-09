package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

class OpenWeatherMapIntegration {
    static final String app_url_owm_root = "https://openweathermap.org/";
    static final String app_url_owm_weather_root = "https://api.openweathermap.org/";
    static final String app_url_owm_map_root = "https://tile.openweathermap.org/map/";
    static final String app_owm_api_key = "442dabf9a4b790d1198c03fbd0d31550";

    static String getOWMURL(final Context context, final LatLng latLng, boolean isWeekly) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                PrefKeys.app_global_preferences, Context.MODE_PRIVATE);
        final String units = sharedPref.getString(PrefKeys.units, PrefDefValues.units);
        return app_url_owm_root
                + "data/2.5/"
                + (isWeekly ? "forecast" : "weather")
                + "?lat=" + latLng.latitude + "&lon=" + latLng.longitude
                + "&units=" + units
                + "&lang=" + context.getResources().getConfiguration().locale.getDisplayLanguage()
                + "&mode=json"
                + "&appid=" + context.getResources().getString(R.string.open_weather_maps_key);
    }
}

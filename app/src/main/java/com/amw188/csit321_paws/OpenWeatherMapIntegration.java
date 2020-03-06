package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

class OpenWeatherMapIntegration {
    static String getOWMURL(Context context, LatLng latLng, boolean isWeekly) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                PrefKeys.app_global_preferences, Context.MODE_PRIVATE);
        final boolean isMetric = sharedPref.getString(
                PrefKeys.units, PrefDefValues.units).equals(PrefConstValues.units_metric);
        return context.getResources().getString(R.string.app_url_owm_weather_root)
                + "data/2.5/"
                + (isWeekly ? "forecast" : "weather")
                + "?lat=" + latLng.latitude + "&lon=" + latLng.longitude
                + "&units=" + (isMetric ? PrefConstValues.units_metric : PrefConstValues.units_imperial)
                + "&lang=" + context.getResources().getConfiguration().locale.getDisplayLanguage()
                + "&mode=json"
                + "&appid=" + context.getResources().getString(R.string.open_weather_maps_key);
    }
}

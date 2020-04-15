package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

class OpenWeatherIntegration {
    private static final String app_url_owm_weather_root = "https://api.openweathermap.org/";
    private static final String app_url_owm_tile_root = "https://tile.openweathermap.org/map/";

    private static final String app_owm_api_key = "442dabf9a4b790d1198c03fbd0d31550";

    static String getOpenWeatherURL(final Context context, final LatLng latLng, boolean isWeekly) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String units = sharedPref.getString(PrefKeys.units, PrefDefValues.units);
        return app_url_owm_weather_root
                + "data/2.5/"
                + (isWeekly ? "forecast" : "weather")
                + "?lat=" + latLng.latitude + "&lon=" + latLng.longitude
                + "&units=" + units
                + "&lang=" + context.getResources().getConfiguration().locale.getDisplayLanguage()
                + "&mode=json"
                + "&appid=" + app_owm_api_key;
    }

    static String getOpenWeatherTileURL(final String layer, final int x, final int y, final int zoom) {
        return String.format(Locale.US, "%s%s/%s/%d/%d.png?appid=%s",
                app_url_owm_tile_root,
                layer, zoom, x, y,
                app_owm_api_key);
    }
}

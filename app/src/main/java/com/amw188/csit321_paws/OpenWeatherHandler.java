package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

class OpenWeatherHandler extends WeatherHandler {

    private static final String TAG = PrefConstValues.tag_prefix + "h_owm";

    private static final double LOC_CERTAINTY = 0.1d;
	private WeatherReceivedListener mHostListener;
	private Context mContext;

    OpenWeatherHandler(final WeatherReceivedListener listener, final Context context) {
        mContext = context;
        mHostListener = listener;
    }

    /**
     * Determines whether to await an updated weather forecast.
     * @param latLng Target coordinates for the weather forecast.
     * @return Returns true if the host should await an update, or false to act on current data.
     */
    boolean awaitWeatherUpdate(final LatLng latLng) {
        if (shouldFetchWeatherUpdate(latLng))
            return fetchWeatherUpdate(latLng);
        return false;
    }

    /**
     * Determines whether a weather update is required around the location of some coordinates.
     * Updates are considered unnecessary if we already have a forecast for the given area
     * in some relevant timeframe that allows us to keep a complete forecast.
     * @param latLng Given coordinates for the weather forecast.
     * @return Returns whether to await an updated batch of weather data.
     * Coordinates are corrected to the last values if near enough.
     */
    private boolean shouldFetchWeatherUpdate(final LatLng latLng) {
        try {
            SharedPreferences sharedPref = PreferenceManager
                    .getDefaultSharedPreferences(mContext);
            final boolean isMetric = PAWSAPI.preferredMetric(sharedPref);

            // Decide whether to update current weather data
            JSONObject lastWeather = new JSONObject(sharedPref.getString(
                    PrefKeys.last_weather_json, PrefConstValues.empty_json_object));

            if (lastWeather.toString().equals(PrefConstValues.empty_json_object)
					|| lastWeather.length() == 0) {
                Log.d(TAG,
                        "Last weather data does not exist.");
            } else {
                // Embed dummy data in exceptional circumstances
                if (!lastWeather.has("lat_lng")) {
                    lastWeather.put("lat_lng", new JSONObject(
                            PAWSAPI.getLatLngJsonObjectString(0.00d, 0.00d)));
                }
                if (!lastWeather.has("is_metric")) {
					// Embed false isMetric to encourage an update, as last data is assumed invalid
					lastWeather.put("is_metric", !isMetric);
				}

                if (isMetric != lastWeather.getBoolean("is_metric")) {
                	// Request new data if units have changed
                    Log.d(TAG, "Units of measurement are different.");
					return true;
                } else if (Math.abs(latLng.latitude - lastWeather.getJSONObject("lat_lng")
                        .getDouble("latitude")) < LOC_CERTAINTY
                        && Math.abs(latLng.longitude - lastWeather.getJSONObject("lat_lng")
                        .getDouble("longitude")) < LOC_CERTAINTY) {
					// Request new data if the location has changed
                    Log.d(TAG, "Weather data locations are different.");
                    return true;
                } else if (PAWSAPI.getWeatherJsonIndexForTime(lastWeather.getJSONArray("list"),
                        System.currentTimeMillis()) > 0) {
                    // Don't request new data if the current data was received in the last 3 hours
                    Log.d(TAG, "Last weather data for this location is up-to-date.");
                    return true;
                }
            }
        } catch (JSONException ex) {
        	Log.e(TAG, "Failed to read current weather data.");
            ex.printStackTrace();
        }
        return false;
    }

    /**
     * Fetches a weather forecast for the given LatLng coordinates from OpenWeatherMaps.
     * @param latLng Target coordinates for the weather forecast.
	 * @return Returns success or failure to begin fetch request.
     */
    private boolean fetchWeatherUpdate(final LatLng latLng) {
        SharedPreferences sharedPref = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();
        final boolean isMetric = PAWSAPI.preferredMetric(sharedPref);

        // Generate URL and request queue
        final int requestCode = WeatherHandler.REQUEST_OPEN_WEATHER;
        String url = OpenWeatherIntegration.getOpenWeatherURL(mContext, latLng, true);
        Log.d(TAG, "URL:\n" + url);

        // Generate and post the request.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                (response) -> {
                    Log.d(TAG, "stringRequest.onResponse");

                    mHostListener.onWeatherReceived(requestCode, response);

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
                    sharedEditor.putString(PrefKeys.last_weather_json, response);
                    sharedEditor.apply();
                },
                (ex) -> {
                    Log.println(Log.ERROR, TAG, "stringRequest.onErrorResponse");

                    // olive oil didn't work
                    ex.printStackTrace();
                });

        getFromURL(mContext, stringRequest, true);
        return false;
    }
}

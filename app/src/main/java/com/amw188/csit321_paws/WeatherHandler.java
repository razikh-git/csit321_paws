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

class WeatherHandler {

    private static final String TAG = PrefConstValues.tag_prefix + "h_w";

    private static final double LOC_CERTAINTY = 0.1d;
	private WeatherReceivedListener mHostListener;

    // Interface to send updates to host activity
    interface WeatherReceivedListener {
        void onWeatherReceived(LatLng latLng, String response, boolean isMetric);
    }

    WeatherHandler(WeatherReceivedListener listener) {
		mHostListener = listener;
    }

    /**
     * Determines whether to await an updated weather forecast.
     * @param latLng Target coordinates for the weather forecast.
     * @param context Context.
     * @param isMetric Units in metric or imperial.
     * @return Returns true if the host should await an update, or false to act on current data.
     */
    boolean awaitWeatherUpdate(final Context context, final LatLng latLng,
							   final boolean isMetric) {
        if (shouldFetchWeatherUpdate(context, latLng, isMetric))
            return fetchWeatherUpdate(context, latLng, isMetric);
        return false;
    }

    /**
     * Determines whether a weather update is required around the location of some coordinates.
     * Updates are considered unnecessary if we already have a forecast for the given area
     * in some relevant timeframe that allows us to keep a complete forecast.
     * @param context Context.
     * @param latLng Given coordinates for the weather forecast.
     * @param isMetric Units in metric or imperial.
     * @return Returns coordinates for weather update, or null if no update is required.
     * Coordinates are corrected to the last values if near enough.
     */
    private boolean shouldFetchWeatherUpdate(final Context context,
                                             final LatLng latLng, final boolean isMetric) {
        try {
            SharedPreferences sharedPref = context.getSharedPreferences(
                    PrefKeys.app_global_preferences, Context.MODE_PRIVATE);

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
                	// Request new data if units have cahnged
                    Log.d(TAG, "Units of measurement are different.");
					return true;
                } else if (Math.abs(latLng.latitude - lastWeather.getJSONObject("lat_lng")
                        .getDouble("latitude")) < LOC_CERTAINTY
                        && Math.abs(latLng.longitude - lastWeather.getJSONObject("lat_lng")
                        .getDouble("longitude")) < LOC_CERTAINTY) {
					// Request new data if the location has changed
                    Log.d(TAG, "Weather data locations are different.");

                    // Don't request new data if the current data was received in the last 3 hours
                    if (PAWSAPI.getWeatherJsonIndexForTime(lastWeather.getJSONArray("list"),
                            System.currentTimeMillis()) == 0) {
						Log.d(TAG, "Last weather data for this location is up-to-date.");
						return false;
					}
                }
            }
        } catch (JSONException ex) {
        	Log.e(TAG, "Failed to read current weather data.");
            ex.printStackTrace();
        }
        return true;
    }

    /**
     * Fetches a weather forecast for the given LatLng coordinates from OpenWeatherMaps.
     * @param latLng Target coordinates for the weather forecast.
     * @param context Context.
     * @param isMetric Units in metric or imperial.
	 * @return Returns success or failure to begin fetch request.
     */
    private boolean fetchWeatherUpdate(final Context context,
                                       final LatLng latLng, final boolean isMetric) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                PrefKeys.app_global_preferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Generate URL and request queue
        RequestQueue queue = Volley.newRequestQueue(context);
        String url = OpenWeatherMapIntegration.getOWMWeatherURL(context, latLng, true);
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
                    sharedEditor.putString(PrefKeys.last_weather_json, response);
                    sharedEditor.apply();
                },
                (ex) -> {
                    Log.println(Log.ERROR, TAG, "stringRequest.onErrorResponse");

                    // olive oil didn't work
                    ex.printStackTrace();
                });

        queue.add(stringRequest);
        return false;
    }
}

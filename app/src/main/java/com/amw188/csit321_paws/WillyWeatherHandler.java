package com.amw188.csit321_paws;

import android.content.Context;
import android.location.Address;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;

class WillyWeatherHandler extends WeatherHandler {

	private static final String TAG = PrefConstValues.tag_prefix + "h_willy";

	private WeatherReceivedListener mHostListener;
	private Context mContext;

	WillyWeatherHandler(final WeatherReceivedListener listener, final Context context) {
		mContext = context;
		mHostListener = listener;
	}

	void awaitWeatherUpdate(final int requestCode, final Address address, final String args) {
		fetchLocationId(requestCode, address, args);
	}

	/**
	 * Gets the unique location ID for this address for use in WillyWeather GET requests.
	 * @param requestCode Request code for type of data to retrieve.
	 * @param address Address, hopefully with postcode and/or locality name.
	 * @param args Extra arguments for the forecast, eg. number of days, forecast elements.
	 * @return Whether the host should act on existing weather data, or await an update.
	 */
	private void fetchLocationId(final int requestCode, final Address address, final String args) {
		String identifier = address.getLocality();
		if (identifier == null || identifier.isEmpty())
			identifier = address.getPostalCode();
		if (identifier == null || identifier.isEmpty()) {
			Log.e(TAG, "Failed to gather any identification from address.");
			return;
		}

		final String url = WillyWeatherIntegration.getWillyWeatherURL(
				WeatherHandler.REQUEST_WILLY_SEARCH, identifier, args);

		// Generate and post the request
		StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
				(response) -> {
					Log.d(TAG, "stringRequest.onResponse");
					Log.d(TAG, "requestCode: " + requestCode);
					Log.d(TAG, "response:    " + response);

					try {
						final String id = new JSONArray(response).getJSONObject(0)
								.getString("id");
						fetchWeatherUpdate(requestCode, id, args);
					} catch (JSONException ex) {
						ex.printStackTrace();
					}
				},
				(ex) -> {
					Log.e(TAG, "stringRequest.onErrorResponse");

					// olive oil didn't work
					ex.printStackTrace();
				});

		getFromURL(mContext, stringRequest, true);
	}

	/**
	 * Fetches a weather forecast for the given LatLng coordinates from WillyWeather.
	 * @return Returns success or failure to begin fetch request.
	 */
	private void fetchWeatherUpdate(final int requestCode, final String id, final String args) {
		// Generate URL and request queue
		String url = WillyWeatherIntegration.getWillyWeatherURL(
				requestCode, id, args);

		// Generate and post the request
		StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
				(response) -> {
					Log.d(TAG, "stringRequest.onResponse");
					Log.d(TAG, requestCode + response);

					mHostListener.onWeatherReceived(requestCode, response);
				},
				(ex) -> {
					Log.println(Log.ERROR, TAG, "stringRequest.onErrorResponse");

					// olive oil didn't work
					ex.printStackTrace();
				});

		getFromURL(mContext, stringRequest, true);
	}
}

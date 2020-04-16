package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class PeriodicDataUpdateWorker
		extends
			Worker
		implements
			WeatherHandler.WeatherReceivedListener,
			LocationHandler.LocationReceivedListener,
			AddressHandler.AddressReceivedListener {
	private static final String TAG = PrefConstValues.tag_prefix + "worker_updater";

	static final String WORK_TAG = PrefConstValues.package_name + ".periodic_update_work";

	public PeriodicDataUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
		super(context, workerParams);
	}

	@NonNull
	@Override
	public Result doWork() {
		Result result = Result.success();

		// todo: update location one-time per period

		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext());
		try {
			LatLng latLng = null;
			JSONObject lastLatLngJSON;
			JSONObject lastWeatherJSON = new JSONObject(sharedPref.getString(
					PrefKeys.last_weather_json, PrefConstValues.empty_json_object));
			if (lastWeatherJSON.has("lat_lng")) {
				lastLatLngJSON = lastWeatherJSON.getJSONObject("lat_lng");
				latLng = new LatLng(
						lastLatLngJSON.getDouble("latitude"),
						lastLatLngJSON.getDouble("longitude"));
			} else {
				lastLatLngJSON = new JSONObject(sharedPref.getString(
						PrefKeys.last_best_lat_lng, PrefConstValues.empty_json_object));
				if (lastLatLngJSON.has("latitude") && lastLatLngJSON.has("longitude")) {
					latLng = new LatLng(
							lastLatLngJSON.getDouble("latitude"),
							lastLatLngJSON.getDouble("longitude"));
				}
			}
			if (latLng != null) {
				new OpenWeatherHandler(this, getApplicationContext()).awaitWeatherUpdate(latLng);
			} else {
				Log.e(TAG, "Went to update weather but no valid LatLng pair was found.");
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
			result = Result.failure();
		}

		return result;
	}

	@Override
	public void onWeatherReceived(int requestCode, String response) {
		// my work here is done
		Toast.makeText(getApplicationContext(),
				"SurfWatch received periodic weather update.",
				Toast.LENGTH_LONG).show();
	}

	@Override
	public void onLastLocationReceived(Location location) {

	}

	@Override
	public void onLocationReceived(LocationResult locationResult) {

	}

	@Override
	public void onAddressReceived(ArrayList<Address> addressResults) {

	}
}

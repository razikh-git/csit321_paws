package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

abstract class LocationActivity extends PermissionActivity {

	private static final String TAG = PrefConstValues.tag_prefix + "la";

	protected Location mSelectedLocation;
	protected FusedLocationProviderClient mFusedLocationClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
	}

	protected void awaitLocation() {
		mFusedLocationClient.getLastLocation().addOnSuccessListener(this::onLocationSuccess);
	}

	private void onLocationSuccess(Location location) {
		mSelectedLocation = location;
		if (mSelectedLocation == null) {
			try {
				// TODO remove debug functionality
				SharedPreferences sharedPref = this.getSharedPreferences(
						PrefKeys.app_global_preferences,
						Context.MODE_PRIVATE);
				JSONObject lastWeather = new JSONObject(sharedPref.getString(
						PrefKeys.last_weather_json, PrefConstValues.empty_json_object));
				mSelectedLocation = new Location(LocationManager.GPS_PROVIDER);
				mSelectedLocation.setLatitude(
						Float.parseFloat(
								lastWeather.getJSONObject("lat_lng")
										.getString("latitude")));
				mSelectedLocation.setLongitude(
						Float.parseFloat(
								lastWeather.getJSONObject("lat_lng")
										.getString("longitude")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
						/*
						Toast.makeText(this,
								R.string.sv_fa_service_unavailable,
								Toast.LENGTH_LONG).show();
						 */
			return;
		}
		// todo: push this as a dialog fragment
		// Push an error message on geocoder failure
		if (!Geocoder.isPresent()) {
			Toast.makeText(this,
					R.string.sv_fa_geocoder_unavailable,
					Toast.LENGTH_LONG).show();
			return;
		}
		onLocationReceived();
	}

	protected void onLocationReceived() {}

	@Override
	protected void onPermissionGranted(String perm) {}
	@Override
	protected void onPermissionBlocked(String perm) {}
	@Override
	protected void onAllPermissionsGranted(String[] permissions) {}
}

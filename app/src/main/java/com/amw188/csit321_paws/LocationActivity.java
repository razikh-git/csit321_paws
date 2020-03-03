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

class LocationActivity extends PermissionActivity {

	private String TAG = "snowpaws_la";

	protected ArrayList<Address> mAddressList;
	protected MapsActivity.AddressResultReceiver mAddressReceiver;
	protected Location mLocation;
	protected FusedLocationProviderClient mFusedLocationClient;

	class AddressResultReceiver extends ResultReceiver {
		AddressResultReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			if (resultData == null)
				return;
			String error = resultData.getString(FetchAddressCode.RESULT_DATA_KEY);
			if (error == null || error.equals(""))
				mAddressList = resultData.getParcelableArrayList(
						FetchAddressCode.RESULT_ADDRESSLIST_KEY);
			else
				return;
			// Update the interface with the new location
			onAddressReceived();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		mAddressReceiver = new AddressResultReceiver(new Handler());
	}

	protected String[] getAddress(ArrayList<Address> addressList) {
		String[] addressLine = null;
		try {
			Log.d(TAG, "addressList: " + addressList.toString());
			addressLine = addressList.get(0).getAddressLine(0)
					.split(", ", 3);
		} catch (NullPointerException ex) {
			Log.e(TAG, "Failed to read from null address object.");
			ex.printStackTrace();
		}
		return addressLine;
	}

	protected String getAbbreviatedAddress(ArrayList<Address> addressList) {
		String[] addressLine = getAddress(addressList);
		return addressLine.length > 0
				? addressLine[Math.max(0, addressLine.length - 2)]
				: null;
	}

	protected void fetchAddress() {
		FetchAddressIntentService.startActionFetchAddress(this,
				mAddressReceiver, mLocation);
	}

	protected void fetchLocation() {
		// Prepare the initial location information
		mFusedLocationClient.getLastLocation().addOnSuccessListener(
				(location -> {
					mLocation = location;
					if (mLocation == null) {
						try {
							// TODO remove debug functionality
							SharedPreferences sharedPref = this.getSharedPreferences(
									getResources().getString(R.string.app_global_preferences),
									Context.MODE_PRIVATE);
							JSONObject lastWeather = new JSONObject(
									sharedPref.getString(
											"last_weather_json", "{}"));
							mLocation = new Location(LocationManager.GPS_PROVIDER);
							mLocation.setLatitude(
									Float.parseFloat(
											lastWeather.getJSONObject("lat_lng")
													.getString("latitude")));
							mLocation.setLongitude(
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
				})
		);
	}

	protected void onLocationReceived() {}

	protected void onAddressReceived() {}

	@Override
	protected void onPermissionGranted(String perm) {}
	@Override
	protected void onPermissionBlocked(String perm) {}
	@Override
	protected void onAllPermissionsGranted(String[] permissions) {}
}

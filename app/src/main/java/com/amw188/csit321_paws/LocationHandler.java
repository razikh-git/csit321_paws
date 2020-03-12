package com.amw188.csit321_paws;

import android.content.Context;
import android.location.Location;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

class LocationHandler {
	private FusedLocationProviderClient mLocationClient;
	private LocationCallback mLocationCallback;
	private LocationReceivedListener mHostListener;
	interface LocationReceivedListener {
		void onLastLocationReceived(Location location);
		void onLocationReceived(LocationResult locationResult);
	}

	LocationHandler(final LocationReceivedListener listener, final Context context) {
		mHostListener = listener;
		mLocationClient = new FusedLocationProviderClient(context);
		mLocationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				mHostListener.onLocationReceived(locationResult);
			}
		};
	}

	void getLastBestLocation(final LocationReceivedListener listener) {
		mLocationClient.getLastLocation().addOnSuccessListener(
				listener::onLastLocationReceived);
	}

	boolean start(final LocationRequest locationRequest) {
		mLocationClient.requestLocationUpdates(
				locationRequest, mLocationCallback, Looper.getMainLooper());
		return true;
	}

	boolean stop(){
		mLocationClient.removeLocationUpdates(mLocationCallback);
		return false;
	}
}

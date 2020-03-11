package com.amw188.csit321_paws;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

class AddressHandler {
	private static final String TAG = PrefConstValues.tag_prefix + "_h_a";

	private AddressHandler.AddressReceivedListener mHostListener;

	// Interface to send updates to host activity
	interface AddressReceivedListener {
		void onAddressReceived(ArrayList<Address> addressResults);
	}

	class AddressReceiver extends ResultReceiver {
		AddressReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			if (resultData == null)
				return;
			String error = resultData.getString(FetchAddressCodes.RESULT_DATA_KEY);
			if (error == null || error.equals("")) {
				ArrayList<Address> addressResults = resultData.getParcelableArrayList(
						FetchAddressCodes.RESULT_ADDRESSLIST_KEY);

				// Debug print the full address
				for (Address address : addressResults) {
					for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
						Log.d(TAG, address.getAddressLine(i));
					}
				}
				mHostListener.onAddressReceived(addressResults);
			}
		}
	}

	AddressHandler(AddressReceivedListener listener) {
		mHostListener = listener;
	}

	static String[] getAddress(final ArrayList<Address> addressResults) {
		String[] addressLine = null;
		try {
			Log.d(TAG, "addressList: " + addressResults.toString());
			addressLine = addressResults.get(0).getAddressLine(0)
					.split(", ", 3);
		} catch (NullPointerException ex) {
			Log.e(TAG, "Failed to read from null address object.");
			ex.printStackTrace();
		}
		return addressLine;
	}

	static String getAustralianStateCode(final Address address) {
		Log.d(TAG, "Address: " + address.toString());
		final int areaInfoIndex = address.getAddressLine(0).length() > 2
				? 1 : 0;
		final String[] area = address.getAddressLine(0)
				.split(", ")[areaInfoIndex]
				.split(" ");
		final int stateCodeIndex = area.length > 2
				? 1 : 0 ;
		if (area.length > 0)
			return area[stateCodeIndex];
		return null;
	}

	static String getBestAddressTitle(final Address address) {
		if (address == null)
			return null;
		final String str = address.getFeatureName();
		if (str != null)
			if (str.toLowerCase().endsWith("beach"))
				return address.getFeatureName();
		if (address.getLocality() != null)
			return address.getLocality();
		return null;
	}

	static String getAbbreviatedAddress(final ArrayList<Address> addressResults) {
		String[] addressLine = getAddress(addressResults);
		return addressLine.length > 0
				? addressLine[Math.max(0, addressLine.length - 2)]
				: null;
	}

	void awaitAddress(final Context context, final LatLng latLng) {
		Location location = new Location(LocationManager.GPS_PROVIDER);
		location.setLatitude(latLng.latitude);
		location.setLongitude(latLng.longitude);
		awaitAddress(context, location);
	}

	void awaitAddress(final Context context, final Location location) {
		AddressReceiver addressReceiver =
				new AddressHandler.AddressReceiver(new Handler());
		FetchAddressIntentService.startActionFetchAddress(context,
				addressReceiver, location);
	}
}

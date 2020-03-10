package com.amw188.csit321_paws;

import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

class AddressHandler {
	private static final String TAG = PrefConstValues.package_name + "_ah";
	protected ArrayList<Address> mSelectedAddressList;
	protected MapsActivity.AddressResultReceiver mAddressReceiver;

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
				mSelectedAddressList = resultData.getParcelableArrayList(
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

	protected void awaitAddress(Location location) {
		FetchAddressIntentService.startActionFetchAddress(this,
				mAddressReceiver, location);
	}

	protected void onAddressReceived() {}
}

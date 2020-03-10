package com.amw188.csit321_paws;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import java.util.ArrayList;

class AddressHandler {
	private static final String TAG = PrefConstValues.tag_prefix + "_ah";

	private AddressHandler.AddressReceivedListener mHostListener;

	// Interface to send updates to host activity
	interface AddressReceivedListener {
		void onAddressReceived(int resultCode, Bundle resultData);
	}

	class AddressReceiver extends ResultReceiver {
		AddressReceiver(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			mHostListener.onAddressReceived(resultCode, resultData);
		}
	}

	AddressHandler(AddressReceivedListener listener) {
		mHostListener = listener;
	}

	static String[] getAddress(final ArrayList<Address> addressList) {
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

	static String getAbbreviatedAddress(final ArrayList<Address> addressList) {
		String[] addressLine = getAddress(addressList);
		return addressLine.length > 0
				? addressLine[Math.max(0, addressLine.length - 2)]
				: null;
	}

	void awaitAddress(final Context context, final Location location) {
		AddressReceiver addressReceiver =
				new AddressHandler.AddressReceiver(new Handler());
		FetchAddressIntentService.startActionFetchAddress(context,
				addressReceiver, location);
	}
}

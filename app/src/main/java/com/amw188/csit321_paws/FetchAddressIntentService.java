package com.amw188.csit321_paws;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    private static final String TAG = PrefConstValues.tag_prefix + "fais";

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    protected ResultReceiver mReceiver;

    /**
     * Queues up an address fetch request
     * @param context Activity context.
     * @param receiver Activity to deliver result to.
     * @param location Location object to derive address from.
     */
    public static void startActionFetchAddress(Context context,
                                               ResultReceiver receiver, Location location) {
        Intent intent = new Intent(context, FetchAddressIntentService.class);
        intent.setAction(FetchAddressCode.ACTION_FETCH_ADDRESS);
        intent.putExtra(FetchAddressCode.EXTRA_RECEIVER, receiver);
        intent.putExtra(FetchAddressCode.EXTRA_LOCATION, location);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action.equals(FetchAddressCode.ACTION_FETCH_ADDRESS)) {
                mReceiver = intent.getParcelableExtra(FetchAddressCode.EXTRA_RECEIVER);
                Location location = intent.getParcelableExtra(FetchAddressCode.EXTRA_LOCATION);
                handleActionFetchAddress(location);
            }
        }
    }

    /**
     * Handles address fetching in a background thread
     * @param location Location object to derive address from.
     */
    private void handleActionFetchAddress(Location location) {
        String error = "";
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        ArrayList<Address> addressList = null;
        try {
            addressList = (ArrayList<Address>)geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    1);
        } catch (IOException e) {
            error = getString(R.string.sv_fa_service_unavailable);
            e.printStackTrace();
            Log.e(TAG, error);
        } catch (IllegalArgumentException e) {
            error = getString(R.string.sv_fa_invalid_lat_lng);
            e.printStackTrace();
            Log.e(TAG, error);
        }

        if (addressList == null || addressList.size() == 0) {
            if (!error.isEmpty()) {
                error = getString(R.string.sv_fa_no_address);
                Log.e(TAG, error);
            }
            deliverResultToReceiver(FetchAddressCode.FAILURE_RESULT,
                    addressList, error);
        } else {
            deliverResultToReceiver(FetchAddressCode.SUCCESS_RESULT,
                    addressList, "");
        }
    }

    /**
     * Returns the address list to the receiving activity.
     * @param resultCode Success or failure code.
     * @param addressList List of address data for this location.
     * @param message Extra message, contains error info.
     */
    private void deliverResultToReceiver(int resultCode,
                                         ArrayList<Address> addressList, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(FetchAddressCode.RESULT_DATA_KEY, message);
        bundle.putParcelableArrayList(FetchAddressCode.RESULT_ADDRESSLIST_KEY, addressList);
        mReceiver.send(resultCode, bundle);
    }
}

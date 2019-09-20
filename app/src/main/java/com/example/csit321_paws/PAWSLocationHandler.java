package com.example.csit321_paws;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class PAWSLocationHandler implements LocationListener {

    private final Context mContext;

    private Location mLoc;
    private LocationManager mLocManager;
    private LocationListener mLocListener;

    private int mUpdateInterval;
    private int mUpdateDistance;
    private int mUpdateAccuracy;

    private boolean isGPSEnabled = false;
    private boolean isNetworkingEnabled = false;
    private boolean isPermitted = false;

    protected PAWSLocationHandler(Context context) {
        mContext = context;
        mLocManager = (LocationManager)(mContext.getSystemService(Context.LOCATION_SERVICE));
    }

    // Reset parameters for live location listener.
    private void setUpdateParams(int updateInterval, int updateDistance, int updateAccuracy) {
        mUpdateInterval = updateInterval;
        mUpdateDistance = updateDistance;
        mUpdateAccuracy = updateAccuracy;

        // now, restart the update request with new params.
    }

    // Create an object containing mobile location, with name and time of generation.
    protected PAWSLocation getPAWSLocation() {
        Location loc = getLocation();
        Log.println(Log.DEBUG, "snowpaws", "getPAWSLocation");

        PAWSLocation pawsLoc = null;
        if (loc != null) {
            pawsLoc = new PAWSLocation(loc, getLocationName(loc), System.currentTimeMillis());
        } else {
            Log.println(Log.DEBUG, "snowpaws", "null location");
        }
        return pawsLoc;
    }

    // Fetch nearest applicable name for location.
    private String getLocationName(Location loc) {
        String name = null;
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        List<Address> addresses;
        try {
            if (loc != null) {
                addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    name = addresses.get(0).getLocality();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return name;
    }

    // TODO : handle exceptional permission errors

    // Fetch location object
    private Location getLocation() {
        try {
            isGPSEnabled = mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkingEnabled = mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isGPSEnabled && isNetworkingEnabled) {
                if (isPermitted) {
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(mUpdateAccuracy);
                    criteria.setCostAllowed(true);

                    mLocManager.requestLocationUpdates(
                            mUpdateInterval, mUpdateDistance, criteria, mLocListener, null
                    );

                    String strLocProvider = mLocManager.getBestProvider(criteria, true);
                    mLoc = mLocManager.getLastKnownLocation(strLocProvider);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return mLoc;
    }

    @Override
    public void onLocationChanged(Location loc) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

}

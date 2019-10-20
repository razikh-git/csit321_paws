package com.example.csit321_paws;

import android.app.Activity;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class LocationHandler {

    private final Context mContext;

    private Location mLoc;
    private LocationManager mLocManager;
    private Timer mLocTimer = new Timer();

    private int mUpdateInterval;
    private int mUpdateDistance;
    private int mUpdateAccuracy;

    private boolean mIsGPSEnabled = false;
    private boolean mIsNetworkingEnabled = false;

    private LocationUpdateListener mLocationUpdateListener;

    LocationListener mLocListenerGPS = new LocationListener() {
        public void onLocationChanged(Location loc) {
            mLocTimer.cancel();
            mLoc = loc;
            mLocManager.removeUpdates(this);
            mLocManager.removeUpdates(mLocListenerNet);
        }
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    LocationListener mLocListenerNet = new LocationListener() {
        public void onLocationChanged(Location loc) {
            mLocTimer.cancel();
            mLoc = loc;
            mLocManager.removeUpdates(this);
            mLocManager.removeUpdates(mLocListenerGPS);
        }
        public void onProviderEnabled(String provider) {}
        public void onProviderDisabled(String provider) {}
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    };

    // Interface to send updates to host activity
    public interface LocationUpdateListener {
        void onLocationUpdated(Location loc);
    }

    LocationHandler(Activity activity) {
        mContext = activity.getApplicationContext();
        mLocManager = (LocationManager)(mContext.getSystemService(Context.LOCATION_SERVICE));
        mLocationUpdateListener = (LocationUpdateListener) activity;
    }

    // Reset parameters for live location listener.
    protected void setUpdateParams(int updateInterval, int updateDistance, int updateAccuracy) {
        mUpdateInterval = updateInterval;
        mUpdateDistance = updateDistance;
        mUpdateAccuracy = updateAccuracy;

        // now, restart the update request with new params.
    }

    // Initialise location objects and begin the update loop.
    protected void beginLocationUpdates() {
        getLocation();

        // Schedule location updates.
        mLocTimer.schedule(new getLastLocation(), mUpdateInterval);
    }

    // End the update loop.
    protected void endLocationUpdates() {
        if (mLocManager != null) {
            mLocManager.removeUpdates(mLocListenerGPS);
            mLocManager.removeUpdates(mLocListenerNet);
        }
    }

    // TODO : handle exceptional permission errors

    // Fetch location object.
    Location getLocation() {
        Log.println(Log.DEBUG, "snowpaws", "LocationHandler.getLocation()");

        try {
            mIsGPSEnabled = mLocManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            mIsNetworkingEnabled = mLocManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            Criteria criteria = new Criteria();
            criteria.setAccuracy(mUpdateAccuracy);
            criteria.setCostAllowed(true);

            if (mIsGPSEnabled) {
                Log.println(Log.DEBUG, "snowpaws", "getLocation.mIsGPSEnabled TRUE");

                mLocManager.requestLocationUpdates(
                        //mUpdateInterval, mUpdateDistance, criteria, mLocListenerGPS, null
                        LocationManager.GPS_PROVIDER, mUpdateInterval, mUpdateDistance, mLocListenerGPS
                );
            }

            if (mIsNetworkingEnabled) {
                Log.println(Log.DEBUG, "snowpaws", "getLocation.mIsNetworkingEnabled TRUE");

                mLocManager.requestLocationUpdates(
                        //mUpdateInterval, mUpdateDistance, criteria, mLocListenerNet, null
                        LocationManager.NETWORK_PROVIDER, mUpdateInterval, mUpdateDistance, mLocListenerNet
                );
            }

            String strLocProvider = mLocManager.getBestProvider(criteria, true);
            mLoc = mLocManager.getLastKnownLocation(strLocProvider);

            // Fetch immediate location.
            new getLastLocation().run();

        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (mLoc == null) {
            Log.println(Log.DEBUG, "snowpaws", "null location");
        }

        return mLoc;
    }

    // Repeatedly attempts to retrieve best location from GPS/networking data.
    class getLastLocation extends TimerTask {
        @Override
        public void run() {

            Log.println(Log.DEBUG, "snowpaws", "TimerTask.getLastLocation.run()");

            Location locGPS = null;
            Location locNet = null;

            try {
                if (mIsGPSEnabled) {
                    locGPS = mLocManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                } else
                if (mIsNetworkingEnabled) {
                    locNet = mLocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                if (locGPS != null)
                    Log.println(Log.DEBUG, "snowpaws", "TimerTask.getLastLocation.run.locGPS TRUE");
                if (locNet != null)
                    Log.println(Log.DEBUG, "snowpaws", "TimerTask.getLastLocation.run.locNet TRUE");

                // Retrieve latest of both results.
                if (locGPS != null && locNet != null) {
                    if (locGPS.getTime() > locNet.getTime()) {
                        mLoc = locGPS;
                    } else {
                        mLoc = locNet;
                    }
                } else if (locGPS != null){
                    mLoc = locGPS;
                } else if (locNet != null) {
                    mLoc = locNet;
                } else {
                    mLoc = null;
                }

                // Push updated locations to host activities.
                if (mLoc != null) {
                    Log.println(Log.DEBUG, "snowpaws", "mLocationUpdateListener.onLocationUpdated");
                    mLocationUpdateListener.onLocationUpdated(getLocation());
                }

                // Schedule next update.
                /*
                if (mLocManager != null) {
                    mLocTimer.schedule(new getLastLocation(), mUpdateInterval);
                }
                */

            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }
}

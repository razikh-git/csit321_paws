package com.example.csit321_paws;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class PAWSLocationListener implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private final Context mContext;

    private Location mLoc;
    private LocationManager mLocManager;
    private LocationRequest mLocRequest;
    private GoogleApiClient mGAPIClient;

    private int mUpdatePriority;
    private int mUpdateInterval;
    private int mUpdateFastestInterval;

    private boolean isGPSEnabled = false;
    private boolean isNetworkingEnabled = false;
    private boolean isPermitted = false;

    public PAWSLocationListener(Context context) {
        this.mContext = context;
        this.mGAPIClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        getLocation();
    }

    public void setUpdateParams(int updatePriority, int updateInterval, int updateFastestInterval) {
        mUpdatePriority = updatePriority;
        mUpdateInterval = updateInterval;
        mUpdateFastestInterval = updateFastestInterval;

        // now, restart the update request with new params
    }

    public void getLocation() {
        LocationManager locManager = (LocationManager)(mContext.getSystemService(Context.LOCATION_SERVICE));
        isGPSEnabled = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkingEnabled = locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isGPSEnabled && !isNetworkingEnabled) {
            return;
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO : request confirm/denial
        }

        // start updating location

        mLoc = LocationServices.FusedLocationApi.getLastLocation(mGAPIClient);
        if (mLoc == null) {

            // fire callback to home or something

        }
    }

    protected void requestUpdates() {
        mLocRequest = LocationRequest.create()
                .setPriority(mUpdatePriority)
                .setInterval(mUpdateInterval)
                .setFastestInterval(mUpdateFastestInterval);
        if (!isPermitted) {
            return;
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGAPIClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location loc) {}

}

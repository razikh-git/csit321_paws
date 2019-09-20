package com.example.csit321_paws;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashMap;

public class HomeActivity extends PermissionActivity {

    private static final String[] REQUEST_PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;
    private PAWSLocationHandler mLocHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mSharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        initStrings();
        initLocationData();
    }

    private boolean initStrings() {
        try {
            // Initialise map of request and permission codes.
            mCodeMap = new HashMap<>();
            mCodeMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    RequestCode.PERMISSION_LOCATION_COARSE);
            mCodeMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    RequestCode.PERMISSION_LOCATION_FINE);
            mCodeMap = Collections.unmodifiableMap(mCodeMap);

            // Initialise map of title messages.
            mTitleMap = new HashMap<>();
            mTitleMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    getResources().getString(R.string.app_title_request_loc_coarse));
            mTitleMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    getResources().getString(R.string.app_title_request_loc_fine));

            // Initialise map of request messages.
            mMessageMap = new HashMap<>();
            mMessageMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    getResources().getString(R.string.app_msg_request_loc_coarse));
            mMessageMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    getResources().getString(R.string.app_msg_request_loc_fine));

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // TODO : store last-known location in binary, allow for out-of-service use of old data

    private boolean initLocationData() {
        PAWSLocation loc = null;
        mLocHandler = new PAWSLocationHandler(getApplicationContext());

        Log.println(Log.DEBUG, "snowpaws", "welcome to initLocationData");

        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE, REQUEST_PERMISSIONS_LOCATION)) {

            Log.println(Log.DEBUG, "snowpaws", "i'm in");

            // Fetch location data.
            TextView text;
            loc = mLocHandler.getPAWSLocation();
            if (loc != null) {
                // Initialise location displays.
                text = findViewById(R.id.txtCity);
                text.setText(loc.name());

                text = findViewById(R.id.txtTimestampTop);
                text.setText(DateFormat.format("HH:mm:ss", loc.timestamp()).toString());

                text = findViewById(R.id.txtTimestampBottom);
                text.setText(DateFormat.format("dd-MM-yyyy", loc.timestamp()).toString());

                // Generate URL and request OWM data.

                // Initialise weather conditions.

                return true;
            }

            // If no latest location could be fetched, post fallback info.

            // . . .

        } else {
            // Determine warnings and layout.

            // . . .

        }

        return false;
    }

    @Override
    protected void onPermissionGranted(String perm) {

    }

    @Override
    protected void onPermissionBlocked(String perm) {

    }

    @Override
    protected void onAllPermissionsGranted(String[] permissions) {

    }
}

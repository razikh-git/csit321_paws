package com.example.csit321_paws;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class HomeActivity   extends
                                PermissionActivity
                            implements
                                LocationHandler.LocationUpdateListener
{

    private static final String[] REQUEST_PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final String[] REQUEST_PERMISSIONS_NETWORK = {
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;
    private LocationHandler mLocHandler;
    private JSONObject mWeatherJSON;

    private String mLat;
    private String mLon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // ScrollingActivity code

        /*
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */

        mSharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        initActivity();
    }

    private boolean initActivity() {
        Location loc = null;
        mLocHandler = new LocationHandler(this);

        Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initActivity()");

        // Fetch location data and set all fields.
        initStringMaps();
        //mLocHandler.beginLocationUpdates();
        initInterface(loc);

        return false;
    }

    // TODO : store last-known location in binary, allow for out-of-service use of old data

    public boolean initInterface(Location loc) {

        Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initInterface()");

        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE, REQUEST_PERMISSIONS_LOCATION)) {

            Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initInterface.hasPermssions TRUE");

            if (initLocationData(loc)) {
                // Locations were fetched successfully.

                Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initInterface.initLocationData TRUE");

                // . . .

                return true;

            } else {
                // If no latest location could be fetched, post fallback info.

                Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initInterface.initLocationData FALSE");

                // . . .

            }

        } else {
            // Determine warnings and layout.

            Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initInterface.hasPermssions FALSE");

            // . . .
        }

        return false;
    }

    public boolean initLocationData(Location loc) {

        Log.println(Log.DEBUG, "snowpaws", "HomeActivity.initInterface.initLocationData()");

        mLat = getResources().getString(R.string.app_default_loc_lat);
        mLon = getResources().getString(R.string.app_default_loc_lon);

        // Use data from locational tracking if possible.
        /*
        if (loc != null) {
            text = findViewById(R.id.txtTimestampTop);
            text.setText(DateFormat.format("HH:mm:ss", loc.getTime()).toString());

            text = findViewById(R.id.txtTimestampBottom);
            text.setText(DateFormat.format("dd-MM-yyyy", loc.getTime()).toString());
        }
        */

        // Generate URL and request OWM data.
        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE, REQUEST_PERMISSIONS_NETWORK)) {
            // Generate URL and request queue.
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = getResources().getString(R.string.app_url_protocol_secure)
                    + getResources().getString(R.string.app_url_owmapi_root)
                    + "weather" + "?lat=" + mLat + "&lon=" + mLon
                    + "&appid=" + getResources().getString(R.string.owm_default_api_key);

            // Generate and post the request.
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {

                            Log.println(Log.DEBUG, "snowpaws", "stringRequest.onResponse");

                            // Set JSON dict and view fields from response.
                            try {
                                mWeatherJSON = new JSONObject(response);
                                initWeatherData();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                            Log.println(Log.DEBUG, "snowpaws", "stringRequest.onErrorResponse");

                            // olive oil didn't work
                            error.printStackTrace();
                        }
                    }
            );

            queue.add(stringRequest);
        }

        return false;
    }

    // Initialise weather conditions fields.
    private void initWeatherData() {
        try {
            TextView text;

            text = findViewById(R.id.txtFooterTime);
            text.setText(DateFormat.format("HH:mm:ss",
                    Long.parseLong(mWeatherJSON.getString("dt"))).toString());

            text = findViewById(R.id.txtFooterDate);
            text.setText(DateFormat.format("dd-MM-yyyy",
                    Long.parseLong(mWeatherJSON.getString("dt"))).toString());

            text = findViewById(R.id.txtCity);
            text.setText(mWeatherJSON.getString("name")
                    + " (" + mLon + " " + mLat + ")");

            text = findViewById(R.id.txtCountry);
            text.setText("(" + mWeatherJSON.getJSONObject("sys").getString("country") + ")");

            text = findViewById(R.id.txtTimezone);
            text.setText("GMT" + TimeUnit.HOURS.convert(
                    Integer.parseInt(mWeatherJSON.getString("timezone")), TimeUnit.SECONDS));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean initStringMaps() {
        try {
            // Initialise map of request and permission codes.
            mCodeMap = new HashMap<>();
            // Location
            mCodeMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    RequestCode.PERMISSION_LOCATION_COARSE);
            mCodeMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    RequestCode.PERMISSION_LOCATION_FINE);
            // Network
            mCodeMap.put(Manifest.permission.INTERNET,
                    RequestCode.PERMISSION_INTERNET);
            mCodeMap.put(Manifest.permission.ACCESS_NETWORK_STATE,
                    RequestCode.PERMISSION_NETWORK_STATE);

            // Render immutable
            mCodeMap = Collections.unmodifiableMap(mCodeMap);

            // Initialise map of title messages.
            mTitleMap = new HashMap<>();
            // Location
            mTitleMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    getResources().getString(R.string.app_title_request_loc_coarse));
            mTitleMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    getResources().getString(R.string.app_title_request_loc_fine));
            // Network
            mTitleMap.put(Manifest.permission.INTERNET,
                    getResources().getString(R.string.app_title_request_internet));
            mTitleMap.put(Manifest.permission.ACCESS_NETWORK_STATE,
                    getResources().getString(R.string.app_title_request_network_state));
            // Render immutable
            mTitleMap = Collections.unmodifiableMap(mTitleMap);

            // Initialise map of request messages.
            mMessageMap = new HashMap<>();
            // Location
            mMessageMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    getResources().getString(R.string.app_msg_request_loc_coarse));
            mMessageMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    getResources().getString(R.string.app_msg_request_loc_fine));
            // Network
            mMessageMap.put(Manifest.permission.INTERNET,
                    getResources().getString(R.string.app_msg_request_internet));
            mMessageMap.put(Manifest.permission.ACCESS_NETWORK_STATE,
                    getResources().getString(R.string.app_msg_request_network_state));
            // Render immutable
            mMessageMap = Collections.unmodifiableMap(mMessageMap);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPermissionGranted(String perm) {

    }

    @Override
    protected void onPermissionBlocked(String perm) {

    }

    @Override
    protected void onAllPermissionsGranted(String[] permissions) {
        if (Arrays.asList(permissions).contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        || (Arrays.asList(permissions).contains(Manifest.permission.INTERNET))) {
            initInterface(mLocHandler.getLocation());
        }
    }

    @Override
    public void onLocationUpdated(Location loc) {
        Log.println(Log.DEBUG, "snowpaws", "HomeActivity.onLocationUpdated()");

        // Reinitialise all relevant fields.
        initLocationData(loc);
    }
}

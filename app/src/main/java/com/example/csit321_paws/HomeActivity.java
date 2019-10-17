package com.example.csit321_paws;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class HomeActivity
        extends
            PermissionActivity
        implements
            LocationHandler.LocationUpdateListener,
            WeatherHandler.WeatherForecastReceivedListener
{

    private static final String[] REQUEST_PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private static final String[] REQUEST_PERMISSIONS_NETWORK = {
            Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE};

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;
    private LocationHandler mLocHandler;

    private double mLat;
    private double mLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // consider this for sticky incomplete survey banner

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

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        // Initialise vanity and interactive interface elements.
        initStringMaps();
        initButtons();

        // Location management.
        Location loc = null;
        mLocHandler = new LocationHandler(this);

        // Fetch location data and set all fields.
        //mLocHandler.beginLocationUpdates();
        initInterface(loc);

    }

    public void onWeatherForecastReceived(LatLng latLng, String response) {
        initWeatherData(latLng, response);
    }

    private boolean initButtons() {
        // Button functionality.
        try {
            findViewById(R.id.layWeatherDataContainer).setOnClickListener((view) -> onClickWeather(view));
            findViewById(R.id.cardWarningBanner).setOnClickListener((view) -> onClickSurveys(view));
            findViewById(R.id.cardSurveys).setOnClickListener((view) -> onClickSurveys(view));
            findViewById(R.id.cardMaps).setOnClickListener((view) -> onClickMaps(view));
            findViewById(R.id.cardSettings).setOnClickListener((view) -> onClickSettings(view));
            findViewById(R.id.cardHelp).setOnClickListener((view) -> onClickHelp(view));
            findViewById(R.id.cardProfile).setOnClickListener((view) -> onClickProfile(view));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // TODO : store last-known location in binary, allow for out-of-service use of old data

    private boolean initInterface(Location loc) {

        // Initialise the warning banner.
        if (mSharedPref.getInt("survey_last_question", 1)
                < getResources().getInteger(R.integer.survey_question_count))
            findViewById(R.id.cardWarningBanner).setVisibility(VISIBLE);

        // Attempt to initialise location elements.
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

    private boolean initLocationData(Location loc) {

        Log.println(Log.DEBUG, "snowpaws_home", "HomeActivity.initInterface.initLocationData()");

        mLat = Double.parseDouble(getString(R.string.app_default_loc_lat));
        mLng = Double.parseDouble(getString(R.string.app_default_loc_lng));

        // Use data from locational tracking if possible.
/*
        if (loc != null) {
            ((TextView)findViewById(R.id.txtTimestampTop)).setText(
                DateFormat.format("HH:mm:ss", loc.getTime()).toString());
            ((TextView)findViewById(R.id.txtTimestampBottom)).setText(
                DateFormat.format("dd-MM-yyyy", loc.getTime()).toString());
        }
*/

        Log.println(Log.DEBUG, "snowpaws_home", "Loaded lat/long presets.");

        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE, REQUEST_PERMISSIONS_NETWORK)) {
            WeatherHandler.updateLatestWeatherForecast(this, new LatLng(mLat, mLng));
        }

        return false;
    }

    // Initialise weather conditions fields.
    private boolean initWeatherData(LatLng latLng, String response) {
        try {
            int index = 0;
            String str;
            Double dbl;

            JSONObject weatherForecastJSON = null;
            JSONObject weatherCurrentJSON = null;

            try {
                // Current weather object
                weatherForecastJSON = new JSONObject(response);
                weatherCurrentJSON = (JSONObject)(weatherForecastJSON.getJSONArray("list")
                        .getJSONObject(index)
                        .getJSONArray("weather").get(0));
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }

            // Fill in header data.

            /*
            // Weather icon
            str = getString(R.string.app_url_owm_root)
                    + "img/"
                    + "wn/"
                    + weatherCurrentJSON.getString("icon")
                    + ".png";
            ImageLoader.getInstance().displayImage(str, (ImageView) findViewById(R.id.imgWeatherIcon),
                    null, new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String imageUri, View view) {
                            // Display already-visible circular progress bar.
                        }

                        @Override
                        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                            // Hide progress bar.
                            findViewById(R.id.barWeatherIcon).setVisibility(GONE);

                            // Display error icon.
                            view.setVisibility(VISIBLE);
                            ((ImageView)view).setColorFilter(ContextCompat.getColor(
                                    view.getContext(), R.color.color_on_primary_light));
                            ((ImageView)view).setImageDrawable(getDrawable(R.drawable.ic_cloud_off));
                        }

                        @Override
                        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                            // Hide progress bar.
                            findViewById(R.id.barWeatherIcon).setVisibility(GONE);

                            // Display weather icon.
                            view.setVisibility(VISIBLE);
                            findViewById(R.id.imgWeatherIcon).setVisibility(VISIBLE);
                            ((ImageView)view).setImageBitmap(loadedImage);
                        }

                        @Override
                        public void onLoadingCancelled(String imageUri, View view) {
                            // Hide progress bar.
                            findViewById(R.id.barWeatherIcon).setVisibility(GONE);

                            // Display error icon.
                            view.setVisibility(VISIBLE);
                            ((ImageView)view).setColorFilter(ContextCompat.getColor(
                                    view.getContext(), R.color.color_on_primary_light));
                            ((ImageView)view).setImageDrawable(getDrawable(R.drawable.ic_cloud_off));
                        }
            });
            */

            // Set icon for weather type.
            Drawable drawable = PAWSAPI.getWeatherDrawable(this, weatherCurrentJSON.getString("icon"));

            // Hide progress bar.
            findViewById(R.id.barWeatherIcon).setVisibility(GONE);

            ImageView img = findViewById(R.id.imgWeatherIcon);
            if (drawable != null) {
                // Display weather icon.
                img.setVisibility(VISIBLE);
                img.setImageDrawable(drawable);
            } else {
                // Display error icon.
                img.setVisibility(VISIBLE);
                img.setColorFilter(ContextCompat.getColor(
                        this, R.color.color_on_primary_light));
                img.setImageDrawable(getDrawable(R.drawable.ic_cloud_off));
            }

            // Weather description
            ((TextView)findViewById(R.id.txtWeatherDescription)).setText(
                    weatherCurrentJSON.getString("description"));

            // City name
            ((TextView)findViewById(R.id.txtCity)).setText(
                    weatherForecastJSON.getJSONObject("city").getString("name"));
            // Country code
            ((TextView)findViewById(R.id.txtCountry)).setText(
                    weatherForecastJSON.getJSONObject("city").getString("country"));
            // GPS Coordinates
            char bearingLng = mLng > 0 ? 'E' : 'W';
            char bearingLat = mLat > 0 ? 'N' : 'S';
            ((TextView)findViewById(R.id.txtCoordinates)).setText(
                    Math.abs(mLng) + " " + bearingLng + "  " + Math.abs(mLat) + " " + bearingLat);

            // Fill in body data.

            // Temperature (current)
            str = mSharedPref.getString("units", "metric").equals("metric") ?
                "°C" : "°F";
            ((TextView)findViewById(R.id.txtTempCurrent)).setText(
                    String.valueOf(
                            Math.round(weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                    .getJSONObject("main").getDouble("temp")))
                    + str);

            // Wind (speed)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                    .getJSONObject("wind").getDouble("speed");
            if (mSharedPref.getString("units", "metric").equals("metric")) {
                str = " km/h";
                dbl *= PAWSAPI.MS_TO_MPH;
            }
            else {
                str = " mph";
                dbl *= PAWSAPI.MS_TO_KMH;
            }
            ((TextView)findViewById(R.id.txtWindSpeed)).setText(
                    String.valueOf(
                            Math.round(dbl)
                    + str));

            // Wind (bearing)
            str = "north";
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                    .getJSONObject("wind").getDouble("deg");
            if (dbl < 135)
                str = "west";
            else if (dbl < 225)
                str = "south";
            else if (dbl < 315)
                str = "east";
            ((TextView)findViewById(R.id.txtWindBearing)).setText(str);

            // Weather type
            str = weatherCurrentJSON.getString("main");
            switch (str) {
                case "Clear":
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                    .getJSONObject("main").getString("humidity")
                            + "%");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            "humidity");
                    break;
                case "Clouds":
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                    .getJSONObject("clouds").getString("all")
                            + "%");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            "coverage");
                    break;
                case "Thunderstorm":
                case "Drizzle":
                case "Rain":
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            new DecimalFormat("#.##").format(
                                    weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                    .getJSONObject("rain").getDouble("3h"))
                            + "mm");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            "last 3 hrs");
                    break;
                case "Snow" :
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            new DecimalFormat("#.##").format(
                                    weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                    .getJSONObject("snow").getString("3h"))
                            + "mm");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            "last 3 hrs");
                    break;
            }

            // Time of forecast
            ((TextView)(findViewById(R.id.txtFooterTime))).setText(
                    DateFormat.format("HH:mm dd.MM.yyyy",
                    weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                            .getLong("dt") * 1000).toString());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
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
                    getString(R.string.app_title_request_loc_coarse));
            mTitleMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    getString(R.string.app_title_request_loc_fine));
            // Network
            mTitleMap.put(Manifest.permission.INTERNET,
                    getString(R.string.app_title_request_internet));
            mTitleMap.put(Manifest.permission.ACCESS_NETWORK_STATE,
                    getString(R.string.app_title_request_network_state));
            // Render immutable
            mTitleMap = Collections.unmodifiableMap(mTitleMap);

            // Initialise map of request messages.
            mMessageMap = new HashMap<>();
            // Location
            mMessageMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    getString(R.string.app_msg_request_loc_coarse));
            mMessageMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    getString(R.string.app_msg_request_loc_fine));
            // Network
            mMessageMap.put(Manifest.permission.INTERNET,
                    getString(R.string.app_msg_request_internet));
            mMessageMap.put(Manifest.permission.ACCESS_NETWORK_STATE,
                    getString(R.string.app_msg_request_network_state));
            // Render immutable
            mMessageMap = Collections.unmodifiableMap(mMessageMap);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void onClickWeather(View view) {
        // Redirect to Weather Activity
        Intent intent = new Intent(this, WeatherActivity.class);
        startActivity(intent);
    }

    private void onClickMaps(View view) {
        // Redirect to Maps Activity
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    private void onClickSurveys(View view) {
        // Redirect to Profiling Launchpad Activity
        Intent intent = new Intent(this, ProfilingMenuActivity.class);
        startActivity(intent);
    }

    private void onClickProfile(View view) {
        // Redirect to Profile Details Activity
        //Intent intent = new Intent(this, ProfileDetailsActivity.class);
        //startActivity(intent);
    }

    private void onClickSettings(View view) {
        // Redirect to Settings Activity
        //Intent intent = new Intent(this, SettingsActivity.class);
        //startActivity(intent);
    }

    private void onClickHelp(View view) {
        // Redirect to Help Page Activity
        //Intent intent = new Intent(this, HelpPageActivity.class);
        //startActivity(intent);
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

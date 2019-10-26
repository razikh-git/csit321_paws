package com.amw188.csit321_paws;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class HomeActivity
        extends LocationActivity
        implements WeatherHandler.WeatherReceivedListener
{
    private static final String TAG = "snowpaws_home";

    private SharedPreferences mSharedPref;

    WeatherHandler mWeatherHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

        // Initialise vanity and interactive interface elements.
        initStringMaps();
        initButtons();
        initInterface();
    }

    @Override
    public void onWeatherReceived(LatLng latLng, String response, boolean isMetric) {
        initWeatherDisplay(response);
    }

    private boolean initButtons() {
        // Button functionality.
        try {
            findViewById(R.id.cardWarningBanner).setOnClickListener((view) -> onClickProfiling(view));
            findViewById(R.id.cardWeather).setOnClickListener((view) -> onClickWeather(view));
            findViewById(R.id.cardMaps).setOnClickListener((view) -> onClickMaps(view));
            findViewById(R.id.btnSettings).setOnClickListener((view) -> onClickSettings(view));
            findViewById(R.id.btnProfile).setOnClickListener((view) -> onClickProfiling(view));
            findViewById(R.id.btnHelp).setOnClickListener((view) -> onClickHelp(view));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void initInterface() {
        // Initialise home screen banners.
        if (mSharedPref.getInt("survey_last_question", 1) < getResources().getInteger(R.integer.survey_question_count)) {
            findViewById(R.id.cardWarningBanner).setVisibility(VISIBLE);
            float pad = getResources().getDimension(R.dimen.height_banners_contextual);
            findViewById(R.id.layHome).setPadding(0, (int)pad, 0, 0);
        }

        // Attempt to initialise location elements.
        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE,
                RequestCode.REQUEST_PERMISSIONS_LOCATION)) {
            Log.i("snowpaws", "HomeActivity.initInterface.hasPermssions TRUE");
            fetchLocation();
        }
    }

    // Initialise weather conditions fields.
    private boolean initWeatherDisplay(String response) {
        try {
            boolean isMetric = mSharedPref.getString("units", "metric")
                    .equals("metric");

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

            // Hide progress bar.
            findViewById(R.id.barWeatherIcon).setVisibility(GONE);
            // Set icon for weather type.
            Drawable drawable = PAWSAPI.getWeatherDrawable(this, weatherCurrentJSON.getString("icon"));
            ImageView img = findViewById(R.id.imgWeatherIcon);
            if (drawable != null) {
                // Display weather icon.
                img.setVisibility(VISIBLE);
                img.setImageDrawable(drawable);
            } else {
                // Display error icon.
                img.setVisibility(VISIBLE);
                img.setColorFilter(ContextCompat.getColor(
                        this, R.color.color_on_primary));
                img.setImageDrawable(getDrawable(R.drawable.ic_cloud_off));
            }

            // Weather description
            ((TextView)findViewById(R.id.txtWeatherDescription)).setText(
                    weatherCurrentJSON.getString("description"));

            // City name
            ((TextView)findViewById(R.id.txtCity)).setText(
                    weatherForecastJSON.getJSONObject("city").getString("name"));

            // Time of forecast
            ((TextView)(findViewById(R.id.txtWeatherTimestamp))).setText(
                    getString(R.string.home_weather_timestamp) + " " +
                            DateFormat.format("dd/MM HH:mm",
                                    weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                            .getLong("dt") * 1000).toString());

            // Fill in body data.

            // Temperature (current)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                            .getJSONObject("main").getDouble("temp");
            str = PAWSAPI.getTemperatureString(isMetric, dbl, true);
            ((TextView)findViewById(R.id.txtTempCurrent)).setText(str);

            // Wind (speed)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                    .getJSONObject("wind").getDouble("speed");
            str = PAWSAPI.getWindSpeedString(isMetric, dbl);
            ((TextView)findViewById(R.id.txtWindSpeed)).setText(str);

            // Wind (bearing)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                    .getJSONObject("wind").getDouble("deg");
            str = PAWSAPI.getWindBearingString(dbl);
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
                            getString(R.string.home_clear_label));
                    break;
                case "Clouds":
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                                    .getJSONObject("clouds").getString("all")
                            + "%");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_cloud_label));
                    break;
                case "Thunderstorm":
                case "Drizzle":
                case "Rain":
                    dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                            .getJSONObject("rain").getDouble("3h");
                    str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(str);
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_precip_label));
                    break;
                case "Snow" :
                    dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                            .getJSONObject("snow").getDouble("3h");
                    str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(str);
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_precip_label));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean initLocationDisplay() {
        if (checkHasPermissions(RequestCode.PERMISSION_MULTIPLE, RequestCode.REQUEST_PERMISSIONS_NETWORK)) {
            if (mLocation != null) {
                // Call and await an update to the weather JSON string in prefs.
                boolean isMetric = mSharedPref.getString("units", "metric").equals("metric");
                LatLng latLng = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                mWeatherHandler = new WeatherHandler(this);
                if (!mWeatherHandler.updateWeather(this, latLng, isMetric)) {
                    // Initialise weather displays with last best values if none are being updated.
                    initWeatherDisplay(mSharedPref.getString("last_weather_json", "{}"));
                }
            }
        }

        return false;
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

    private void onClickSettings(View view) {
        // Redirect to App Settings Activity
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void onClickProfiling(View view) {
        // Redirect to Profiling Menu Activity
        Intent intent = new Intent(this, ProfilingMenuActivity.class);
        startActivity(intent);
    }

    private void onClickHelp(View view) {
        // Redirect to First-Launch Info Page Activity
        Intent intent = new Intent(this, ProfilingPromptActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onPermissionGranted(String perm) {}

    @Override
    protected void onPermissionBlocked(String perm) {}

    @Override
    protected void onAllPermissionsGranted(String[] permissions) {
        if (Arrays.asList(permissions).contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        || (Arrays.asList(permissions).contains(Manifest.permission.INTERNET))) {
            initInterface();
        }
    }

    @Override
    protected void onLocationReceived() {
        // Reinitialise all relevant fields.
        initLocationDisplay();
    }
}

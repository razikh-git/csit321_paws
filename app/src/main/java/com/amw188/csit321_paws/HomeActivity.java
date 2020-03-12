package com.amw188.csit321_paws;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class HomeActivity
        extends LocationActivity
        implements WeatherHandler.WeatherReceivedListener
{
    private static final String TAG = PrefConstValues.tag_prefix + "a_home";

    private SharedPreferences mSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (!init()) {
            Log.e(TAG, "Failed to completely initialise HomeActivity.");
        }
    }

    /**
     * Initialise interactive and vanity interface elements.
     * @return Operation success.
     */
    private boolean init() {
        return initStringMaps() && initClickables() && initInterface();
    }

    /**
     * Adds button functionalities.
     * @return Operation success.
     */
    private boolean initClickables() {
        try {
            findViewById(R.id.cardWarningBanner).setOnClickListener(this::onClickProfiling);
            findViewById(R.id.cardWeather).setOnClickListener(this::onClickWeather);
            findViewById(R.id.cardMaps).setOnClickListener(this::onClickMaps);
            findViewById(R.id.btnSettings).setOnClickListener(this::onClickSettings);
            findViewById(R.id.btnProfile).setOnClickListener(this::onClickProfiling);
            findViewById(R.id.btnHelp).setOnClickListener(this::onClickHelp);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Called from onCreate.
     * Adds contextual banners and displays.
     * Parses location data into the actiity via fetchlocation => initLocationDisplay.
     * @return Operation success.
     */
    private boolean initInterface() {
        // Initialise home screen banners
        if (mSharedPref.getInt(PrefKeys.survey_last_question, 1)
                < PAWSAPI.getSurveyQuestionCount(this)) {
            findViewById(R.id.cardWarningBanner).setVisibility(VISIBLE);
            final float pad = getResources().getDimension(R.dimen.height_banners_contextual);
            findViewById(R.id.layHome).setPadding(0, (int)pad, 0, 0);
        }

        // Attempt to initialise location elements
        if (checkHasPermissions(RequestCodes.PERMISSION_MULTIPLE,
                RequestCodes.REQUEST_PERMISSIONS_LOCATION)) {
            Log.i(TAG, "HomeActivity.initInterface.hasPermssions TRUE");
            awaitLocation();
        }
        return true;
    }

    /**
     * Called from onWeatherReceived (PlaceInfoActivity.WeatherReceivedListener)
     * and initLocationInterface.
     * Parses a forecast string into data strings and displays in the activity.
     * @param response Incredibly long string containing weather 5-day forecast.
     * @return Operation success.
     */
    private boolean initWeatherDisplay(String response) {
        try {
            final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);
            int whichTime;
            String str;
            Double dbl;

            JSONObject weatherForecastJSON;
            JSONObject weatherCurrentJSON;

            try {
                // Current weather object
                weatherForecastJSON = new JSONObject(response);
                whichTime = PAWSAPI.getWeatherJsonIndexForTime(
                        weatherForecastJSON.getJSONArray("list"),
                        System.currentTimeMillis());
                weatherCurrentJSON = (JSONObject)(weatherForecastJSON.getJSONArray("list")
                        .getJSONObject(whichTime).getJSONArray("weather").get(0));
            } catch (JSONException ex) {
                ex.printStackTrace();
                return false;
            }

            // Hide progress bar
            findViewById(R.id.barWeatherIcon).setVisibility(GONE);
            // Set icon for weather type
            Drawable drawable = PAWSAPI.getWeatherDrawable(this, weatherCurrentJSON.getString("icon"));
            ImageView img = findViewById(R.id.imgWeatherIcon);
            if (drawable != null) {
                // Display weather icon
                img.setVisibility(VISIBLE);
                img.setImageDrawable(drawable);
            } else {
                // Display error icon
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
                    PAWSAPI.getWeatherTimestampString(this,
                            weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                            .getLong("dt") * 1000));

            // Fill in body data

            // Temperature (current)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                            .getJSONObject("main").getDouble("temp");
            str = PAWSAPI.getTemperatureString(dbl, isMetric);
            ((TextView)findViewById(R.id.txtTempCurrent)).setText(str);

            // Wind (speed)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                    .getJSONObject("wind").getDouble("speed");
            str = PAWSAPI.getWindSpeedString(dbl, isMetric);
            ((TextView)findViewById(R.id.txtWindSpeed)).setText(str);

            // Wind (bearing)
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                    .getJSONObject("wind").getDouble("deg");
            str = PAWSAPI.getWindBearingString(this, dbl);
            ((TextView)findViewById(R.id.txtWindBearing)).setText(str);

            // Weather type
            str = weatherCurrentJSON.getString("main");
            switch (str) {
                case "Clear":
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                                    .getJSONObject("main").getString("humidity")
                            + "%");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_clear_label));
                    break;
                case "Clouds":
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(
                            weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                                    .getJSONObject("clouds").getString("all")
                            + "%");
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_cloud_label));
                    break;
                case "Thunderstorm":
                case "Drizzle":
                case "Rain":
                    dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                            .getJSONObject("rain").getDouble("3h");
                    str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(str);
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_precip_label));
                    break;
                case "Snow" :
                    dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                            .getJSONObject("snow").getDouble("3h");
                    str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                    ((TextView)findViewById(R.id.txtPrecipAuxData1)).setText(str);
                    ((TextView)findViewById(R.id.txtPrecipAuxData2)).setText(
                            getString(R.string.home_precip_label));
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Called from onLocationReceived after LocationActivity.awaitLocation.
     * Updates location strings in activity with latest results.
     * Calls initWeatherDisplay if data in strings is out of date.
     * @return Operation success.
     */
    private boolean initLocationDisplay() {
        if (checkHasPermissions(
                RequestCodes.PERMISSION_MULTIPLE, RequestCodes.REQUEST_PERMISSIONS_NETWORK)) {
            if (mSelectedLocation != null) {
                // Call and await an update to the weather JSON string in prefs
                boolean success = true;
                final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);
                LatLng latLng = new LatLng(
                        mSelectedLocation.getLatitude(), mSelectedLocation.getLongitude());
                if (!new WeatherHandler(this, this).awaitWeatherUpdate(
                        latLng, isMetric)) {
                    // Initialise weather displays with last best values if none are being updated
                    success = initWeatherDisplay(mSharedPref.getString(
                            PrefKeys.last_weather_json, PrefConstValues.empty_json_object));
                }
                return success;
            }
        }
        return false;
    }

    /**
     * Called in onCreate.
     * Populates hashmaps of functional request/permission codes and vanity strings.
     * @return Operation success.
     */
    private boolean initStringMaps() {
        try {
            // Initialise map of request and permission codes
            mCodeMap = new HashMap<>();
            // Location
            mCodeMap.put(Manifest.permission.ACCESS_COARSE_LOCATION,
                    RequestCodes.PERMISSION_LOCATION_COARSE);
            mCodeMap.put(Manifest.permission.ACCESS_FINE_LOCATION,
                    RequestCodes.PERMISSION_LOCATION_FINE);
            // Network
            mCodeMap.put(Manifest.permission.INTERNET,
                    RequestCodes.PERMISSION_INTERNET);
            mCodeMap.put(Manifest.permission.ACCESS_NETWORK_STATE,
                    RequestCodes.PERMISSION_NETWORK_STATE);

            // Render immutable
            mCodeMap = Collections.unmodifiableMap(mCodeMap);

            // Initialise map of title messages
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

            // Initialise map of request messages
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

        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    private void onClickWeather(View view) {
        // Redirect to Weather Activity
        Intent intent = new Intent(this, PlaceInfoActivity.class);
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
        // Redirect to Survey Entry Activity
        Intent intent = new Intent(this, SurveyEntryActivity.class);
        startActivity(intent);
    }

    private void onClickHelp(View view) {
        // Redirect to First-Launch Info Page Activity
        Intent intent = new Intent(this, SurveyInfosheetActivity.class);
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

    /**
     * Override of WeatherHandler.WeatherReceivedListener.
     * Called from WeatherHandler.getWeather in WeatherHandler.awaitWeatherUpdate.
     * Acts on the weather forecast for the coming week.
     * Updates weather display fields in the activity, and handles
     * weather update notifictions from the service.
     * @param latLng Latitude/longitude of weather data batch.
     * @param response Incredibly long string containing weather 5-day forecast.
     * @param isMetric Metric or imperial measurements.
     */
    @Override
    public void onWeatherReceived(LatLng latLng, String response, boolean isMetric) {
        initWeatherDisplay(response);
    }

    /**
     * Override of LocationActivity.onLocationReceived.
     * Called from LocationActivity.awaitLocation.
     * Redirects to initLocationDisplay to update data displays in the activity.
     */
    @Override
    protected void onLocationReceived() {
        if (!initLocationDisplay()) {
            Log.e(TAG, "Failed to update location elements.");
        }
    }
}

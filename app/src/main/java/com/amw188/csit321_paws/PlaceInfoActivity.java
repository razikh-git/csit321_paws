package com.amw188.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.preference.Preference;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class PlaceInfoActivity
        extends
                BottomNavBarActivity
        implements
                WeatherHandler.WeatherReceivedListener,
                Preference.OnPreferenceChangeListener
{
    private static final String TAG = PrefConstValues.tag_prefix + "a_inf";

    private SharedPreferences mSharedPref;

    private String mNearbyPlace;
    private LatLng mPlaceLatLng;

    // todo: add uncertainty to precipitation measures to avoid 0.2mm showing as 'light rain'

    // todo: relocate weather calls to the foreground service

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_info);

        if (!init(savedInstanceState))
            Log.e(TAG, "Failed to initialise weather for place info.");
    }

    private boolean init(Bundle savedInstanceState) {
        return initActivity() && initClickables() && initWeatherData(savedInstanceState);
    }

    private boolean initClickables() {
        findViewById(R.id.layPlaceInfoHeader).setOnClickListener(this::redirectToMapsActivity);
        return true;
    }

    private boolean initActivity() {
        mSharedPref = this.getSharedPreferences(
                PrefKeys.app_global_preferences, Context.MODE_PRIVATE);
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        return true;
    }

    private boolean initWeatherData(Bundle savedInstanceState) {
        // Initialise all weather data
        LatLng latLng = null;
        try {
            if (savedInstanceState == null) {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    Log.d(TAG, "PlaceInfoActivity identified parcelable extras.");
                    latLng = extras.getParcelable(RequestCodes.EXTRA_LATLNG);
                    mNearbyPlace = extras.getString(RequestCodes.EXTRA_PLACENAME);
                } else {
                    Log.d(TAG, "No parcelable extras bundled in call to PlaceInfoActivity.");
                }
            }
            if (latLng == null) {
                JSONObject lastWeather = new JSONObject(
                        mSharedPref.getString(PrefKeys.last_weather_json,
                                PrefConstValues.empty_json_object));
                latLng = new LatLng(lastWeather.getJSONObject("city").getJSONObject("coord")
                        .getDouble("lat"),
                        lastWeather.getJSONObject("city").getJSONObject("coord")
                                .getDouble("lon"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        // Call and await an update to the weather JSON string in shared prefs
        final boolean isMetric = PAWSAPI.preferredMetric(mSharedPref);
        if (!new WeatherHandler(this).awaitWeatherUpdate(this, latLng, isMetric)) {
            // Initialise weather displays with last best values if none are being updated
            return initWeatherDisplay(
                    latLng, mSharedPref.getString(
                            PrefKeys.last_weather_json, PrefConstValues.empty_json_object),
                    isMetric);
        }
        return true;
    }

    /**
     * Open up MapsActivity focusing on the current place.
     */
    private void redirectToMapsActivity(View view) {
        Intent intent = new Intent(this, MapsActivity.class)
                .putExtra(RequestCodes.EXTRA_LATLNG, mPlaceLatLng);
        startActivity(intent);
    }

    @Override
    public void onWeatherReceived(LatLng latLng, String response, boolean isMetric) {
        initWeatherDisplay(latLng, response, isMetric);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "Preference " + preference.getKey() + " changing to " + newValue);
        return true;
    }

    private boolean initWeatherDisplay(LatLng latLng, String response, boolean isMetric) {
        String str;
        Double dbl;

        // Update LatLng value for returning to MapsActivity with the weather data location
        mPlaceLatLng = latLng;

        final int elemsPerDay = 24 / 3;
        final int pad = Math.round(getResources().getDimension(R.dimen.app_spacing_medium));

        try {
            // Fetch the latest weather forecast for the provided location
            final JSONObject weatherForecastJSON = new JSONObject(response);
            final JSONObject currentWeatherJson = weatherForecastJSON.getJSONArray("list")
                    .getJSONObject(0);

            // Weather title -- City of forecast
            str = weatherForecastJSON.getJSONObject("city").getString("name");
            ((TextView)findViewById(R.id.txtWeatherCity)).setText(str);

            // Weather subtitle -- Nearby place from maps marker
            if (mNearbyPlace != null && !mNearbyPlace.equals("") && !mNearbyPlace.equals(str)) {
                ((TextView)findViewById(R.id.txtWeatherNearby)).setText(
                        getString(R.string.wa_nearby) + ' ' + mNearbyPlace);
                findViewById(R.id.txtWeatherNearby).setVisibility(View.VISIBLE);
            } else {
                ((TextView)findViewById(R.id.txtWeatherNearby)).setText("");
                findViewById(R.id.txtWeatherNearby).setVisibility(View.GONE);
            }

            // Weather description
            str = currentWeatherJson.getJSONArray("weather").getJSONObject(0)
                    .getString("description");
            ((TextView)findViewById(R.id.txtWeatherDescription)).setText(str);

            // Weather icon
            str = currentWeatherJson.getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            ((ImageView)findViewById(R.id.imgWeatherIcon)).setImageDrawable(
                    PAWSAPI.getWeatherDrawable(this, str));

            // Current temperature
            dbl = currentWeatherJson.getJSONObject("main").getDouble("temp");
            str = PAWSAPI.getTemperatureString(dbl, isMetric);
            ((TextView)findViewById(R.id.txtTempCurrent)).setText(str);

            // Current wind
            dbl = currentWeatherJson.getJSONObject("wind").getDouble("speed");
            str = PAWSAPI.getWindSpeedString(dbl, isMetric);
            ((TextView)findViewById(R.id.txtWindCurrent)).setText(str);

            // Current precipitation
            dbl = 0.0d;
            if (currentWeatherJson.has("rain"))
                if (currentWeatherJson.getJSONObject("rain").has("3h"))
                    dbl = currentWeatherJson.getJSONObject("rain").getDouble("3h");
            str = PAWSAPI.getPrecipitationString(isMetric, dbl);
            ((TextView)findViewById(R.id.txtPrecipCurrent)).setText(str);

            // Current humidity
            dbl = (double)currentWeatherJson.getJSONObject("main").getInt("humidity");
            str = PAWSAPI.getSimplePercentageString(dbl);
            ((TextView)findViewById(R.id.txtHumidityCurrent)).setText(str);

            /* Populate today's weather: */

            // todo: prevent today's weather from accumulating
            LinearLayout layParent = findViewById(R.id.layWeatherToday);
            for (int elem = 0; elem < elemsPerDay + 1; elem++) {
                final JSONObject periodicWeatherJson = weatherForecastJSON
                        .getJSONArray("list").getJSONObject(elem);

                // Create a new LinearLayout child
                LinearLayout.LayoutParams params;
                LinearLayout layout = new LinearLayout(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layout.setLayoutParams(params);
                layout.setOrientation(LinearLayout.VERTICAL);

                TextView txt;
                ImageView img;

                // Write the time of the forecast sample
                str = PAWSAPI.getShortClockString(
                        periodicWeatherJson.getLong("dt") * 1000);
                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_accent_alt));
                txt.setPadding(pad, 0, pad, 0);
                layout.addView(txt);

                // Create a weather icon
                str = periodicWeatherJson.getJSONArray("weather").getJSONObject(0)
                        .getString("icon");
                img = new ImageView(this);
                img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, str));
                params = new LinearLayout.LayoutParams(
                        Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                        Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                params.gravity = Gravity.CENTER;
                img.setLayoutParams(params);
                layout.addView(img);

                // Add any predicted precipitation
                int id = periodicWeatherJson.getJSONArray("weather").getJSONObject(0)
                        .getInt("id");
                if (id < 800) {
                    if (id > 100) {
                        // Rainy weather, measurements as periodic volume in millimetres
                        dbl = periodicWeatherJson.getJSONObject("rain").getDouble("3h");
                        str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                    }
                } else if (id == 800) {
                    // Clear weather, no notable measurements
                    str = "";
                } else {
                    // Cloudy weather, measurements in percentage coverage
                    dbl = periodicWeatherJson.getJSONObject("clouds").getDouble("all");
                    str = PAWSAPI.getSimplePercentageString(dbl);
                }

                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Tiny);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_on_background));
                layout.addView(txt);

                // Add the predicted temperature
                dbl = periodicWeatherJson.getJSONObject("main").getDouble("temp");
                str = PAWSAPI.getTemperatureString(dbl);
                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_accent_alt));
                layout.addView(txt);

                // Add the child to the hierarchy
                layParent.addView(layout);
            }

            /* Populate the 5-day forecast: */

            // todo: prevent the 5-day forecast from accumulating

            // todo: rewrite the 4 (!) different loops over the week in every iteration of this loop

            double tempHigh = weatherForecastJSON.getJSONArray("list").getJSONObject(elemsPerDay)
                            .getJSONObject("main")
                            .getDouble("temp");
            double tempLow = tempHigh;
            String icon = weatherForecastJSON.getJSONArray("list").getJSONObject(elemsPerDay)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            String icon2 = icon;
            int weatherId1 = weatherForecastJSON.getJSONArray("list").getJSONObject(elemsPerDay)
                    .getJSONArray("weather").getJSONObject(0)
                    .getInt("id");
            int weatherId2 = weatherId1;

            layParent = findViewById(R.id.layWeatherWeekly);
            final int elemCount = weatherForecastJSON.getJSONArray("list").length();
            for (int elem = elemsPerDay; elem < elemCount; ++elem) {
                final JSONObject periodicWeatherJson = weatherForecastJSON
                        .getJSONArray("list").getJSONObject(elem);

                if ((elem + 1) % elemsPerDay == 0) {
                    LinearLayout.LayoutParams params;

                    // Create a vertical divider between children
                    if (elem / elemsPerDay > 1) {
                        View view = new View(this);
                        params = new LinearLayout.LayoutParams(
                                1,
                                LinearLayout.LayoutParams.MATCH_PARENT);
                        view.setLayoutParams(params);
                        view.setBackgroundColor(ContextCompat.getColor(
                                this, R.color.color_primary));
                        view.setAlpha(0.75f);
                        layParent.addView(view);
                    }

                    // Create a new LinearLayout child
                    LinearLayout layout = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            250,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layout.setLayoutParams(params);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    TextView txt;
                    ImageView img;

                    // Write the day's title
                    str = DateFormat.format("E",
                            weatherForecastJSON.getJSONArray("list")
                                    .getJSONObject(elem - 8)
                                    .getLong("dt") * 1000).toString();
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    txt.setPadding(0, pad / 3, 0, pad);
                    layout.addView(txt);

                    // Create a new layout child for weather icons
                    LinearLayout layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layTemp.setLayoutParams(params);
                    layTemp.setOrientation(LinearLayout.HORIZONTAL);

                    // Create primary weather icon
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            1);
                    img.setLayoutParams(params);
                    layTemp.addView(img);

                    // Create secondary weather icon
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon2));
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            1);
                    img.setLayoutParams(params);
                    layTemp.addView(img);

                    // Add the icon layout to the child
                    layout.addView(layTemp);

                    // Create a new layout child for the daily temperature range
                    layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layTemp.setLayoutParams(params);
                    layTemp.setOrientation(LinearLayout.HORIZONTAL);

                    // Add the predicted high temperature
                    str = PAWSAPI.getTemperatureString(tempHigh);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1);
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_accent_alt));
                    layTemp.addView(txt);

                    // Add the predicted low temperature
                    str = PAWSAPI.getTemperatureString(tempLow);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1);
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    layTemp.addView(txt);

                    // Add the temperature layout to the child
                    layout.addView(layTemp);

                    // Create a wind bearing icon
                    dbl = 0d;
                    for (int i = elem - elemsPerDay; i < elem; i++)
                        dbl += weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                                .getJSONObject("wind")
                                .getDouble("deg");
                    dbl /= elemsPerDay;
                    img = new ImageView(this);
                    img.setImageDrawable(getDrawable(R.drawable.ic_navigation));
                    img.setColorFilter(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    img.setRotation(dbl.floatValue());
                    params = new LinearLayout.LayoutParams(
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_small)),
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_small)));
                    params.gravity = Gravity.CENTER;
                    params.topMargin = pad;
                    params.bottomMargin = pad;
                    img.setLayoutParams(params);
                    layout.addView(img);

                    // Add the predicted average wind speed
                    dbl = 0d;
                    for (int i = elem - elemsPerDay - 1; i < elem; i++)
                        dbl += weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                                .getJSONObject("wind").getDouble("speed");
                    str = PAWSAPI.getWindSpeedString(dbl / elemsPerDay, isMetric);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                    txt.setTextColor(ContextCompat.getColor(
                            this, R.color.color_on_background));
                    layout.addView(txt);

                    // Add any precipitation for the lowest-tier weather effects for the day
                    str = "";
                    if (weatherId1 < 800) {
                        if (weatherId1 > 100) {
                            // Rainy weather, measurements as daily total volume in millimetres
                            dbl = 0d;
                            for (int i = elem - elemsPerDay - 1; i < elem; i++) {
                                if (weatherForecastJSON.getJSONArray("list")
                                        .getJSONObject(i).has("rain")) {
                                    if (weatherForecastJSON.getJSONArray("list")
                                            .getJSONObject(i).getJSONObject("rain")
                                            .has("3h")) {
                                        Log.d(TAG,
                                                "Sampling rain/3h from element " + i + ". ("
                                                + weatherForecastJSON.getJSONArray("list")
                                                .getJSONObject(i).getJSONObject("rain")
                                                .getDouble("3h") + ")");
                                        dbl += weatherForecastJSON.getJSONArray("list")
                                                .getJSONObject(i)
                                                .getJSONObject("rain").getDouble("3h");
                                    }
                                }
                            }
                            if (dbl > 0d)
                                str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                        }
                    } else if (weatherId1 == 800) {
                        // Clear skies, no notable measurements
                    } else {
                        // Cloudy weather, measurements in percentage coverage
                        dbl = 0d;
                        for (int i = elem - elemsPerDay - 1; i < elem; i++) {
                            Log.d(TAG, "Sampling clouds/all from element " + i + ". ("
                                    + weatherForecastJSON.getJSONArray("list")
                                    .getJSONObject(i).getJSONObject("clouds")
                                    .getInt("all") + ")");
                            dbl += Double.parseDouble(
                                    weatherForecastJSON.getJSONArray("list")
                                            .getJSONObject(i).getJSONObject("clouds")
                                            .getString("all"));
                        }
                        str = PAWSAPI.getSimplePercentageString(dbl / elemsPerDay);
                    }

                    if (!(str.equals(""))) {
                        Log.d(TAG, "Adding additional weather data ("
                                + str + ") for day " + elem / elemsPerDay);

                        // Create a weather icon
                        img = new ImageView(this);
                        img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                        params = new LinearLayout.LayoutParams(
                                Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                                Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                        params.gravity = Gravity.CENTER;
                        params.topMargin = pad;
                        img.setLayoutParams(params);
                        layout.addView(img);

                        // Add the additional information
                        txt = new TextView(this);
                        params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.gravity = Gravity.CENTER;
                        txt.setLayoutParams(params);
                        txt.setText(str);
                        txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        txt.setTextAppearance(this, R.style.TextAppearance_Paws_Tiny);
                        txt.setTextColor(ContextCompat.getColor(
                                this, R.color.color_on_background));
                        layout.addView(txt);
                    }

                    // Add the daily weather child to the hierarchy
                    layParent.addView(layout);

                    // After each 24-hour cluster of samples, publish the data as a new day
                    double temp = periodicWeatherJson.getJSONObject("main").getDouble("temp");
                    tempHigh = temp;
                    tempLow = temp;
                }

                // Compare temperatures sampled per 3 hours to identify highs and lows for the day
                double temp = periodicWeatherJson.getJSONObject("main").getDouble("temp");
                Log.d(TAG, "Sampling temperature/3h from element "
                        + elem + ". (" + temp + ")");

                if (tempHigh < temp)
                    tempHigh = temp;
                if (tempLow > temp)
                    tempLow = temp;

                // Compare weather IDs to bring notable weather events to attention
                // Note:
                // Cloud > Clear = 800 > Atmospherics > Snow > Rain > Drizzle > Thunderstorm
                int elemWeatherId = periodicWeatherJson.getJSONArray("weather")
                        .getJSONObject(0).getInt("id");
                if (elemWeatherId < weatherId1) {
                    weatherId1 = elemWeatherId;
                    icon = periodicWeatherJson.getJSONArray("weather")
                            .getJSONObject(0).getString("icon");
                } else if (elemWeatherId < weatherId2) {
                    weatherId2 = elemWeatherId;
                    icon = periodicWeatherJson.getJSONArray("weather")
                            .getJSONObject(0).getString("icon");
                }
            }

            // Fill in other weather details

            // TODO : Guarantee sunrise/sunset uses local timezone rather than system timezone.

            // Sunrise and sunset
            str = PAWSAPI.getClockString(this,
                    weatherForecastJSON.getJSONObject("city").getLong("sunrise") * 1000,
                    true);
            ((TextView)findViewById(R.id.txtSunriseTime)).setText(str);

            str = PAWSAPI.getClockString(this,
                    weatherForecastJSON.getJSONObject("city").getLong("sunset") * 1000,
                    true);
            ((TextView)findViewById(R.id.txtSunsetTime)).setText(str);

            // Timestamp for current weather sample
            final int whichTime = PAWSAPI.getWeatherJsonIndexForTime(
                    weatherForecastJSON.getJSONArray("list"), System.currentTimeMillis());
            if (whichTime >= 0) {
                str = PAWSAPI.getWeatherTimestampString(this,
                        weatherForecastJSON.getJSONArray("list").getJSONObject(whichTime)
                                .getLong("dt") * 1000);
                ((TextView)findViewById(R.id.txtWeatherTimestamp)).setText(str);
            } else {
                Log.e(TAG, "Invalid or obsolete weather JSON.");
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

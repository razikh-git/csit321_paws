package com.amw188.csit321_paws;

import android.content.Context;
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

import java.text.DecimalFormat;

public class WeatherActivity
        extends
                BottomNavBarActivity
        implements
                WeatherHandler.WeatherReceivedListener,
                Preference.OnPreferenceChangeListener
{
    private static final String TAG = "snowpaws_wa";

    SharedPreferences mSharedPref;
    WeatherHandler mWeatherHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Load global preferences.
         mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Initialise all weather data.
        LatLng latLng = null;
        try {
            if (savedInstanceState == null) {
                Bundle extras = getIntent().getExtras();
                if (extras != null) {
                    Log.d(TAG, "WeatherActivity identified parcelable extras.");
                    latLng = extras.getParcelable(RequestCode.EXTRA_LATLNG);
                } else {
                    Log.d(TAG, "No parcelable extras were bundled in the call to WeatherActivity.");
                }
            }
            if (latLng == null) {
                JSONObject lastWeather = new JSONObject(
                        mSharedPref.getString("last_weather_json", "{}"));
                latLng = new LatLng(lastWeather.getJSONObject("city").getJSONObject("coord")
                        .getDouble("lat"),
                        lastWeather.getJSONObject("city").getJSONObject("coord")
                                .getDouble("lon"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (latLng != null) {
            // Call and await an update to the weather JSON string in prefs.
            boolean isMetric = mSharedPref.getString("units", "metric").equals("metric");
            mWeatherHandler = new WeatherHandler(this);
            if (!mWeatherHandler.updateWeather(this, latLng, isMetric)) {
                // Initialise weather displays with last best values if none are being updated.
                initWeatherDisplay(
                        latLng,
                        mSharedPref.getString("last_weather_json", "{}"),
                        isMetric);
            }
        }
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
        long lon;

        final int index = 8;
        final int pad = Math.round(getResources().getDimension(R.dimen.text_spacing));

        try {
            // Fetch the latest weather forecast for the provided location.
            JSONObject weatherForecastJSON = new JSONObject(response);

            // Weather title
            str = weatherForecastJSON.getJSONObject("city")
                    .getString("name");
            ((TextView)findViewById(R.id.txtWeatherCity)).setText(str);

            // Weather description
            str = weatherForecastJSON.getJSONArray("list").getJSONObject(0)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("description");
            ((TextView)findViewById(R.id.txtWeatherDescription)).setText(str);

            // Weather icon
            str = weatherForecastJSON.getJSONArray("list").getJSONObject(0)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            ((ImageView)findViewById(R.id.imgWeatherIcon)).setImageDrawable(
                    PAWSAPI.getWeatherDrawable(this, str));

            // Current temperature
            dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(0)
                    .getJSONObject("main")
                    .getDouble("temp");
            str = PAWSAPI.getTemperatureString(isMetric, dbl, true);

            ((TextView)findViewById(R.id.txtTempCurrent)).setText(str);

            // Populate today's weather.
            LinearLayout layParent = findViewById(R.id.layWeatherToday);
            for (int i = 0; i < index + 1; i++) {
                // Create a new LinearLayout child.
                LinearLayout.LayoutParams params;
                LinearLayout layout = new LinearLayout(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                layout.setLayoutParams(params);
                layout.setOrientation(LinearLayout.VERTICAL);

                TextView txt;
                ImageView img;

                // Write the time of the forecast sample.
                str = DateFormat.format("h a", weatherForecastJSON.getJSONArray("list").getJSONObject(i)
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
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_accent_alt));
                txt.setPadding(pad, 0, pad, 0);
                layout.addView(txt);

                // Create a weather icon.
                str = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                        .getJSONArray("weather").getJSONObject(0)
                        .getString("icon");
                img = new ImageView(this);
                img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, str));
                params = new LinearLayout.LayoutParams(
                        Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                        Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                params.gravity = Gravity.CENTER;
                img.setLayoutParams(params);
                layout.addView(img);

                // Add any predicted precipitation.
                int id = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                        .getJSONArray("weather").getJSONObject(0)
                        .getInt("id");
                if (id < 800) {
                    if (id > 100) {
                        // Rainy weather, measurements as periodic volume in millimetres.
                        dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                                .getJSONObject("rain").getDouble("3h");
                        str = PAWSAPI.getPrecipitationString(isMetric, dbl);

                    }
                } else if (id == 800) {
                    // Clear weather, no notable measurements.
                    str = "";
                } else {
                    // Cloudy weather, measurements in percentage coverage.
                    dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONObject("clouds").getDouble("all");
                    str = 10 * Math.round(dbl / 10) + "%";
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

                // Add the predicted temperature.
                dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                        .getJSONObject("main")
                        .getDouble("temp");
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

                // Add the child to the hierarchy.
                layParent.addView(layout);
            }

            // Populate the 5-day forecast.
            double tempHigh = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                            .getJSONObject("main")
                            .getDouble("temp");
            double tempLow = tempHigh;
            String icon = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            String icon2 = icon;
            int id = weatherForecastJSON.getJSONArray("list").getJSONObject(index)
                    .getJSONArray("weather").getJSONObject(0)
                    .getInt("id");
            int id2 = id;

            layParent = findViewById(R.id.layWeatherWeekly);
            for (int i = index; i < 40; ++i) {
                if ((i + 1) % 8 == 0) {
                    LinearLayout.LayoutParams params;

                    // Create a vertical divider between children.
                    if (i / 8 > 1) {
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

                    // Create a new LinearLayout child.
                    LinearLayout layout = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            250,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layout.setLayoutParams(params);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    TextView txt;
                    ImageView img;

                    // Write the day's title.
                    lon = weatherForecastJSON.getJSONArray("list").getJSONObject(i - 8)
                            .getLong("dt") * 1000;
                    str = new DateFormat().format("E", lon).toString();
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Medium);
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_on_background));
                    txt.setPadding(0, pad / 3, 0, pad);
                    layout.addView(txt);

                    // Create a new layout child for weather icons.
                    LinearLayout layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layTemp.setLayoutParams(params);
                    layTemp.setOrientation(LinearLayout.HORIZONTAL);

                    // Create primary weather icon.
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            1);
                    img.setLayoutParams(params);
                    layTemp.addView(img);

                    // Create secondary weather icon.
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon2));
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            1);
                    img.setLayoutParams(params);
                    layTemp.addView(img);

                    // Add the icon layout to the child.
                    layout.addView(layTemp);

                    // Create a new layout child for the daily temperature range.
                    layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layTemp.setLayoutParams(params);
                    layTemp.setOrientation(LinearLayout.HORIZONTAL);

                    // Add the predicted high temperature.
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
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_accent_alt));
                    layTemp.addView(txt);

                    // Add the predicted low temperature.
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
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_on_background));
                    layTemp.addView(txt);

                    // Add the temperature layout to the child.
                    layout.addView(layTemp);

                    // Create a wind bearing icon.
                    dbl = 0d;
                    for (int j = i - 8; j < i; j++)
                        dbl += weatherForecastJSON.getJSONArray("list").getJSONObject(j)
                                .getJSONObject("wind")
                                .getDouble("deg");
                    dbl /= 8;
                    img = new ImageView(this);
                    img.setImageDrawable(getDrawable(R.drawable.ic_navigation));
                    img.setColorFilter(ContextCompat.getColor(this, R.color.color_on_background));
                    img.setRotation(dbl.floatValue());
                    params = new LinearLayout.LayoutParams(
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_small)),
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_small)));
                    params.gravity = Gravity.CENTER;
                    params.topMargin = pad;
                    params.bottomMargin = pad;
                    img.setLayoutParams(params);
                    layout.addView(img);

                    // Add the predicted average wind speed.
                    dbl = 0d;
                    for (int j = i - 7; j < i; j++)
                        dbl += weatherForecastJSON.getJSONArray("list").getJSONObject(j)
                                .getJSONObject("wind")
                                .getDouble("speed");
                    str = PAWSAPI.getWindSpeedString(isMetric, dbl / 8);
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_on_background));
                    layout.addView(txt);

                    // Add any precipitation for the lowest-tier weather effects for the day.
                    str = "";
                    if (id < 800) {
                        if (id > 100) {
                            // Rainy weather, measurements as daily total volume in millimetres.
                            dbl = 0d;
                            for (int j = i - 7; j < i; j++) {
                                if (weatherForecastJSON.getJSONArray("list").getJSONObject(j)
                                        .has("rain")) {
                                    if (weatherForecastJSON.getJSONArray("list").getJSONObject(j)
                                            .getJSONObject("rain").has("3h")) {
                                        Log.println(Log.DEBUG, "snowpaws_weather",
                                                "Sampling rain/3h from element " + j + ". ("
                                                        + weatherForecastJSON.getJSONArray("list").getJSONObject(j).getJSONObject("rain").getDouble("3h") + ")");
                                        dbl += weatherForecastJSON.getJSONArray("list").getJSONObject(j)
                                                .getJSONObject("rain").getDouble("3h");
                                    }

                                }
                            }
                            if (dbl > 0d)
                                str = PAWSAPI.getPrecipitationString(isMetric, dbl);
                        }
                    } else if (id == 800) {
                        // Clear skies, no notable measurements.
                    } else {
                        // Cloudy weather, measurements in percentage coverage.
                        dbl = 0d;
                        for (int j = i - 7; j < i; j++) {
                            Log.println(Log.DEBUG, "snowpaws_weather",
                                    "Sampling clouds/all from element " + j + ". ("
                                            + weatherForecastJSON.getJSONArray("list").getJSONObject(j).getJSONObject("clouds").getInt("all") + ")");
                            dbl += Double.parseDouble(weatherForecastJSON.getJSONArray("list").getJSONObject(j)
                                    .getJSONObject("clouds").getString("all"));
                        }
                        dbl /= 8;
                        str = 10 * Math.round(dbl / 10) + "%";
                    }

                    if (!(str.equals(""))) {
                        Log.println(Log.DEBUG, "snowpaws_weather",
                                "Adding additional weather data (" + str + ") for day " + i / 8);

                        // Create a weather icon.
                        img = new ImageView(this);
                        img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                        params = new LinearLayout.LayoutParams(
                                Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                                Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                        params.gravity = Gravity.CENTER;
                        params.topMargin = pad;
                        img.setLayoutParams(params);
                        layout.addView(img);

                        // Add the additional information.
                        txt = new TextView(this);
                        params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.gravity = Gravity.CENTER;
                        txt.setLayoutParams(params);
                        txt.setText(str);
                        txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        txt.setTextAppearance(this, R.style.TextAppearance_Paws_Tiny);
                        txt.setTextColor(ContextCompat.getColor(this, R.color.color_on_background));
                        layout.addView(txt);
                    }

                    // Add the daily weather child to the hierarchy.
                    layParent.addView(layout);

                    // After each 24-hour cluster of samples, publish the data as a new day.
                    double temp = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONObject("main")
                            .getDouble("temp");
                    tempHigh = temp;
                    tempLow = temp;
                }

                // Compare temperatures sampled every 3 hours to identify highs and lows for the day.
                double temp = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                        .getJSONObject("main")
                        .getDouble("temp");

                if (tempHigh < temp)
                    tempHigh = temp;
                if (tempLow > temp)
                    tempLow = temp;

                // Compare weather IDs to bring notable weather events to attention.
                // Note:
                // Cloud > Clear = 800 > Atmospherics > Snow > Rain > Drizzle > Thunderstorm
                int idTemp = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                        .getJSONArray("weather").getJSONObject(0)
                        .getInt("id");
                if (idTemp < id) {
                    id = idTemp;
                    icon = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONArray("weather").getJSONObject(0)
                            .getString("icon");
                } else if (idTemp < id2) {
                    id2 = idTemp;
                    icon = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONArray("weather").getJSONObject(0)
                            .getString("icon");
                }
            }


            // Fill in other weather details.

            // TODO : Guarantee sunrise/sunset uses local timezone rather than system timezone.

            // Sunrise and sunset
            str = (DateFormat.format("h:mm a",
                    weatherForecastJSON.getJSONObject("city").getLong("sunrise") * 1000)).toString();
            ((TextView)findViewById(R.id.txtSunrise)).setText(str);

            str = (DateFormat.format("h:mm a",
                    weatherForecastJSON.getJSONObject("city").getLong("sunset") * 1000)).toString();
            ((TextView)findViewById(R.id.txtSunset)).setText(str);

        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}

package com.example.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.Constraints;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;

public class WeatherActivity extends BottomNavBarActivity {

    SharedPreferences mSharedPref;
    SharedPreferences.Editor mSharedEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        // Load global preferences.
         mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
         mSharedEditor = mSharedPref.edit();

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // . . .

        initWeatherDisplay();
    }

    private boolean initWeatherDisplay() {
        double lat = Double.parseDouble(getResources().getString(R.string.app_default_loc_lat));
        double lng = Double.parseDouble(getResources().getString(R.string.app_default_loc_lng));

        try {
            PAWSAPI.updateLatestWeatherForecast(this, lat, lng);
            JSONObject weatherForecastJSON = new JSONObject(mSharedPref.getString("last_weather_json", "{}"));

            String str;
            Double dbl;
            Long lon;

            // Weather title
            str = weatherForecastJSON.getJSONObject("city").getString("name");
            ((TextView)findViewById(R.id.txtWeatherCity)).setText(str);

            // Weather icon
            str = weatherForecastJSON.getJSONArray("list").getJSONObject(0)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            ((ImageView)findViewById(R.id.imgWeatherIcon)).setImageDrawable(
                    PAWSAPI.getWeatherDrawable(this, str));

            // Current temperature
            str = weatherForecastJSON.getJSONArray("list").getJSONObject(0)
                    .getJSONObject("main")
                    .getString("temp");
            str += mSharedPref.getString("units", "metric").equals("metric") ?
                    "°C" : "°F";

            // Populate today's weather.
            LinearLayout layParent = findViewById(R.id.layWeatherToday);
            for (int i = 0; i < 8; i++) {
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

                int pad = Math.round(getResources().getDimension(R.dimen.text_spacing));

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
                        str = new DecimalFormat("#").format(dbl) + "mm";

                    } else {
                        // Clear weather, no notable measurements.
                        str = "";
                    }
                } else {
                    // Cloudy weather, measurements in percentage coverage.
                    dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONObject("clouds").getDouble("all");
                    str = new DecimalFormat("#").format(dbl) + "%";
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
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_black));
                txt.setPadding(pad, 0, pad, 0);
                layout.addView(txt);

                // Add the predicted temperature.
                dbl = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                        .getJSONObject("main")
                        .getDouble("temp");
                str = new DecimalFormat("#").format(dbl) + "°";
                txt = new TextView(this);
                params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                txt.setLayoutParams(params);
                txt.setText(str);
                txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                txt.setTextColor(ContextCompat.getColor(this, R.color.color_primary_dark));
                txt.setPadding(pad, 0, pad, 0);
                layout.addView(txt);

                // Add the child to the hierarchy.
                layParent.addView(layout);
            }

            // Populate the 5-day forecast.
            double tempHigh = weatherForecastJSON.getJSONArray("list").getJSONObject(8)
                            .getJSONObject("main")
                            .getDouble("temp");
            double tempLow = tempHigh;
            String icon = weatherForecastJSON.getJSONArray("list").getJSONObject(8)
                    .getJSONArray("weather").getJSONObject(0)
                    .getString("icon");
            int id = weatherForecastJSON.getJSONArray("list").getJSONObject(8)
                    .getJSONArray("weather").getJSONObject(0)
                    .getInt("id");

            layParent = findViewById(R.id.layWeatherWeekly);
            for (int i = 8; i < 40; i++) {
                // TODO resolve first element obviously being wrong
                if (i % 8 == 0) {
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

                    int pad = Math.round(getResources().getDimension(R.dimen.text_spacing));

                    // Write the day's title.
                    lon = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
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
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_black));
                    txt.setPadding(pad, 0, pad, 0);
                    layout.addView(txt);

                    // TODO customise the icon based on importance (ie. ID > 100)
                    // todo it doesnt work probably

                    // Create a weather icon.
                    img = new ImageView(this);
                    img.setImageDrawable(PAWSAPI.getWeatherDrawable(this, icon));
                    params = new LinearLayout.LayoutParams(
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)),
                            Math.round(getResources().getDimension(R.dimen.dimen_icon_medium)));
                    params.gravity = Gravity.CENTER;
                    img.setLayoutParams(params);
                    layout.addView(img);

                    // Create a new layout child for temperatures.
                    LinearLayout layTemp = new LinearLayout(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    layout.setLayoutParams(params);
                    layout.setOrientation(LinearLayout.VERTICAL);

                    // Add the predicted high temperature.
                    str = new DecimalFormat("#").format(tempHigh) + "°";
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_primary_dark));
                    txt.setPadding(pad, 0, pad, 0);
                    layout.addView(txt);

                    // Add the predicted low temperature.
                    str = new DecimalFormat("#").format(tempLow) + "°";
                    txt = new TextView(this);
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.gravity = Gravity.CENTER;
                    txt.setLayoutParams(params);
                    txt.setText(str);
                    txt.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                    txt.setTextAppearance(this, R.style.TextAppearance_Paws_Small);
                    txt.setTextColor(ContextCompat.getColor(this, R.color.color_grey));
                    txt.setPadding(pad, 0, pad, 0);
                    layout.addView(txt);

                    // Add the child to the hierarchy.
                    layParent.addView(layout);

                    // After each 24-hour cluster of samples, publish the data as a new day.
                    double temp = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONObject("main")
                            .getDouble("temp");
                    tempHigh = temp;
                    tempLow = temp;

                } else {
                    // Compare temperatures sampled every 3 hours to identify highs and lows for the day.
                    double temp = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONObject("main").getDouble("temp");

                    if (tempHigh < temp)
                        tempHigh = temp;
                    else if (tempLow > temp)
                        tempLow = temp;

                    // Compare weather IDs to bring notable weather events to attention.
                    // Note:
                    // Cloud > Clear = 800 > Atmospherics > Snow > Rain > Drizzle > Thunderstorm
                    int id2 = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                            .getJSONArray("weather").getJSONObject(0)
                            .getInt("id");
                    if (id2 < id) {
                        id = id2;
                        icon = weatherForecastJSON.getJSONArray("list").getJSONObject(i)
                                .getJSONArray("weather").getJSONObject(0)
                                .getString("icon");
                    }
                }
            }


            // Fill in other weather details.

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

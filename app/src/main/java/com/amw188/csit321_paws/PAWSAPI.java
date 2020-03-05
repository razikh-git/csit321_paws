package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

final class PAWSAPI {
    private PAWSAPI() {}

    private static final String TAG = "snowpaws_api";

    private static double toKilometresPerHour(double ms) { return ms * 3.6d; }
    private static double toMilesPerHour(double ms) { return ms * 2.237d; }
    private static double toInches(double mm) { return mm / 25.4d; }

    static double metresToMiles(double m) { return m * 0.62d; }

    static String getDistanceString(boolean isMetric, double m) {
    	return (isMetric ? m / 1000.0f : metresToMiles(m)) + (isMetric ? "km" : "mi");
	}

    /**
     * Formats a string for some precipitation depth in appropriate units.
     * @param isMetric Value conversion and units to display for metric or non-metric preferences.
     * @param mm Value to show and/or convert.
     * @return Formatted string.
     */
    static String getPrecipitationString(boolean isMetric, double mm) {
            return isMetric
                ? new DecimalFormat("#.##").format(mm) + "mm"
                : new DecimalFormat("#.##").format(toInches(mm)) + "in";
    }

    /**
     * Formats a string for some wind speed in appropriate units.
     * @param isMetric Value conversion and units to display for metric or non-metric preferences.
     * @param speed Value to show and convert.
     * @return Formatted string.
     */
    static String getWindSpeedString(double speed, boolean isMetric) {
        return isMetric
                ? new DecimalFormat("#").format(toKilometresPerHour(speed)) + " km/h"
                : new DecimalFormat("#").format(toMilesPerHour(speed)) + " mph";
    }

    /**
     * Fetches a cardinal direction for some angle in degrees.
     * @param bearing Wind bearing value in degrees.
     * @return Cardinal direction string.
     */
    static String getWindBearingString(double bearing) {
        String str = "north";
        if (bearing < 135)
            str = "west";
        else if (bearing < 225)
            str = "south";
        else if (bearing < 315)
            str = "east";
        return str;
    }

    /**
     * Fetches a cardinal direction for some angle in degrees.
     * @param bearing Wind bearing value in degrees.
     * @param isVerbose Whether to return the full name or an abbreviation.
     * @return Cardinal direction string.
     */
    static String getWindBearingString(double bearing, boolean isVerbose) {
        String str = getWindBearingString(bearing);
        return isVerbose ? str : str.substring(0, 1).toUpperCase();
    }

    /**
     * Formats a string for some temperature without units.
     * @param temperature Value to show.
     * @return Formatted string.
     */
    static String getTemperatureString(double temperature) {
        return new DecimalFormat("#").format(temperature) + "°";
    }

    /**
     * Formats a string for some temperature with appropriate units.
     * @param temperature Value to show.
     * @return Formatted string.
     */
    static String getTemperatureString(double temperature, boolean isMetric) {
        return new DecimalFormat("#").format(temperature) + (isMetric ? "°C" : "°F");
    }

    static int getWeatherIconId(String icon) {
        int id = -1;
        switch (icon) {
            case "01":
                id = R.drawable.w01;
                break;
            case "01d":
                id = R.drawable.w01d;
                break;
            case "01n":
                id = R.drawable.w01n;
                break;
            case "02d":
                id = R.drawable.w02d;
                break;
            case "02n":
                id = R.drawable.w02n;
                break;
            case "03d":
                id = R.drawable.w03d;
                break;
            case "03n":
                id = R.drawable.w03n;
                break;
            case "04d":
                id = R.drawable.w04d;
                break;
            case "04n":
                id = R.drawable.w04n;
                break;
            case "9d":
                id = R.drawable.w09d;
                break;
            case "09n":
                id = R.drawable.w09n;
                break;
            case "10d":
                id = R.drawable.w10d;
                break;
            case "10n":
                id = R.drawable.w10n;
                break;
            case "11d":
                id = R.drawable.w11d;
                break;
            case "11n":
                id = R.drawable.w11n;
                break;
            case "13d":
                id = R.drawable.w13d;
                break;
            case "13n":
                id = R.drawable.w13n;
                break;
            case "50d":
                id = R.drawable.w50d;
                break;
            case "50n":
                id = R.drawable.w50n;
                break;
        }
        return id;
    }

    /**
     * Fetches a bitmap image for a weather icon code provided by OpenWeatherMaps.
     * @param ctx Context.
     * @param icon OWM icon code.
     * @return Drawable for weather icon code.
     */
    static Bitmap getWeatherBitmap(Context ctx, String icon) {
        return BitmapFactory.decodeResource(ctx.getResources(),
                getWeatherIconId(icon));
    }

    /**
     * Fetches a drawable for a weather icon code provided by OpenWeatherMaps.
     * @param ctx Context.
     * @param icon OWM icon code.
     * @return Drawable for weather icon code.
     */
    static Drawable getWeatherDrawable(Context ctx, String icon) {
        return ctx.getDrawable(getWeatherIconId(icon));
    }

    /**
     * Value conversion rounding milliseconds down to decimal hours.
     * Replace with DateFormat.format("h") ?
     * @param milliseconds Duration to round down.
     * @return Decimal hour value.
     */
    static double msToHours(long milliseconds) {
        final double hours = (double) milliseconds / 1000 / 60;
        return (hours / 60) % hours;
    }

    /**
     * Value conversion fetching minutes from an hour's decimal remainder.
     * Replace with DateFormat.format("m") ?
     * @param hour Hour with minutes as a floating point remainder.
     * @return Minutes extra to the hour.
     */
    static int minuteOfHour(double hour) { return (int)((hour - (int)Math.floor(hour)) * 60); }

    /**
     * Gives the time in milliseconds, to the nearest minute, from some starting time
     * until the next given hour and minute on a 24 hour clock.
     * @param from Starting instance in time.
     * @param hour Hour of the day.
     * @param minute Minute of the hour.
     * @return Milliseconds between starting time and the next instance of this hour and minute.
     */
    static long getTimeUntil(long from, int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(from);
        return (hour - calendar.get(Calendar.HOUR_OF_DAY)) * 1000 * 60 * 60
                + (minute - calendar.get(Calendar.MINUTE)) * 1000 * 60;
    }

    /**
     * Compiles the temperature values from a range of weather samples between some starting time
     * until the next day begins at midnight.
     * @param startTime Starting instance in time.
     * @param weatherJSON JSON array containing a list of weather samples to iterate through.
     * @return ArrayList containing all temperature samples for the day.
     */
    static ArrayList<Double> getDailyTemperatures(long startTime, JSONArray weatherJSON) {
        Log.d(TAG, "Fetching new daily temperatures.");
        ArrayList<Double> tempList = new ArrayList<>();
        try {
            // Fetches the temperatures at each 3hr interval until the next day
            for (int i = 0; i < weatherJSON.length(); ++i) {
                final long sampleTime = weatherJSON.getJSONObject(i).getLong("dt") * 1000;
                if (sampleTime >= startTime) {
                    Log.d(TAG, "Fetching...");
                    int elemsPerDay = (int)Math.floor((24 - msToHours(startTime)) / 3);
                    if (elemsPerDay < 1)
                        elemsPerDay = 24 / 3;
                    final int end = Math.min(weatherJSON.length(), i + elemsPerDay);
                    for (int j = i; j < end; ++j)
                        tempList.add(weatherJSON.getJSONObject(j)
                                .getJSONObject("main")
                                .getDouble("temp"));
                    break;
                }
            }
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to read weather JSON.");
            ex.printStackTrace();
        }
        return tempList;
    }

    static void resetProfileData(Context context) {
        // Load global preferences
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getResources().getString(
                        R.string.app_global_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();
        sharedEditor.putBoolean("app_init", false);
        sharedEditor.putBoolean("facebook_init", false);

        // Reset all survey profile data
        final int end = context.getResources().getInteger(R.integer.survey_question_count);
        for (int i = 0; i < end; ++i)
            sharedEditor.putInt("survey_answer_" + i, 0);
        sharedEditor.putInt("survey_last_question", 0);
        sharedEditor.putLong("survey_time_completed", 0);
        sharedEditor.apply();

        Toast.makeText(context,
                R.string.app_reset_result,
                Toast.LENGTH_LONG)
                .show();
    }
}

package com.amw188.csit321_paws;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;

final class PAWSAPI {
    private PAWSAPI() {}

    private static final String TAG = "snowpaws_pawsapi";

    private static double toKilometresPerHour(double ms) { return ms * 3.6d; }
    private static double toMilesPerHour(double ms) { return ms * 2.237d; }
    private static double toInches(double mm) { return mm / 25.4; }

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
     * @param velocity Value to show and convert.
     * @return Formatted string.
     */
    static String getWindSpeedString(boolean isMetric, double velocity) {
        return isMetric
                ? new DecimalFormat("#").format(toKilometresPerHour(velocity)) + " km/h"
                : new DecimalFormat("#").format(toMilesPerHour(velocity)) + " mph";
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
     * Fetches a drawable for a weather icon code provided by OpenWeatherMaps.
     * @param ctx Context.
     * @param icon OWM icon code.
     * @return Drawable for weather icon code.
     */
    static Drawable getWeatherDrawable(Context ctx, String icon) {
        switch (icon) {
            case "01":
                return ctx.getDrawable(R.drawable.w01);
            case "01d":
                return ctx.getDrawable(R.drawable.w01d);
            case "01n":
                return ctx.getDrawable(R.drawable.w01n);
            case "02d":
                return ctx.getDrawable(R.drawable.w02d);
            case "02n":
                return ctx.getDrawable(R.drawable.w02n);
            case "03d":
                return ctx.getDrawable(R.drawable.w03d);
            case "03n":
                return ctx.getDrawable(R.drawable.w03n);
            case "04d":
                return ctx.getDrawable(R.drawable.w04d);
            case "04n":
                return ctx.getDrawable(R.drawable.w04n);
            case "9d":
                return ctx.getDrawable(R.drawable.w09d);
            case "09n":
                return ctx.getDrawable(R.drawable.w09n);
            case "10d":
                return ctx.getDrawable(R.drawable.w10d);
            case "10n":
                return ctx.getDrawable(R.drawable.w10n);
            case "11d":
                return ctx.getDrawable(R.drawable.w11d);
            case "11n":
                return ctx.getDrawable(R.drawable.w11n);
            case "13d":
                return ctx.getDrawable(R.drawable.w13d);
            case "13n":
                return ctx.getDrawable(R.drawable.w13n);
            case "50d":
                return ctx.getDrawable(R.drawable.w50d);
            case "50n":
                return ctx.getDrawable(R.drawable.w50n);
            default:
                return null;
        }
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
                        tempList.add(weatherJSON.getJSONObject(i)
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
}

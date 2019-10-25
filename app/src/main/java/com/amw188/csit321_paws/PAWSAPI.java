package com.amw188.csit321_paws;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.text.DecimalFormat;

final class PAWSAPI {
    private PAWSAPI() {}

    private static double toKilometresPerHour(double ms) { return ms * 3.6d; }
    private static double toMilesPerHour(double ms) { return ms * 2.237d; }
    private static double toInches(double mm) { return mm / 25.4; }

    static String getPrecipitationString(boolean isMetric, double mm) {
            return isMetric
                ? new DecimalFormat("#.##").format(mm) + "mm"
                : new DecimalFormat("#.##").format(toInches(mm)) + "in";
    }

    static String getWindSpeedString(boolean isMetric, double velocity) {
        return isMetric
                ? new DecimalFormat("#").format(toKilometresPerHour(velocity)) + " km/h"
                : new DecimalFormat("#").format(toMilesPerHour(velocity)) + " mph";
    }

    static String getTemperatureString(double temperature) {
        return new DecimalFormat("#").format(temperature) + "°";
    }

    static String getTemperatureString(boolean isMetric, double temperature, boolean isVerbose) {
        if (!isVerbose)
            return getTemperatureString(temperature);
        else
            return isMetric
                    ? new DecimalFormat("#").format(temperature) + "°C"
                    : new DecimalFormat("#").format(temperature) + "°F";
    }

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
}

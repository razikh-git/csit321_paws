package com.amw188.csit321_paws;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class DailyWeatherWorker extends Worker {
    private static final String TAG = PrefConstValues.tag_prefix + "dww";

    private static final int WEATHER_ID = 1338;
    private static final String WEATHER_TAG = "paws_weather_channel";
    static final String WORK_TAG = PrefConstValues.package_name + ".daily_weather_work";
    static final String WEATHER_CHANNEL_ID = "paws_weather_channel";

    private Context mContext;
    private SharedPreferences mSharedPref;

    private long mLastDailySample;
    private ArrayList<Double> mDailyTemps;

    public DailyWeatherWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override @NonNull
    public Result doWork() {
		Log.d(TAG, "in doWork()");

        Result result = Result.success();
        mContext = getApplicationContext();
        mSharedPref = mContext.getSharedPreferences(
                PrefKeys.app_global_preferences, Context.MODE_PRIVATE);

        final long timeNow = System.currentTimeMillis();
        final long timeUntilStart = PAWSAPI.getTimeUntil(
                timeNow, Integer.parseInt(getStartTime()[0]), Integer.parseInt(getStartTime()[1]));
        final long timeUntilEnd = PAWSAPI.getTimeUntil(
                timeNow, Integer.parseInt(getEndTime()[0]), Integer.parseInt(getEndTime()[1]));

        Log.d(TAG, "Time until: (start=" + timeUntilStart + " end=" + timeUntilEnd);

        if (timeUntilStart < 0 && timeUntilEnd > 0) {
            Log.d(TAG, "Within hourly bounds for sending notification.");
            result = pushWeatherNotification();
        } else {
            Log.d(TAG, "Was not in hourly bounds for sending notification.");
        }

        return result;
    }

    /**
     * Fetches and then posts a weather notification.
     * @return Operation success.
     */
    private Result pushWeatherNotification() {
        final NotificationManager manager = (NotificationManager) mContext.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Notification manager failed to initialise.");
            return Result.failure();
        }

        final Notification notif = getWeatherNotification();
        if (notif == null)
            return Result.failure();
        manager.notify(WEATHER_TAG, WEATHER_ID, notif);
        return Result.success();
    }

    /**
     * Creates a notification containing relevant weather info for some location.
     * @return Assembled weather notification.
     */
    private Notification getWeatherNotification() {
        // debug code
        try {
        	final JSONObject debugJSON = new JSONObject(
					mSharedPref.getString(PrefKeys.last_weather_json, PrefConstValues.empty_json))
					.getJSONObject("lat_lng");
			final LatLng latLng = new LatLng(debugJSON.getDouble("latitude"),
					debugJSON.getDouble("longitude"));
			Log.d(TAG, "URL: " + OpenWeatherMapIntegration.getOWMURL(
			        mContext, latLng, true));
		} catch (JSONException ex) {
        	Log.e(TAG, "couldnt get URL, we blew it");
		}
		// debug code

        // todo: include weather information local to some place
        // store recent and favourite places to refer to

        // todo: add tide/swell information

        // todo: mention whether there's a chance of rain later?

        final String notifTitle = mContext.getString(R.string.notif_title_weather);
        String weatherTitle = "%s at %s";
        String weatherMessage = "%s and %s"
                + "\n\nWinds are %s %s."
                + "\nDaily high and low temperatures of %s to %s."
                + "\nCurrently feels like %s.";
        Bitmap weatherIcon;

        try {
            // Fetch the last best weather forecast from storage
            final JSONObject weatherJson = new JSONObject(mSharedPref.getString(
                    PrefKeys.last_weather_json, PrefConstValues.empty_json));

            // Choose the next best forecast 3hr time block
            final long now = System.currentTimeMillis();
            final int whichTime = PAWSAPI.getWeatherJsonIndexForTime(
                    weatherJson.getJSONArray("list"), now);
            if (whichTime < 0) {
                Log.e(TAG, "Invalid or obsolete weather JSON.");
                return null;
            }
            final JSONObject timeJSON = weatherJson.getJSONArray("list")
                    .getJSONObject(whichTime);

            // Update the daily weather values when the day passes
            // todo: ensure this is called for all common interval settings and notice time ranges
            if (mDailyTemps == null || now - mLastDailySample >= 1000 * 60 * 60 * 24) {
                final long startTime = mDailyTemps != null
                        ? now + PAWSAPI.getTimeUntil(now, 0, 0)
                        : now;
                mDailyTemps = PAWSAPI.getDailyTemperatures(
                        startTime, weatherJson.getJSONArray("list"));
                mLastDailySample = now;
            }

            // Set the notification icon for the coming weather conditions
            weatherIcon = PAWSAPI.getWeatherBitmap(mContext,
                    timeJSON.getJSONArray("weather").getJSONObject(0)
                    .getString("icon"));

            // Format the notification with the coming weather data
            final boolean isMetric = mSharedPref.getString(PrefKeys.units, PrefDefValues.units)
                    .equals(PrefConstValues.units_metric);
            weatherTitle = String.format(Locale.getDefault(), weatherTitle,
                    weatherJson.getJSONObject("city").getString("name"),
                    DateFormat.format(
                            "h a", timeJSON.getLong("dt") * 1000));
            weatherMessage = String.format(Locale.getDefault(), weatherMessage,

                    // Weather summary
                    PAWSAPI.getTemperatureString(
                            timeJSON.getJSONObject("main").getDouble("temp"), isMetric),
                    timeJSON.getJSONArray("weather").getJSONObject(0)
                            .getString("description"),

                    // Wind data
                    PAWSAPI.getWindSpeedString(
                            timeJSON.getJSONObject("wind").getDouble("speed"), isMetric),
                    PAWSAPI.getWindBearingString(mContext,
                            timeJSON.getJSONObject("wind").getDouble("deg"), true),

                    // Temperature range
                    PAWSAPI.getTemperatureString(
                            Collections.max(mDailyTemps)),
                    PAWSAPI.getTemperatureString(
                            Collections.min(mDailyTemps)),
                    PAWSAPI.getTemperatureString(
                            timeJSON.getJSONObject("main").getDouble("feels_like"), isMetric));

            Log.d(TAG, "Finished building notification.");
        } catch (JSONException ex) {
            Log.e(TAG, "Failed to initialise weather JSON object.");
            ex.printStackTrace();
            return null;
        }

        // Assemble and build the notification
        PendingIntent contentIntent = PendingIntent.getActivity(
                mContext, 0, new Intent(mContext, PlaceInfoActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                mContext, WEATHER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_paws_icon)
                .setContentIntent(contentIntent)
                .setContentTitle(weatherTitle)
                .setContentText(weatherMessage.split("\n")[0])
                .setLargeIcon(weatherIcon)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(weatherTitle)
                        .setSummaryText(notifTitle)
                        .bigText(weatherMessage))
                .setPriority(Notification.PRIORITY_DEFAULT);
        return builder.build();
    }

    private String[] getStartTime() {
        return mSharedPref.getString(PrefKeys.weather_notif_time_start,
                PrefDefValues.weather_notif_time_start)
                .split(":");
    }

    private String[] getEndTime() {
        return mSharedPref.getString(PrefKeys.weather_notif_time_end,
                PrefDefValues.weather_notif_time_end)
                .split(":");
    }
}

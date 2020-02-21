package com.amw188.csit321_paws;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class DailyWeatherWorker extends Worker {
    private static final String TAG = "snowpaws_dww";

    private static final String PACKAGE_NAME = "com.amw188.csit321_paws";
    private static final int WEATHER_ID = 1338;
    private static final String WEATHER_TAG = "paws_weather_channel";
    static final String WORK_TAG = PACKAGE_NAME + ".daily_weather_work";
    static final String WEATHER_CHANNEL_ID = "paws_weather_channel";

    public DailyWeatherWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        Log.d(TAG, "Doing work");

        Result result = Result.success();
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getResources().getString(R.string.app_global_preferences),
                Context.MODE_PRIVATE);
        String[] startTime = sharedPref.getString("weather_notification_time_start",
                context.getResources().getString(R.string.app_default_weather_notif_time_start))
                .split(":");
        String[] endTime = sharedPref.getString("weather_notification_time_end",
                context.getResources().getString(R.string.app_default_weather_notif_time_end))
                .split(":");
        long timeNow = System.currentTimeMillis();
        long timeUntilStart = PAWSAPI.getTimeUntil(
                timeNow, Long.parseLong(startTime[0]), Long.parseLong(startTime[1]));
        long timeUntilEnd = PAWSAPI.getTimeUntil(
                timeNow, Long.parseLong(endTime[0]), Long.parseLong(endTime[1]));

        Log.d(TAG, "Time until: (start=" + timeUntilStart + " end=" + timeUntilEnd);

        if (timeUntilStart < 0 && timeUntilEnd > 0) {
            Log.d(TAG, "Within hourly bounds for sending notification.");
            result = pushWeatherNotification(context);
        }

        return result;
    }

    /**
     * Fetches and then posts a weather notification.
     * @return Operation success.
     */
    private Result pushWeatherNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Notification manager failed to initialise.");
            return Result.failure();
        }

        manager.notify(WEATHER_TAG, WEATHER_ID, getWeatherNotification(context));
        return Result.success();
    }

    /**
     * Creates a notification containing relevant weather info for some location.
     * @return Populated notification object.
     */
    private Notification getWeatherNotification(Context context) {
        // todo: include weather information local to some place

        PendingIntent contentIntent = PendingIntent.getActivity(
                context, 0, new Intent(context, WeatherActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, WEATHER_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_paws_icon)
                .setContentIntent(contentIntent)
                .setContentText("CONTENT TEXT, HELLO HELLO")
                .setContentTitle("Weather over {AREA}: {0}")
                .setPriority(Notification.PRIORITY_DEFAULT);
        return builder.build();
    }
}

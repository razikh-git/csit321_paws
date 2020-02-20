package com.amw188.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.mainAppTheme);
        super.onCreate(savedInstanceState);

        // Initialise config for image downloader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // todo: be aware: notification service instantiated here

        // Initilise notifications subsystem
        Context context = getApplicationContext();
        Intent intent = new Intent(context, NotificationService.class);
        context.startService(intent);

        // Load global preferences
        SharedPreferences sharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Handle first-time launches
        if (sharedPref.getBoolean("app_init", false)) {
            // Proceed straight to the home screen
            enterHome();
        } else {
            // Initialise all preferences to defaults
            sharedEditor.putInt("survey_last_question", 0);
            sharedEditor.putLong("survey_time_completed", 0);
            sharedEditor.putLong("selfanalysis_time_completed", 0);
            sharedEditor.putBoolean("facebook_init", false);
            sharedEditor.putBoolean("app_init", true);
            sharedEditor.putBoolean("weather_notifications_allowed", true);
            sharedEditor.putString("weather_notification_time_start", getResources().getString(
                    R.string.app_default_weather_notif_time_start));
            sharedEditor.putString("weather_notification_time_end", getResources().getString(
                    R.string.app_default_weather_notif_time_end));
            sharedEditor.putInt("weather_notification_interval", getResources().getInteger(
                    R.integer.app_default_weather_notif_hours_interval));
            sharedEditor.apply();

            // Display a prompt for the user to begin profiling
            enterProfilingPrompt();
        }
    }

    private void enterProfilingPrompt() {
        // Redirect to user profiling prompt and  information screen
        Intent intent = new Intent(this, ProfilingPromptActivity.class);
        startActivity(intent);
    }

    private void enterHome() {
        // Redirect to app landing screen
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}

package com.amw188.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
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
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Handle first-time launches
        if (sharedPref.getBoolean(PrefKeys.app_init, PrefDefValues.app_init)) {
            // Proceed straight to the home screen
            enterHome();
        } else {
            // Initialise all preferences to defaults
            PAWSAPI.resetAppData(this);
            sharedEditor.putBoolean(PrefKeys.app_init, true);
            sharedEditor.apply();

            // Display a prompt for the user to begin profiling
            enterProfilingPrompt();
        }
    }

    private void enterProfilingPrompt() {
        // Redirect to user profiling prompt and  information screen
        Intent intent = new Intent(this, SurveyInfosheetActivity.class);
        startActivity(intent);
        finish();
    }

    private void enterHome() {
        // Redirect to app landing screen
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
		finish();
    }
}

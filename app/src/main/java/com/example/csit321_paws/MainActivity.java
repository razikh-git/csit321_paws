package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    // TODO : preload data and assets in this activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.mainAppTheme);
        super.onCreate(savedInstanceState);

        // Initialise config for image downloader.
        File cacheDir = StorageUtils.getCacheDirectory(this);
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // Setup shared preferences.
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Handle first-time launches.
        if (sharedPref.contains("app_init")) {
            if (sharedPref.getBoolean("app_init", false)) {
                // Proceed straight to the home screen.
                enterHome();
            } else {
                // Initialise all preferences.
                sharedEditor.putBoolean("profile_init", false);
                sharedEditor.putBoolean("facebook_init", false);
                sharedEditor.putInt("profile_last_question", 0);
                sharedEditor.putLong("profile_time_completed", 0);
                sharedEditor.putString("units", "metric");
                sharedEditor.putString("lang", getResources().getConfiguration().locale.getDisplayLanguage());
            }
        }

        // Pass first-time flag after initialisation.
        //sharedEditor.putBoolean("app_init", true); // TODO : remove comment to allow skipping the profiling prompt
        sharedEditor.apply();

        // Display a prompt for the user to begin profiling.
        enterProfilingPrompt();
    }

    private void enterProfilingPrompt() {
        // Redirect to user profiling prompt and  information screen.
        Intent intent = new Intent(this, ProfilingPromptActivity.class);
        startActivity(intent);
    }

    private void enterHome() {
        // Redirect to app landing screen.
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }
}

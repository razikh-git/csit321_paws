package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    // TODO : preload data and assets in this activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.mainAppTheme);
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Handle first-time launches.
        if (sharedPref.contains("app_init")) {
            if (sharedPref.getBoolean("app_init", false)) {
                // Proceed straight to the home screen.
                enterHome();
            } else {
                // Initialise all preferences.
                sharedEditor.putString("units", "metric");
                sharedEditor.putString("lang", getResources().getConfiguration().locale.getDisplayLanguage());
            }
        }

        // Pass first-time flag after initialisation.
        sharedEditor.putBoolean("app_init", true);
        //sharedEditor.apply(); // TODO : remove comment to allow skipping the profiling prompt

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

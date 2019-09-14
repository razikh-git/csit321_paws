package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    // TODO : preload data and assets in this activity

    static final boolean isProfileInitialised = false; // TODO integrate with binary load

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.mainAppTheme);
        super.onCreate(savedInstanceState);

        // Display profiling prompt screen if required.
        if (isProfileInitialised)
            enterHome();
        else
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

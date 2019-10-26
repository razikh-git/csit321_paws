package com.amw188.csit321_paws;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends BottomNavBarActivity {

    static final String KEY_PREF_LOCATION_PRIORITY = "location_priority";
    static final String KEY_PREF_LOCATION_RATE = "location_rate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        // Button functionality.
        findViewById(R.id.btnReset).setOnClickListener((view) -> onClickReset(view));

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    public void onClickReset(View view) {
        // Load global preferences.
        SharedPreferences sharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();
        sharedEditor.putBoolean("app_init", false);
        sharedEditor.putBoolean("facebook_init", false);

        // Reset all survey profile data.
        for (int i = 0; i < getResources().getInteger(R.integer.survey_question_count); ++i)
            sharedEditor.putInt("survey_answer_" + i, 0);
        sharedEditor.putInt("survey_last_question", 0);
        sharedEditor.putLong("profile_time_completed", 0);
        sharedEditor.apply();

        Toast.makeText(this,
                R.string.app_reset_result,
                Toast.LENGTH_LONG).show();
    }
}
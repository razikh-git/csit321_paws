package com.amw188.csit321_paws;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends BottomNavBarActivity {

    static final String KEY_PREF_LOCATION_PRIORITY = "location_usage";
    static final String KEY_PREF_LOCATION_RATE = "location_rate";

    // TODO : Implement reactive reloading of content in other activities upon settings changed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

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
}
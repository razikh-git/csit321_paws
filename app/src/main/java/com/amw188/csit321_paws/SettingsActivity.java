package com.amw188.csit321_paws;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class SettingsActivity extends BottomNavBarActivity {
    private static final String TAG = "snowpaws_settings";

    private static final String PACKAGE_NAME = "com.amw188.csit321_paws";

    static final String TIME_START_EXTRA = PACKAGE_NAME + ".extra.TIME_START";
    static final String TIME_END_EXTRA = PACKAGE_NAME + ".extra.TIME_END";

    static final String KEY_PREF_LOCATION_PRIORITY = "location_priority";
    static final String KEY_PREF_LOCATION_RATE = "location_rate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.app_global_preferences), MODE_PRIVATE);

        // Button functionality.
        findViewById(R.id.btnReset).setOnClickListener((view) -> onClickReset(view));

        // Bottom navigation bar functionality.
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
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

            SharedPreferences sharedPref = getContext().getSharedPreferences(
                    getString(R.string.app_global_preferences), MODE_PRIVATE);

            // todo: add listpreference picker of notification frequency to arrays / stringperfs

            // Update time picker preference interactions
            Preference pref;

            pref = findPreference("notif_time_start");
            pref.setOnPreferenceClickListener(this::onClickNotifTime);
            pref.setTitle(sharedPref.getString("weather_notif_time_start",
                    getString(R.string.app_default_weather_notif_time_start)));

            pref = findPreference("notif_time_end");
            pref.setOnPreferenceClickListener(this::onClickNotifTime);
            pref.setTitle(sharedPref.getString("weather_notif_time_end",
                    getString(R.string.app_default_weather_notif_time_end)));

        }

        private boolean onClickNotifTime(Preference pref) {
            Calendar now = Calendar.getInstance();
            final int hour = now.get(Calendar.HOUR_OF_DAY);
            final int minute = now.get(Calendar.MINUTE);
            final String key = pref.getKey();
            SharedPreferences sharedPref = getContext().getSharedPreferences(
                    getString(R.string.app_global_preferences), MODE_PRIVATE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                    (timePicker, dialogHour, dialogMinute) -> {
                String timeStr = dialogHour + ":" + dialogMinute;
                Context context = getContext();
                SharedPreferences.Editor sharedEditor = sharedPref.edit();
                String prefKey = key.equals("notif_time_start")
                        ? "weather_notif_time_start"
                        : "weather_notif_time_end";
                sharedEditor.putString(prefKey, timeStr);
                sharedEditor.apply();

                // todo: bind manager service, message to reschedule weather notifications

                Toast.makeText(getContext(),
                        "Time: " + timeStr + " -- " + prefKey,
                        Toast.LENGTH_LONG)
                        .show();
                }, hour, minute, false);
            timePickerDialog.setTitle(
                    key.equals("notif_time_start")
                            ? sharedPref.getString("weather_notif_time_start",
                            getString(R.string.app_default_weather_notif_time_start))
                            : sharedPref.getString("weather_notif_time_start",
                            getString(R.string.app_default_weather_notif_time_end)));
            timePickerDialog.show();

            return true;
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
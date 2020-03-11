package com.amw188.csit321_paws;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class SettingsActivity
		extends BottomNavBarActivity
		implements
		Preference.OnPreferenceChangeListener,
		ServiceHandler.ConnectionListener {

    private static final String TAG = PrefConstValues.tag_prefix + "a_settings";

    private ServiceHandler mServiceHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
		if (!init()) {
			Log.e(TAG, "Failed to initialise preferences.");
		}
    }

    private boolean init() {
		findViewById(R.id.btnReset).setOnClickListener(this::onClickReset);
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

		// Bind to the notification service
		mServiceHandler = new ServiceHandler(this);

		return true;
	}

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            SharedPreferences sharedPref = getContext().getSharedPreferences(
            		PrefKeys.app_global_preferences, MODE_PRIVATE);

            // todo: add listpreference picker of notification frequency to arrays / stringperfs

			// Update time picker preference display and interactions
			Preference pref;
			String[] str;
			Calendar now = Calendar.getInstance();

			pref = findPreference("notif_time_heading");
			pref.setEnabled(false);

            pref = findPreference("notif_time_start");
            pref.setOnPreferenceClickListener(this::onClickNotifTimePreference);
            str = sharedPref.getString(PrefKeys.weather_notif_time_start,
					PrefDefValues.weather_notif_time_start)
					.split(":");
			now.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str[0]));
			now.set(Calendar.MINUTE, Integer.parseInt(str[1]));
			pref.setTitle(PAWSAPI.getClockString(getContext(),
					now.getTimeInMillis(), true));

            pref = findPreference("notif_time_end");
            pref.setOnPreferenceClickListener(this::onClickNotifTimePreference);
			str = sharedPref.getString(PrefKeys.weather_notif_time_end,
					PrefDefValues.weather_notif_time_end)
					.split(":");
			now.set(Calendar.HOUR_OF_DAY, Integer.parseInt(str[0]));
			now.set(Calendar.MINUTE, Integer.parseInt(str[1]));
			pref.setTitle(PAWSAPI.getClockString(getContext(),
					now.getTimeInMillis(), true));
        }

        private boolean onClickNotifTimePreference(Preference pref) {
            SharedPreferences sharedPref = getContext().getSharedPreferences(
                    PrefKeys.app_global_preferences, MODE_PRIVATE);

            final boolean isStartTimePref = pref.getKey().equals("notif_time_start");

			String[] str = (isStartTimePref
					? sharedPref.getString(PrefKeys.weather_notif_time_start,
					PrefDefValues.weather_notif_time_start)
					: sharedPref.getString(PrefKeys.weather_notif_time_end,
					PrefDefValues.weather_notif_time_end))
					.split(":");

            TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                    (timePicker, dialogHour, dialogMinute) ->
					{
						/* TimePicker completed action */

						// Set the notification time bounds display on complete
						Calendar now = Calendar.getInstance();
						now.set(Calendar.HOUR_OF_DAY, dialogHour);
						now.set(Calendar.MINUTE, dialogMinute);
						final String timeStr = dialogHour + ":" + dialogMinute;
						final String prefKey = isStartTimePref
								? PrefKeys.weather_notif_time_start
								: PrefKeys.weather_notif_time_end;
						pref.setTitle(DateFormat.format("hh:mm a", now));

						// Push changes to shared preferences
						SharedPreferences.Editor sharedEditor = sharedPref.edit();
						sharedEditor.putString(prefKey, timeStr);
						sharedEditor.apply();

						// todo: bind manager service, message to reschedule weather notifications

						/* TimePicker completed action */
					},
					// Use current existing preferences as default for the time picker
					Integer.parseInt(str[0]),
					Integer.parseInt(str[1]),
					false);

            timePickerDialog.setTitle(isStartTimePref
					? getString(R.string.pref_title_notif_picker_start)
					: getString(R.string.pref_title_notif_picker_end));
            timePickerDialog.show();
            return true;
        }
    }

    public void onClickReset(View view) {
    	PAWSAPI.resetProfileData(this);
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
    	Log.d(TAG, "in onPreferenceChange");
    	if (preference.getKey().equals(PrefKeys.location_rate))
    		if (mServiceHandler.service().rescheduleNotifications())
    			Log.d(TAG, "Rescheduled notifications.");
    		else
    			Log.d(TAG, "No notifications were scheduled already.");
		return true;
	}

	@Override
	public void onServiceConnected(ComponentName className, IBinder service) {
		Log.d(TAG, "in onServiceConnected()");

	}

	@Override
	public void onServiceDisconnected(ComponentName arg0) {
		Log.d(TAG, "in onServiceDisconnected()");
	}

	@Override
	protected void onStart() {
		super.onStart();
		// Bind to the notification service
		mServiceHandler.bind(this);
	}

	@Override
	protected void onStop() {
		// Unbind from the notification service
		mServiceHandler.unbind(this);
		super.onStop();
	}

}
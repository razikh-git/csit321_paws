package com.amw188.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PlaceHistoryActivity extends BottomNavBarActivity {

	private static final String TAG = PrefConstValues.tag_prefix + "pha";

	private SharedPreferences mSharedPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_place_history);

		initActivity();
	}

	private boolean initActivity() {
		mSharedPref = this.getSharedPreferences(
				PrefKeys.app_global_preferences, Context.MODE_PRIVATE);
		BottomNavigationView nav = findViewById(R.id.bottomNavigation);
		nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
		return true;
	}

}

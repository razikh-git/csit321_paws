package com.amw188.csit321_paws;

import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class FacebookEntryActivity extends BottomNavBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facebook_entry);

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

    }
}

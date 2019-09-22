package com.example.csit321_paws;

import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BottomNavBarActivity extends AppCompatActivity {

    protected BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navHome:
                    onClickHome();
                case R.id.navMaps:
                    onClickMaps();
                case R.id.navWeather:
                    onClickWeather();
            }
            return true;
        }
    };

    public void onClickHome() {
        // Redirect to home screen.
        this.finish();
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void onClickMaps() {
        // Redirect to maps screen.

        // . . .
    }

    public void onClickWeather() {
        // Redirect to home screen.

        // . . .
    }
}

package com.amw188.csit321_paws;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BottomNavBarActivity extends AppCompatActivity {

    protected BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = ((item) -> {
            switch (item.getItemId()) {
                case R.id.navHome:
                    onClickHome();
                    return true;
                case R.id.navMaps:
                    onClickMaps();
                    return true;
                case R.id.navWeather:
                    onClickWeather();
                    return true;
                case R.id.navReturn:
                    onClickReturn();
                    return true;
                default:
                    return false;
            }
        });

    public void onClickHome() {
        // Redirect to home screen.
        this.finish();
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void onClickMaps() {
        // Redirect to maps screen.
        this.finish();
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    public void onClickWeather() {
        // Redirect to home screen.
        this.finish();
        Intent intent = new Intent(this, PlaceInfoActivity.class);
        startActivity(intent);
    }

    public void onClickReturn() {
        // Drop out of the current screen.
        this.finish();
    }
}

package com.amw188.csit321_paws;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BottomNavBarActivity extends AppCompatActivity {

    protected BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = ((item) -> {
            switch (item.getItemId()) {
                case R.id.navHome:
                    return onClickHome();
                case R.id.navMaps:
                    return onClickMaps();
                case R.id.navPlaces:
                    return onClickPlaces();
                //case R.id.navWeather:
                    //return onClickWeather();
                case R.id.navReturn:
                    return onClickReturn();
                default:
                    return false;
            }
        });

    public boolean onClickHome() {
        // Redirect to home screen.
        finish();
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        return true;
    }

    public boolean onClickMaps() {
        // Redirect to maps screen.
        finish();
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        return true;
    }

    public boolean onClickPlaces() {
        // Redirect to place history screen.
        finish();
        Intent intent = new Intent(this, PlaceHistoryActivity.class);
        startActivity(intent);
        return true;
    }

    public boolean onClickWeather() {
        // Redirect to home screen.
        finish();
        Intent intent = new Intent(this, PlaceInfoActivity.class);
        startActivity(intent);
        return true;
    }

    public boolean onClickReturn() {
        // Drop out of the current screen.
        finish();
        return true;
    }
}

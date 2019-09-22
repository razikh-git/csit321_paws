package com.example.csit321_paws;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.InputStream;

public class ProfilingMenuActivity extends BottomNavBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profilingmenu);

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Button functionality.
        findViewById(R.id.btnSurvey).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickSurvey(view); }
        });
        findViewById(R.id.btnFacebook).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickFacebook(view); }
        });

        // TODO : integrate binary files to allow for contextual highlighting of survey cards

        // Initialise info screen contents.
        TextView txt = findViewById(R.id.txtInfo);
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.slp_info);
            byte[] b = new byte[in.available()];
            in.read(b);

            txt.setText(new String(b));
        } catch (Exception e){
            e.printStackTrace();
            txt.setText(R.string.app_txt_fallback);
        }
    }

    public void onClickSurvey(View view){
        // Redirect to survey intro screen.
        Intent intent = new Intent(this, SurveyEntryActivity.class);
        startActivity(intent);
    }

    public void onClickFacebook(View view){
        // Redirect to facebook intro screen.
        //Intent intent = new Intent(this, ProfilingMenuActivity.class);
        //startActivity(intent);
    }
}

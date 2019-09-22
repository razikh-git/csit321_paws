package com.example.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;

public class SurveyEntryActivity extends BottomNavBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_entry);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Button functionality.
        findViewById(R.id.btnContinue).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickContinue(view); }
        });

        // Initialise info screen contents.
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.se_info);
            byte[] b = new byte[in.available()];
            in.read(b);
            ((TextView)findViewById(R.id.txtInfo)).setText(new String(b));

        } catch (Exception e){
            e.printStackTrace();
            ((TextView)findViewById(R.id.txtInfo)).setText(R.string.app_txt_fallback);
        }

        // Initialise data summary contents.

        // Populate progress counter label.
        ((TextView)findViewById(R.id.txtProgressSurvey)).setText(
                sharedPref.getInt("profile_last_question", 0)
                        + " / " + getResources().getInteger(R.integer.survey_question_count)
        );

        if (sharedPref.getBoolean("profile_init", false) == false) {
            // Incomplete profiling surveys:

            if (sharedPref.getInt("profile_last_question", 0) == 0) {
                // Profiling surveys with no progress whatsoever:

                // Show zero progress icon.
                findViewById(R.id.imgProgressIcon).setVisibility(View.VISIBLE);

            } else {
                // Incomplete profiling surveys with some progress:

                // Show progress bar for survey completion percentage.
                ProgressBar progressBar = findViewById(R.id.barProgressSurvey);
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(
                        sharedPref.getInt("profile_last_question", 0)
                        / getResources().getInteger(R.integer.survey_question_count)
                        * 10
                );
            }

            // Change card style.
            ((MaterialCardView)findViewById(R.id.cardSurveyResults)).setStrokeColor(
                    getResources().getColor(R.color.color_error_dark));

        } else {
            // Completed profiling surveys:

            // Enable onClick events to redirect to the additional details screen.
            findViewById(R.id.cardSurveyResults).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            onClickDetails(view);
                        }
                    }
            );

            // Change card style.
            ((MaterialCardView)findViewById(R.id.cardSurveyResults)).setStrokeColor(
                    getResources().getColor(R.color.color_primary_light));

            // Show full progress icon.
            findViewById(R.id.imgProgressIcon).setVisibility(View.VISIBLE);
            ((ImageView)findViewById(R.id.imgProgressIcon)).setImageDrawable(
                    getDrawable(R.drawable.ic_radiobutton_checked)
            );

            // Survey completion timestamp.
            ((TextView)findViewById(R.id.txtTimestamp)).setText(
                    DateFormat.format("HH:mm dd/MM/YY",
                            sharedPref.getLong("profile_time_completed", 0)));
        }
    }

    public void onClickDetails(View view) {
        // Redirect to post-completion details summary screen.
        //Intent intent = new Intent(this, SurveyResultsActivity.class);
        //startActivity(intent);
    }

    public void onClickContinue(View view) {
        // Redirect to live survey screen.
        Intent intent = new Intent(this, SurveyQuestionActivity.class);
        startActivity(intent);
    }
}

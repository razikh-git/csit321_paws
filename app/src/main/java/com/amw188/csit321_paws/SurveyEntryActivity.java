package com.amw188.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

import java.io.InputStream;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SurveyEntryActivity
        extends BottomNavBarActivity {

    private static final String TAG = PrefConstValues.tag_prefix + "se";

    private SharedPreferences mSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_entry);
        if (!init()) {
            Toast.makeText(this,
                    "Failed to load the survey launchpad.",
                    Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    private boolean init() {
        return initActivity() && initInterface() && initButtons();
    }

    private boolean initActivity() {
        mSharedPref = this.getSharedPreferences(
                PrefKeys.app_global_preferences, Context.MODE_PRIVATE);

        // Bottom navigation bar functionality
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Initialise info screen contents
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.se_info);
            byte[] b = new byte[in.available()];
            in.read(b);
            ((TextView)findViewById(R.id.txtInfo)).setText(new String(b));
            ((TextView)findViewById(R.id.txtInfo)).setMovementMethod(new ScrollingMovementMethod());

        } catch (Exception e){
            e.printStackTrace();
            ((TextView)findViewById(R.id.txtInfo)).setText(R.string.app_txt_fallback);
            return false;
        }
        return true;
    }

    private boolean initButtons() {
        // Button functionality
        try {
            findViewById(R.id.btnContinue).setOnClickListener(this::onClickContinue);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean initInterface() {
        Log.d(TAG, PrefKeys.app_init + " : " + mSharedPref.getBoolean(
                PrefKeys.app_init, PrefDefValues.app_init));
        Log.d(TAG, PrefKeys.survey_last_question + " : " + mSharedPref.getInt(
                PrefKeys.survey_last_question, 0));
        Log.d(TAG, PrefKeys.survey_time_completed + " : " + mSharedPref.getLong(
                PrefKeys.survey_time_completed, 0));
        Log.d(TAG, PrefKeys.units + " : " + mSharedPref.getString(
                PrefKeys.units, PrefDefValues.units));

        // Initialise data summary contents:
        final int surveyQuestionCount = PAWSAPI.getSurveyQuestionCount(this);

        // Populate progress counter label
        ((TextView)findViewById(R.id.txtProgressSurvey)).setText(
                mSharedPref.getInt(PrefKeys.survey_last_question, 0)
                        + " / " + surveyQuestionCount);

        if (mSharedPref.getInt(PrefKeys.survey_last_question, 0) < surveyQuestionCount) {
            // Incomplete profiling surveys:

            if (mSharedPref.getInt(PrefKeys.survey_last_question, 0) == 0) {
                // Profiling surveys with no progress whatsoever:

                // Relabel the survey entry button
                ((MaterialButton)findViewById(R.id.btnContinue)).setText(
                        getResources().getString(R.string.app_btn_start));

                // Hide the survey results view
                findViewById(R.id.cardSurveyResults).setVisibility(GONE);

            } else {
                // Incomplete profiling surveys with some progress:

                // Relabel the survey entry button
                ((MaterialButton)findViewById(R.id.btnContinue)).setText(
                        getResources().getString(R.string.app_btn_continue));

                // Show progress bar for survey completion percentage
                findViewById(R.id.layProgressContainer).setVisibility(VISIBLE);
                ProgressBar progressBar = findViewById(R.id.barProgressSurvey);
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(VISIBLE);
                progressBar.setProgress(
                        (int)((double)(mSharedPref.getInt(PrefKeys.survey_last_question, 1)
                            / (double)surveyQuestionCount
                            * 100)));
            }

            // Change card style
            ((MaterialCardView)findViewById(R.id.cardSurveyResults)).setStrokeColor(
                    getResources().getColor(R.color.color_error));

        } else {
            // Completed profiling surveys:

            // Enable onClick events to redirect to the additional details screen
            findViewById(R.id.cardSurveyResults).setOnClickListener(this::onClickDetails);

            // Change card style
            ((MaterialCardView)findViewById(R.id.cardSurveyResults)).setStrokeColor(
                    getResources().getColor(R.color.color_primary));

            // Show full progress icon
            findViewById(R.id.imgProgressIcon).setVisibility(VISIBLE);
            ((ImageView)findViewById(R.id.imgProgressIcon)).setImageDrawable(
                    getDrawable(R.drawable.ic_checkbox_checked));
            ((ImageView)findViewById(R.id.imgProgressIcon)).setImageTintList(
                    ContextCompat.getColorStateList(this, R.color.success_colors));

            // Survey completion timestamp
            findViewById(R.id.txtTimestamp).setVisibility(VISIBLE);
            ((TextView)findViewById(R.id.txtTimestamp)).setText(
                    PAWSAPI.getDateTimestampString(this,
                            mSharedPref.getLong(PrefKeys.survey_time_completed, 0),
                            true));

            // Relabel the survey entry button
            ((MaterialButton)findViewById(R.id.btnContinue)).setText(
                    getResources().getString(R.string.app_btn_restart));
        }

        return true;
    }

    public void onClickDetails(View view) {
        // Redirect to post-completion details summary screen
        //Intent intent = new Intent(this, SurveyResultsActivity.class);
        //startActivity(intent);
    }

    public void onClickContinue(View view) {
        // todo: provide dialog prompt for restarting over with a completed profile

        // Reset profile data
        if (mSharedPref.getLong(PrefKeys.survey_time_completed, 0) > 0)
            PAWSAPI.resetProfileData(this);

        // Redirect to live survey screen
        Intent intent = new Intent(this, SurveyQuestionActivity.class);
        startActivityForResult(intent, RequestCode.SURVEY_CONTINUE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        initInterface();
    }
}

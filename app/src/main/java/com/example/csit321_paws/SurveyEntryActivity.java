package com.example.csit321_paws;

import android.app.Activity;
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

import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class SurveyEntryActivity extends BottomNavBarActivity {

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_entry);

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Initialise info screen contents.
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
        }

        // Initialise interface elements.
        initInterface();
        initButtons();
    }

    private boolean initButtons() {
        // Button functionality.
        try {
            findViewById(R.id.btnContinue).setOnClickListener((view) -> onClickContinue(view));
            findViewById(R.id.btnReset).setOnClickListener((view) -> onClickReset(view));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean initInterface() {
        Log.println(Log.DEBUG, "snowpaws_surveyentry",
                "app_init : " + mSharedPref.getBoolean("app_init", false));
        Log.println(Log.DEBUG, "snowpaws_surveyentry",
                "facebook_init : " + mSharedPref.getBoolean("facebook_init", false));
        Log.println(Log.DEBUG, "snowpaws_surveyentry",
                "survey_last_question : " + mSharedPref.getInt("survey_last_question", -1));
        Log.println(Log.DEBUG, "snowpaws_surveyentry",
                "profile_time_completed : " + mSharedPref.getLong("profile_time_completed", -1));
        Log.println(Log.DEBUG, "snowpaws_surveyentry",
                "units : " + mSharedPref.getString("units", "metric"));

        // Initialise data summary contents:

        // Populate progress counter label.
        ((TextView)findViewById(R.id.txtProgressSurvey)).setText(
                mSharedPref.getInt("survey_last_question", 0)
                        + " / " + getResources().getInteger(R.integer.survey_question_count));
        TextViewCompat.setTextAppearance(
                findViewById(R.id.txtProgressSurvey), R.style.TextAppearance_Paws_Medium);

        if (mSharedPref.getInt("survey_last_question", 1)
                < getResources().getInteger(R.integer.survey_question_count)) {

            // Incomplete profiling surveys:

            if (mSharedPref.getInt("survey_last_question", 1) == 1) {
                // Profiling surveys with no progress whatsoever:

                // Relabel the survey entry button.
                ((MaterialButton)findViewById(R.id.btnContinue)).setText(
                        getResources().getString(R.string.app_btn_start));

                // Hide the progress bar.
                findViewById(R.id.barProgressSurvey).setVisibility(GONE);

                // Show warning graphic.
                findViewById(R.id.imgProgressIcon).setVisibility(VISIBLE);
                ((ImageView)findViewById(R.id.imgProgressIcon)).setImageDrawable(
                        getDrawable(R.drawable.ic_error));
                ((ImageView)findViewById(R.id.imgProgressIcon)).setImageTintList(
                        ContextCompat.getColorStateList(this, R.color.error_colors));

                // Display fallback text.
                ((TextView)findViewById(R.id.txtProgressSurvey)).setText(R.string.se_fallback);
                TextViewCompat.setTextAppearance(
                        findViewById(R.id.txtProgressSurvey), R.style.TextAppearance_Paws_Caption);

                // Hide timestamp label.
                findViewById(R.id.txtTimestamp).setVisibility(GONE);

                // Hide survey details label.
                findViewById(R.id.txtSurveyResultsLabel).setVisibility(GONE);

            } else {
                // Incomplete profiling surveys with some progress:

                // Relabel the survey entry button.
                ((MaterialButton)findViewById(R.id.btnContinue)).setText(
                        getResources().getString(R.string.app_btn_continue));

                // Replace timestamp label with a notice.
                findViewById(R.id.txtTimestamp).setVisibility(VISIBLE);
                TextViewCompat.setTextAppearance(
                        findViewById(R.id.txtProgressSurvey), R.style.TextAppearance_Paws_Large);
                ((TextView)findViewById(R.id.txtTimestamp)).setText(
                        getResources().getString(R.string.se_in_progress));

                // Show progress bar for survey completion percentage.
                ProgressBar progressBar = findViewById(R.id.barProgressSurvey);
                progressBar.setIndeterminate(false);
                progressBar.setVisibility(VISIBLE);
                progressBar.setProgress(
                        (int)((double)(mSharedPref.getInt("survey_last_question", 1)
                            / (double)getResources().getInteger(R.integer.survey_question_count)
                            * 100)));
            }

            // Change card style.
            ((MaterialCardView)findViewById(R.id.cardSurveyResults)).setStrokeColor(
                    getResources().getColor(R.color.color_error));

        } else {

            // Completed profiling surveys:

            // Enable onClick events to redirect to the additional details screen.
            findViewById(R.id.cardSurveyResults).setOnClickListener((view) -> onClickDetails(view));

            // Change card style.
            ((MaterialCardView)findViewById(R.id.cardSurveyResults)).setStrokeColor(
                    getResources().getColor(R.color.color_primary_light));

            // Show full progress icon.
            findViewById(R.id.imgProgressIcon).setVisibility(VISIBLE);
            ((ImageView)findViewById(R.id.imgProgressIcon)).setImageDrawable(
                    getDrawable(R.drawable.ic_checkbox_checked));
            ((ImageView)findViewById(R.id.imgProgressIcon)).setImageTintList(
                    ContextCompat.getColorStateList(this, R.color.success_colors));

            // Survey completion timestamp.
            findViewById(R.id.txtTimestamp).setVisibility(VISIBLE);
            TextViewCompat.setTextAppearance(
                    findViewById(R.id.txtProgressSurvey), R.style.TextAppearance_Paws_Small);
            ((TextView)findViewById(R.id.txtTimestamp)).setText(
                    DateFormat.format("HH:mm dd/MM/yy",
                            mSharedPref.getLong("profile_time_completed", 0)));

            // Survey details label.
            findViewById(R.id.txtSurveyResultsLabel).setVisibility(VISIBLE);
        }

        return true;
    }

    public void onClickDetails(View view) {
        // Redirect to post-completion details summary screen.
        //Intent intent = new Intent(this, SurveyResultsActivity.class);
        //startActivity(intent);
    }

    public void onClickContinue(View view) {
        // Redirect to live survey screen.
        Intent intent = new Intent(this, SurveyQuestionActivity.class);
        startActivityForResult(intent, RequestCode.SURVEY_CONTINUE);
    }

    public void onClickReset(View view) {
        // TODO : remove and reimplement debug functionality
        mSharedEditor.putBoolean("app_init", false);
        mSharedEditor.putBoolean("facebook_init", false);

        // Reset all survey profile data.
        for (int i = 0; i < getResources().getInteger(R.integer.survey_question_count); ++i)
            mSharedEditor.putInt("survey_answer_" + i, 0);
        mSharedEditor.putInt("survey_last_question", 1);
        mSharedEditor.putLong("profile_time_completed", 0);
        mSharedEditor.apply();

        // Reinitialise the interface.
        initInterface();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);

        initInterface();

        switch (requestCode) {
            // Result was returned from the SurveyQuestionActivity call.
            case RequestCode.SURVEY_CONTINUE: {
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        int result = resultData.getIntExtra(
                                getResources().getString(R.string.intent_survey_result), ResultCode.RESULT_INCOMPLETE);
                        switch (result) {

                            // TODO : separate the actions in initInterface() per return code

                            // Reinitialise the interface after returning from the survey.
                            case ResultCode.RESULT_INCOMPLETE:
                            case ResultCode.RESULT_COMPLETE: {
                                //initInterface();
                                break;
                            }
                        }
                        break;
                    }
                    case Activity.RESULT_CANCELED: {

                        // . . .

                        break;
                    }
                    default: {

                        // . . .

                    }
                }
                break;
            }
        }
    }
}

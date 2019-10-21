package com.example.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class SelfAnalysisActivity extends BottomNavBarActivity {

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;

    private static boolean mIsRiskSelected;
    private static boolean mIsUpdatesSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_analysis);

        // Load global preferences.
        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mIsRiskSelected = false;
        mIsUpdatesSelected = false;

        // Initialise displays.
        initButtons();
    }

    private boolean initButtons() {
        // Button functionality.
        try {
            findViewById(R.id.cardRiskLow).setOnClickListener((view) -> onClickRisk(view));
            findViewById(R.id.cardRiskMed).setOnClickListener((view) -> onClickRisk(view));
            findViewById(R.id.cardRiskHigh).setOnClickListener((view) -> onClickRisk(view));
            findViewById(R.id.cardUpdatesLow).setOnClickListener((view) -> onClickUpdates(view));
            findViewById(R.id.cardUpdatesMed).setOnClickListener((view) -> onClickUpdates(view));
            findViewById(R.id.cardUpdatesHigh).setOnClickListener((view) -> onClickUpdates(view));
            findViewById(R.id.btnSubmit).setOnClickListener((view) -> onClickSubmit(view));
            findViewById(R.id.btnOptOut).setOnClickListener((view) -> onClickOptOut(view));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void onClickRisk(View view) {
        // Change styles of optional cards.
        int[] cardViews = {R.id.cardRiskLow, R.id.cardRiskMed, R.id.cardRiskHigh};
        for (int card : cardViews) {
            ((MaterialCardView)findViewById(card)).setStrokeColor(
                    ContextCompat.getColor(this, R.color.color_grey));
            ((MaterialCardView)findViewById(card)).setBackgroundColor(
                    ContextCompat.getColor(this, android.R.color.background_light));
        }
        int[] txtViews = {R.id.txtRiskLow, R.id.txtRiskMed, R.id.txtRiskHigh};
        for (int txt : txtViews) {
            ((TextView)findViewById(txt)).setTextColor(
                    ContextCompat.getColor(this, R.color.color_black));
        }
        ((MaterialCardView)findViewById(view.getId())).setStrokeColor(
                ContextCompat.getColor(this, R.color.color_primary));
        ((MaterialCardView)findViewById(view.getId())).setBackgroundColor(
                ContextCompat.getColor(this, R.color.color_primary));

        mIsRiskSelected = true;
        checkToEnableSubmission();
    }

    private void onClickUpdates(View view) {
        // Change styles of optional cards.
        ((MaterialCardView)findViewById(R.id.cardUpdatesLow)).setStrokeColor(
                ContextCompat.getColor(this, R.color.color_grey));
        ((MaterialCardView)findViewById(R.id.cardUpdatesMed)).setStrokeColor(
                ContextCompat.getColor(this, R.color.color_grey));
        ((MaterialCardView)findViewById(R.id.cardUpdatesHigh)).setStrokeColor(
                ContextCompat.getColor(this, R.color.color_grey));

        ((MaterialCardView)findViewById(view.getId())).setStrokeColor(
                ContextCompat.getColor(this, R.color.color_primary));

        mIsUpdatesSelected = true;
        checkToEnableSubmission();
    }

    private void checkToEnableSubmission() {
        if (mIsRiskSelected && mIsUpdatesSelected) {
            findViewById(R.id.btnSubmit).setEnabled(true);
        }
    }

    private void onClickSubmit(View view) {
        // At the end of the survey, finalise all details.
        mSharedEditor.putLong("selfanalysis_time_completed", System.currentTimeMillis());
        mSharedEditor.apply();

        // TODO : add and save results

        // Continue to the completed splash.
        setResult(RESULT_OK, new Intent().putExtra(
                getResources().getString(R.string.intent_survey_result), ResultCode.RESULT_COMPLETE));
        finish();
        Intent intent = new Intent(this, SurveyCompleteActivity.class);
        intent.putExtra(AnalysisEntryCode.EXTRA_KEY, AnalysisEntryCode.ENTRY_SELF_ANALYSIS);
        startActivity(intent);
    }

    private void onClickOptOut(View view) {

        // TODO : clear and save results

        // Return to the Profiling Menu activity.
        setResult(RESULT_CANCELED, new Intent().putExtra(
                getResources().getString(R.string.intent_survey_result), ResultCode.RESULT_INCOMPLETE));
        finish();
    }
}

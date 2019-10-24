package com.amw188.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

public class SelfAnalysisActivity extends BottomNavBarActivity {

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;

    private static float mScale;

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

        mScale = getApplicationContext().getResources().getDisplayMetrics().density;

        mIsRiskSelected = false;
        mIsUpdatesSelected = false;

        // Initialise displays.
        initButtons();
    }

    private boolean initButtons() {
        // Button functionality.
        try {
            findViewById(R.id.btnSubmit).setEnabled(false);

            findViewById(R.id.btnRiskLow).setOnClickListener((view) -> onClickRisk(view));
            findViewById(R.id.btnRiskMed).setOnClickListener((view) -> onClickRisk(view));
            findViewById(R.id.btnRiskHigh).setOnClickListener((view) -> onClickRisk(view));
            findViewById(R.id.btnUpdatesLow).setOnClickListener((view) -> onClickUpdates(view));
            findViewById(R.id.btnUpdatesMed).setOnClickListener((view) -> onClickUpdates(view));
            findViewById(R.id.btnUpdatesHigh).setOnClickListener((view) -> onClickUpdates(view));
            findViewById(R.id.btnOptOut).setOnClickListener((view) -> onClickOptOut(view));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void onClickRisk(View view) {
        // Change styles of optional cards.
        int[] buttons = {R.id.btnRiskLow, R.id.btnRiskMed, R.id.btnRiskHigh};
        for (int btn : buttons) {
            ((MaterialButton)findViewById(btn)).setStrokeWidth(
                    (int)(2 * mScale + 0.5f));
            ((MaterialButton)findViewById(btn)).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_card));
            ((MaterialButton)findViewById(btn)).setTextColor(
                    ContextCompat.getColor(this, R.color.color_on_background));
        }
        ((MaterialButton)findViewById(view.getId())).setStrokeWidth(0);
        ((MaterialButton)findViewById(view.getId())).setBackgroundColor(
                ContextCompat.getColor(this, R.color.color_accent));
        ((MaterialButton)findViewById(view.getId())).setTextColor(
                ContextCompat.getColor(this, R.color.color_on_primary));

        mIsRiskSelected = true;
        checkToEnableSubmission();
    }

    private void onClickUpdates(View view) {
        // Change styles of optional cards.
        int[] buttons = {R.id.btnUpdatesLow, R.id.btnUpdatesMed, R.id.btnUpdatesHigh};
        for (int btn : buttons) {
            ((MaterialButton)findViewById(btn)).setStrokeWidth(
                    (int)(2 * mScale + 0.5f));
            ((MaterialButton)findViewById(btn)).setBackgroundColor(
                    ContextCompat.getColor(this, R.color.color_card));
            ((MaterialButton)findViewById(btn)).setTextColor(
                    ContextCompat.getColor(this, R.color.color_on_background));
        }
        ((MaterialButton)findViewById(view.getId())).setStrokeWidth(0);
        ((MaterialButton)findViewById(view.getId())).setBackgroundColor(
                ContextCompat.getColor(this, R.color.color_accent));
        ((MaterialButton)findViewById(view.getId())).setTextColor(
                ContextCompat.getColor(this, R.color.color_on_primary));

        mIsUpdatesSelected = true;
        checkToEnableSubmission();
    }

    private void checkToEnableSubmission() {
        if (mIsRiskSelected && mIsUpdatesSelected) {
            findViewById(R.id.btnSubmit).setEnabled(true);
            findViewById(R.id.btnSubmit).setOnClickListener((view) -> onClickSubmit(view));
        }
    }

    private void onClickSubmit(View view) {
        // At the end of the survey, finalise all details.
        mSharedEditor.putLong("selfanalysis_time_completed", System.currentTimeMillis());
        mSharedEditor.apply();

        // TODO : add and save results

        // Continue to the completed splash.
        setResult(RESULT_OK, new Intent().putExtra(
                AnalysisEntryCode.EXTRA_KEY, ResultCode.RESULT_COMPLETE));
        finish();
        Intent intent = new Intent(this, SurveyCompleteActivity.class);
        intent.putExtra(AnalysisEntryCode.EXTRA_KEY, AnalysisEntryCode.ENTRY_SELF_ANALYSIS);
        startActivity(intent);
    }

    private void onClickOptOut(View view) {

        // TODO : clear and save results

        // Return to the Profiling Menu activity.
        setResult(RESULT_CANCELED, new Intent().putExtra(
                AnalysisEntryCode.EXTRA_KEY, ResultCode.RESULT_INCOMPLETE));
        finish();
    }
}

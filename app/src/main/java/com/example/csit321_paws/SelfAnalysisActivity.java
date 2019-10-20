package com.example.csit321_paws;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SelfAnalysisActivity extends BottomNavBarActivity {

    private SharedPreferences mSharedPref;
    private SharedPreferences.Editor mSharedEditor;

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

        initButtons();
    }

    private boolean initButtons() {
        // Button functionality.
        try {
            findViewById(R.id.btnSubmit).setOnClickListener((view) -> onClickSubmit(view));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void onClickSubmit(View view) {
        // At the end of the survey, finalise all details.
        mSharedEditor.putLong("profile_time_completed", System.currentTimeMillis());
        mSharedEditor.apply();

        // Continue to the completed splash.
        setResult(RESULT_OK, new Intent().putExtra(
                getResources().getString(R.string.intent_survey_result), ResultCode.RESULT_COMPLETE));
        finish();
        Intent intent = new Intent(this, SurveyCompleteActivity.class);
        intent.putExtra(AnalysisEntryCode.EXTRA_KEY, AnalysisEntryCode.ENTRY_SELF_ANALYSIS);
        startActivity(intent);
    }
}

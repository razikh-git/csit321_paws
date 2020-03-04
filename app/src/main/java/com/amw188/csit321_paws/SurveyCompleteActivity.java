package com.amw188.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SurveyCompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_complete);
        new Handler().postDelayed(
                this::endSurveyCompleteSplash,
                5000);
    }

    private void endSurveyCompleteSplash() {
        finish();
        startActivity(new Intent(SurveyCompleteActivity.this,
                SurveyEntryActivity.class));
    }
}

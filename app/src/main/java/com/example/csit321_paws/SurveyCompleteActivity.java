package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SurveyCompleteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_complete);

        if (savedInstanceState != null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                int activityCode = extras.getInt(AnalysisEntryCode.EXTRA_KEY);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        Intent intent;
                        switch (activityCode) {
                            case AnalysisEntryCode.ENTRY_SURVEY:
                                intent = new Intent(SurveyCompleteActivity.this,
                                        SurveyEntryActivity.class);
                                break;
                            case AnalysisEntryCode.ENTRY_FACEBOOK:
                                intent = new Intent(SurveyCompleteActivity.this,
                                        FacebookEntryActivity.class);
                                break;
                            default:
                                intent = new Intent(SurveyCompleteActivity.this,
                                        ProfilingMenuActivity.class);
                        }

                        startActivity(intent);
                    }
                }, 5000);
                return;
            }
        }

        // End the post-profiling splash preemptively if something goes wrong.
        finish();
    }
}

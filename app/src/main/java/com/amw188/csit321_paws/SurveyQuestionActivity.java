package com.amw188.csit321_paws;

import androidx.constraintlayout.widget.Constraints;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class SurveyQuestionActivity extends BottomNavBarActivity {

    SharedPreferences mSharedPref;

    private static final String TAG = "snowpaws_sq";

    private static final String TAG_PROGRESS = "progress";
    private JSONObject mSurveyJson = null;
    private int mIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_question);

        if (!(init() && continueSurvey())) {
            Toast.makeText(this,
                    "Failed to generate the survey.",
                    Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    private boolean init() {
        return initActivity() && initButtons() && initSurveyLayout();
    }

    private boolean initActivity() {
        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);

        // Bottom navigation bar functionality
        BottomNavigationView nav = findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        return true;
    }

    // Initialise all answer buttons
    private boolean initButtons() {
        try {
            findViewById(R.id.btnAnswer1).setOnClickListener(this::onClickAnswer);
            findViewById(R.id.btnAnswer2).setOnClickListener(this::onClickAnswer);
            findViewById(R.id.btnAnswer3).setOnClickListener(this::onClickAnswer);
            findViewById(R.id.btnAnswer4).setOnClickListener(this::onClickAnswer);
            findViewById(R.id.btnAnswer5).setOnClickListener(this::onClickAnswer);
            findViewById(R.id.btnAnswer6).setOnClickListener(this::onClickAnswer);
            findViewById(R.id.btnAnswer7).setOnClickListener(this::onClickAnswer);
        } catch (NullPointerException ex) {
            Log.e(TAG, "Failed in initButtons");
            return false;
        }
        return true;
    }

    // Initialise generic layout elements
    private boolean initSurveyLayout() {
        final int count = getResources().getInteger(R.integer.survey_question_count);

        // Populate the progress bar
        LinearLayout layout = findViewById(R.id.layProgressContainer);
        for (int i = 0; i < count; ++i) {
            // Generate layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    Math.round(getResources().getDimension(R.dimen.dimen_icon_survey_progress)),
                    Math.round(getResources().getDimension(R.dimen.dimen_icon_survey_progress)),
                    1);

            // Alternate the gravity of icons to achieve a waterfall pattern
            params.gravity = i % 2 == 0 ? Constraints.LayoutParams.LEFT : Constraints.LayoutParams.RIGHT;

            // Apply parameters to the icon
            final String tag = TAG_PROGRESS + (i);
            ImageView img = new ImageView(this);
            img.setLayoutParams(params);
            img.setTag(tag);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_unchecked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_midtone));
            layout.addView(img);
        }
        return true;
    }

    // Initialise the layout based on the last question completed
    private boolean continueSurvey() {
        mIndex = mSharedPref.getInt("survey_last_question", 0);

        // Load survey questionnaire data
        try {
            InputStream in = getResources().openRawResource(R.raw.paws_survey_json);
            byte[] b = new byte[in.available()];
            in.read(b);
            mSurveyJson = new JSONObject(new String(b));

        } catch (Exception e){
            e.printStackTrace();
            ((TextView)findViewById(R.id.txtQuestion)).setText(R.string.app_txt_fallback);
            return false;
        }

        // Fill the progress bubbles
        for (int i = 0; i < mIndex; ++i) {
            ImageView img = findViewById(R.id.layProgressContainer)
                    .findViewWithTag(TAG_PROGRESS + i);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_checked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_primary));
        }

        // Initialise the next question
        return initQuestion(mIndex);
    }

    // Initialise layout elements for the current question
    private boolean initQuestion(final int index) {
        if (mSurveyJson != null) {
            // Question header title
            ((TextView)findViewById(R.id.txtHeaderTitle)).setText(
                    getResources().getString(R.string.sq_title) + " " + (mIndex + 1)
            );

            // Question statement
            try {
                String txt = mSurveyJson.getJSONArray("questions").getJSONObject(index)
                        .getString("statement");
                ((TextView)findViewById(R.id.txtQuestion)).setText(txt);
            } catch (Exception e) {
                e.printStackTrace();
                ((TextView)findViewById(R.id.txtQuestion)).setText(
                        getResources().getString(R.string.app_txt_fallback));
                return false;
            }
        }
        return true;
    }

    // Reload question elements with the next question's data
    private void nextQuestion() {
        try {
            // Fill the matching progress bubble
            ImageView img = findViewById(R.id.layProgressContainer)
                    .findViewWithTag(TAG_PROGRESS + (mIndex - 1));
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_checked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_primary));

            // Show the next question
            initQuestion(mIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Respond to button selections
    protected void onClickAnswer(View view) {
        int answer = 0;
        switch (view.getId()) {
            case R.id.btnAnswer1:
                answer = 1;
                break;
            case R.id.btnAnswer2:
                answer = 2;
                break;
            case R.id.btnAnswer3:
                answer = 3;
                break;
            case R.id.btnAnswer4:
                answer = 4;
                break;
            case R.id.btnAnswer5:
                answer = 5;
                break;
            case R.id.btnAnswer6:
                answer = 6;
                break;
            case R.id.btnAnswer7:
                answer = 7;
                break;
        }

        try {
            if (mSurveyJson.getJSONArray("questions").getJSONObject(mIndex)
                    .getBoolean("negative"))
                answer *= -1;
        } catch (JSONException e) {
            Log.e(TAG, "onClickAnswer: You butchered your JSON!");
            e.printStackTrace();
        }

        // Increment up the question index and move on
        ++mIndex;

        // Push changes to saved data
        SharedPreferences.Editor sharedEditor = mSharedPref.edit();
        sharedEditor.putInt("survey_answer_" + mIndex, answer);
        sharedEditor.putInt("survey_last_question", mIndex);
        sharedEditor.apply();

        if (mIndex < getResources().getInteger(R.integer.survey_question_count)) {
            // Load in the next question
            nextQuestion();
        } else {
            // At the end of the survey, finalise all details
            sharedEditor.putLong("survey_time_completed", System.currentTimeMillis());
            sharedEditor.apply();

            // Continue to the completed splash
            setResult(RESULT_OK, new Intent().putExtra(
                    AnalysisEntryCodes.EXTRA_KEY, ResultCode.RESULT_COMPLETE));
            finish();
            Intent intent = new Intent(this, SurveyCompleteActivity.class);
            intent.putExtra(AnalysisEntryCodes.EXTRA_KEY, AnalysisEntryCodes.ENTRY_SURVEY);
            startActivity(intent);
        }
    }
}

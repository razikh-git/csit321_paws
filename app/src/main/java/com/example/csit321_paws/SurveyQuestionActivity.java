package com.example.csit321_paws;

import androidx.constraintlayout.widget.Constraints;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ActionMenuView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class SurveyQuestionActivity extends BottomNavBarActivity {

    SharedPreferences mSharedPref;
    SharedPreferences.Editor mSharedEditor;

    private static final String TAG = "snowpaws_sq";

    private static final String TAG_PROGRESS = "progress";
    private int mIndex;
    JSONObject mSurvey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_question);

        mSharedPref = this.getSharedPreferences(
                getResources().getString(R.string.app_global_preferences), Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        // Bottom navigation bar functionality.
        BottomNavigationView nav = (BottomNavigationView)findViewById(R.id.bottomNavigation);
        nav.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        initSurveyLayout();
        continueSurvey();
        initButtons();
    }

    // Reload question elements with the next question's data.
    private void nextQuestion() {
        try {
            // Fill the matching progress bubble.
            ImageView img = findViewById(R.id.layProgressContainer)
                    .findViewWithTag(TAG_PROGRESS + mIndex);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_checked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_primary));

            // Update the header question index.
            ((TextView)findViewById(R.id.txtHeaderTitle)).setText(
                    getResources().getString(R.string.sq_title)
                    + " " + mIndex);

            // Display the next question.
            ((TextView)findViewById(R.id.txtQuestion)).setText(
                    mSurvey.getJSONArray("questions").getJSONObject(mIndex).
                            getString("statement"));

        } catch (Exception e) {
            e.printStackTrace();
            ((TextView)findViewById(R.id.txtQuestion)).setText(
                    getResources().getString(R.string.app_txt_fallback));
        }

        // Increment the current index in questionnaire.
        mIndex++;
    }

    // Initialise layout elements contextual to the current question.
    private void initQuestion(int index) {
        if (mSurvey != null) {
            // Question title:
            try {
                String txt = mSurvey.getJSONArray("questions").getJSONObject(index)
                        .getString("statement");
                ((TextView)findViewById(R.id.txtQuestion)).setText(
                        txt);
            } catch (Exception e) {
                e.printStackTrace();
                ((TextView)findViewById(R.id.txtQuestion)).setText(
                        getResources().getString(R.string.app_txt_fallback));
            }
        }
    }

    // Initialise all answer buttons.
    private void initButtons() {
        findViewById(R.id.btnAnswer1).setOnClickListener((view) -> onClickAnswer(view));
        findViewById(R.id.btnAnswer2).setOnClickListener((view) -> onClickAnswer(view));
        findViewById(R.id.btnAnswer3).setOnClickListener((view) -> onClickAnswer(view));
        findViewById(R.id.btnAnswer4).setOnClickListener((view) -> onClickAnswer(view));
        findViewById(R.id.btnAnswer5).setOnClickListener((view) -> onClickAnswer(view));
        findViewById(R.id.btnAnswer6).setOnClickListener((view) -> onClickAnswer(view));
        findViewById(R.id.btnAnswer7).setOnClickListener((view) -> onClickAnswer(view));
    }

    // Initialise generic layout elements.
    private void initSurveyLayout() {
        int count = getResources().getInteger(R.integer.survey_question_count);

        // Populate the progress bar.
        LinearLayout layout = findViewById(R.id.layProgressContainer);
        for (int i = 0; i < count; ++i) {
            // Generate layout parameters.
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    Math.round(getResources().getDimension(R.dimen.dimen_icon_survey_progress)),
                    Math.round(getResources().getDimension(R.dimen.dimen_icon_survey_progress)),
                    1);

            // Alternate the gravity of icons to achieve a waterfall pattern.
            params.gravity = i % 2 == 0 ? Constraints.LayoutParams.LEFT : Constraints.LayoutParams.RIGHT;

            // Apply parameters to the icon.
            ImageView img = new ImageView(this);
            String tag = TAG_PROGRESS + (i);
            img.setLayoutParams(params);
            img.setTag(tag);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_unchecked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_grey));
            layout.addView(img);
        }
    }

    // Initialise the layout based on the last question completed.
    private void continueSurvey() {
        mIndex = mSharedPref.getInt("survey_last_question", 1);

        // Question counter:
        ((TextView)findViewById(R.id.txtHeaderTitle)).setText(
                getResources().getString(R.string.sq_title)
                + " " + mIndex
        );

        // Load survey questionnaire data.
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.paws_survey_json);
            byte[] b = new byte[in.available()];
            in.read(b);
            mSurvey = new JSONObject(new String(b));

        } catch (Exception e){
            e.printStackTrace();
            ((TextView)findViewById(R.id.txtQuestion)).setText(R.string.app_txt_fallback);
        }

        // Fill the progress bubbles.
        for (int i = 0; i < mIndex; ++i) {
            ImageView img = findViewById(R.id.layProgressContainer)
                    .findViewWithTag(TAG_PROGRESS + i);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_checked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_primary));
        }

        // Initialise the next question.
        initQuestion(mIndex);
    }

    // Respond to button selections.
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
            if (mSurvey.getJSONArray("questions").getJSONObject(mIndex)
                    .getBoolean("negative"))
                answer *= -1;
        } catch (JSONException e) {
            Log.d(TAG, "onClickAnswer: You butchered your JSON!");
            e.printStackTrace();
        }

        // Push changes to saved data.
        mSharedEditor.putInt("survey_answer_" + mIndex, answer);
        mSharedEditor.putInt("survey_last_question", mIndex);
        mSharedEditor.apply();

        if (mIndex < getResources().getInteger(R.integer.survey_question_count)) {
            // Load in the next question.
            nextQuestion();
        } else {
            // At the end of the survey, finalise all details.
            mSharedEditor.putLong("profile_time_completed", System.currentTimeMillis());
            mSharedEditor.apply();

            // Continue to the completed splash.
            setResult(RESULT_OK, new Intent().putExtra(
                    getResources().getString(R.string.intent_survey_result), ResultCode.RESULT_COMPLETE));
            finish();
            Intent intent = new Intent(this, SurveyCompleteActivity.class);
            intent.putExtra(AnalysisEntryCode.EXTRA_KEY, AnalysisEntryCode.ENTRY_SURVEY);
            startActivity(intent);
        }
    }
}

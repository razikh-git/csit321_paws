package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.InputStream;

public class SurveyQuestionActivity extends AppCompatActivity {

    SharedPreferences mSharedPref;
    SharedPreferences.Editor mSharedEditor;

    JSONObject mSurvey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_question);

        mSharedPref = this.getPreferences(Context.MODE_PRIVATE);
        mSharedEditor = mSharedPref.edit();

        initSurveyLayout();
        continueSurvey();
    }

    // Initialise layout elements contextual to the current question.
    private void initQuestion(int index) {
        if (mSurvey != null) {
            // Question title:
            try {
                ((TextView)findViewById(R.id.txtQuestion)).setText(
                        mSurvey.getString(String.valueOf(index)));

            } catch (Exception e) {
                e.printStackTrace();
                ((TextView)findViewById(R.id.txtQuestion)).setText(
                        getResources().getString(R.string.app_txt_fallback));
            }
        }
    }

    // Initialise generic layout elements.
    private void initSurveyLayout() {
        int count = getResources().getInteger(R.integer.survey_question_count);

    }

    // Initialise the layout based on the last question completed.
    private void continueSurvey() {
        int index = mSharedPref.getInt("survey_last_question", 0);

        // Question counter:
        ((TextView)findViewById(R.id.txtHeaderTitle)).setText(
                getResources().getString(R.string.sq_title)
                + " "
                + index
        );

        // Load survey questionnaire data.
        TextView txt = findViewById(R.id.txtInfo);
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.slp_info);
            byte[] b = new byte[in.available()];
            in.read(b);
            mSurvey = new JSONObject(new String(b));

        } catch (Exception e){
            e.printStackTrace();
            ((TextView)findViewById(R.id.txtQuestion)).setText(R.string.app_txt_fallback);
        }

        // Progress icons:
        //((ImageView)findViewById());

        // Initialise the next question.
        initQuestion(index);
    }

    // Respond to button selections.
    protected void onClickAnswer(View view) {
        switch (view.getId()) {
            //case
        }
    }
}

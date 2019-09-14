package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.InputStream;

public class SurveyEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_entry);

        // Button functionality.
        /*
        findViewById(R.id.cardSurveyDetails).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickDetails(view); }
        });
        */

        findViewById(R.id.btnContinue).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickContinue(view); }
        });

        findViewById(R.id.btnReturn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickReturn(view); }
        });

        // TODO : integrate binary files to allow for contextual highlighting of survey cards

        // Initialise info screen contents.
        TextView txt = findViewById(R.id.txtInfo);
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.se_info);
            byte[] b = new byte[in.available()];
            in.read(b);

            txt.setText(new String(b));
        } catch (Exception e){
            e.printStackTrace();
            txt.setText(R.string.app_txt_fallback);
        }

        // Initialise data summary contents.

        // TODO : integrate binary files to load user data

        if (true) {
            // Update complete/incomplete indicator icon
            // . . .

            // Update progress bar for survey completion percentage
            ProgressBar progressBar = findViewById(R.id.progressSurveyDetails);
            int progress = 30;
            progressBar.setProgress(progress);

            // Update timestamp of survey completion
            TextView timestamp = findViewById(R.id.timestampTop);
            //timestamp.setText();
            timestamp = findViewById(R.id.timestampBottom);
            //timestamp.setText();
        }
    }

    public void onClickDetails(View view){
        // Redirect to post-completion details summary screen.
        //if (asdf) {
        //  Intent intent = new Intent(this, SurveyDetailsActivity.class);
        //  startActivity(intent);
        //}
    }

    public void onClickContinue(View view){
        // Redirect to live survey screen.
        //Intent intent = new Intent(this, SurveyActivity.class);
        //startActivity(intent);
    }

    public void onClickReturn(View view){
        // Redirect to previous screen.
        this.finish();
    }
}

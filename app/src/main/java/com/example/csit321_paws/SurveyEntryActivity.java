package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;

public class SurveyEntryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_entry);

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor sharedEditor = sharedPref.edit();

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

        // Initialise file data.
        //sharedEditor.

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
            // Complete/incomplete outline
            MaterialCardView cardView = findViewById(R.id.cardSurveyDetails);
            //cardView.setStrokeColor(R.color.color_primary);

            // Complete/incomplete indicator icon
            // . . .

            // Progress bar for survey completion percentage
            ProgressBar progressBar = findViewById(R.id.barProgressSurvey);
            progressBar.setIndeterminate(false);
            int progress = 30;
            progressBar.setProgress(progress);

            txt = findViewById(R.id.txtProgressSurvey);
            String str = "0/20";
            //str =
            txt.setText(str);

            // Timestamp of survey completion
            txt = findViewById(R.id.txtTimestampTop);
            //str =
            //txt.setText(str);
            txt = findViewById(R.id.txtTimestampBottom);
            //str =
            //txt.setText(str);
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

package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;

public class ProfilingMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profilingmenu);

        // Button functionality.
        findViewById(R.id.btnSurvey).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickSurvey(view); }
        });

        findViewById(R.id.btnFacebook).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickFacebook(view); }
        });

        findViewById(R.id.btnReturn).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickReturn(view); }
        });

        // TODO : integrate binary files to allow for contextual highlighting of survey cards

        // Initialise info screen contents.
        TextView txt = findViewById(R.id.txtInfo);
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.slp_info);
            byte[] b = new byte[in.available()];
            in.read(b);

            txt.setText(new String(b));
        } catch (Exception e){
            e.printStackTrace();
            txt.setText(R.string.app_txt_fallback);
        }
    }

    public void onClickSurvey(View view){
        // Redirect to survey intro screen.
        Intent intent = new Intent(this, SurveyEntryActivity.class);
        startActivity(intent);
    }

    public void onClickFacebook(View view){
        // Redirect to facebook intro screen.
        //Intent intent = new Intent(this, ProfilingMenuActivity.class);
        //startActivity(intent);
    }

    public void onClickReturn(View view){
        // Redirect to previous screen.
        this.finish();
    }
}

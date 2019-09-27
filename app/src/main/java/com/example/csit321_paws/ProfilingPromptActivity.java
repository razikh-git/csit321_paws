package com.example.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import java.io.InputStream;

public class ProfilingPromptActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profiling_prompt);

        // Button functionality.
        findViewById(R.id.btnDecline).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickDecline(view); }
        });

        findViewById(R.id.btnAccept).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { onClickAccept(view); }
        });

        // Initialise info screen contents.
        TextView txt = findViewById(R.id.txtInfo);
        try {
            Resources res = getResources();
            InputStream in = res.openRawResource(R.raw.pp_info);
            byte[] b = new byte[in.available()];
            in.read(b);

            txt.setText(new String(b));
            txt.setMovementMethod(new ScrollingMovementMethod());
        } catch (Exception e){
            e.printStackTrace();
            txt.setText(R.string.app_txt_fallback);
        }
    }

    public void onClickDecline(View view){
        // Redirect to app landing screen.
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void onClickAccept(View view){
        // Redirect to app landing screen.
        Intent intent = new Intent(this, ProfilingMenuActivity.class);
        startActivity(intent);
    }
}

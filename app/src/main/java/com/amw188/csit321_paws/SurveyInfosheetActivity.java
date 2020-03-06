package com.amw188.csit321_paws;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SurveyInfosheetActivity extends AppCompatActivity {

    private static final String TAG = PrefConstValues.tag_prefix + "pp";

    private int mIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_infosheet);

        if (!init()) {
            Toast.makeText(this,
                    "Failed to initialise onloading activity.",
                    Toast.LENGTH_LONG)
                    .show();
        }
    }

    private boolean init() {
        return initActivity() && initLayout() && initButtons();
    }

    private boolean initActivity() {
        // Initialise info screen contents
        ((TextView)findViewById(R.id.txtInfo)).setMovementMethod(new ScrollingMovementMethod());
        return true;
    }

    private boolean initButtons() {
        findViewById(R.id.btnDecline).setOnClickListener(this::onClickDecline);
        findViewById(R.id.btnAccept).setOnClickListener(this::onClickAccept);
        findViewById(R.id.cardInfo).setOnClickListener(this::onNextInfo);
        findViewById(R.id.txtInfo).setOnClickListener(this::onNextInfo);
        return true;
    }

    // Initialise generic layout elements
    private boolean initLayout() {
        // Populate the progress bar
        LinearLayout layout = findViewById(R.id.layProgressContainer);
        for (int i = 1; i < 4; ++i) {
            // Generate layout parameters
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    Math.round(getResources().getDimension(R.dimen.dimen_icon_tiny)),
                    Math.round(getResources().getDimension(R.dimen.dimen_icon_tiny)),
                    1);

            // Apply parameters to the icon
            ImageView img = new ImageView(this);
            String tag = TAG + i;
            img.setLayoutParams(params);
            img.setTag(tag);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_unchecked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_midtone));
            layout.addView(img);
        }

        // Initialise the first info box
        onNextInfo(findViewById(R.id.cardInfo));
        return true;
    }

    public void onNextInfo(View view) {
        // Wrap to first element again
        if (++mIndex > 3) {
            mIndex = 1;
        }

        // Default the progress bubbles
        for (int i = 1; i < 4; ++i) {
            ImageView img = findViewById(R.id.layProgressContainer)
                    .findViewWithTag(TAG + i);
            img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_unchecked));
            img.setColorFilter(ContextCompat.getColor(this, R.color.color_midtone));
        }

        // Fill the now-current progress bubble
        ImageView img = findViewById(R.id.layProgressContainer)
                .findViewWithTag(TAG + mIndex);
        img.setImageDrawable(getDrawable(R.drawable.ic_radiobutton_checked));
        img.setColorFilter(ContextCompat.getColor(this, R.color.color_primary));

        // Show the now-current string
        if (mIndex == 1) {
            ((TextView) findViewById(R.id.txtInfo)).setText(R.string.pp_desc_1);
        } else if (mIndex == 2) {
            ((TextView)findViewById(R.id.txtInfo)).setText(R.string.pp_desc_2);
        } else if (mIndex == 3) {
            ((TextView)findViewById(R.id.txtInfo)).setText(R.string.pp_desc_3);
        }
    }

    public void onClickDecline(View view){
        // Redirect to app landing screen
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
    }

    public void onClickAccept(View view){
        // Redirect to app landing screen
        Intent intent = new Intent(this, SurveyEntryActivity.class);
        startActivity(intent);
    }
}

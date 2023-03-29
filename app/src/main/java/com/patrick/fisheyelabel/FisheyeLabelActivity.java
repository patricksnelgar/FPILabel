package com.patrick.fisheyelabel;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.patrick.fpilabel.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FisheyeLabelActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private String mRecordingMethod;
    private final String mTAG = "Label Activity";
    private final String PREFERENCES_LAST_RECORDING_VALUE = "last_recording_value";
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        Button mDecButton = (Button)findViewById(R.id.decButton);
        mDecButton.setOnClickListener(mDecButtonListener);

        Button mIncButton = (Button)findViewById(R.id.incButton);
        mIncButton.setOnClickListener(mIncButtonListener);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        SharedPreferences mPrefManager = PreferenceManager.getDefaultSharedPreferences(FisheyeLabelActivity.this);
        mRecordingMethod = mPrefManager.getString("recording_method", "----");

        Log.d(mTAG, "recording mode is: " + mRecordingMethod);

        TextView mLabel = (TextView)findViewById(R.id.label_text);
        // Set up to use defaults
        switch(mRecordingMethod) {
            case "FPI":
                mLabel.setText(R.string.fpi_default);
                break;
            case "List":
                mLabel.setText(R.string.list_default);
                break;
            case "Alpha-Numeric":
                mLabel.setText(R.string.alpha_numeric_default);
                break;
            default:
                mLabel.setText("Uh..HI??");
                break;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        TextView mText = (TextView)findViewById(R.id.label_text);
        //Log.d(mTAG, "I am resuming!");
        // Set up UI depending on which method is used
        // pull from settings and fallback on defaults

        SharedPreferences mPrefManager = PreferenceManager.getDefaultSharedPreferences(FisheyeLabelActivity.this);
        int mLastValue = Integer.parseInt(mPrefManager.getString(PREFERENCES_LAST_RECORDING_VALUE, "0"));

        mRecordingMethod = mPrefManager.getString("recording_method", "----");

        TextView mLabel = (TextView)findViewById(R.id.label_text);

        switch(mRecordingMethod) {
            case "FPI":
                mLabel.setText(R.string.fpi_default);
                break;
            case "List":
                mLabel.setText(getListItem(mLastValue));
                break;
            case "Alpha-Numeric":
                mLabel.setText(R.string.alpha_numeric_default);
                break;
            default:
                mLabel.setText("You broke it");
                break;
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.settings_menu) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * For list mode only
     *
     * @param lastValue last index into the list of labels
     * @return next item in the label list
     */
    private String getListItem(int lastValue) {
        String res = "";

        // Get the label file
        SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(FisheyeLabelActivity.this);

        try {
            Uri mUri = Uri.parse(mSharedPrefs.getString("label_file", ""));
            //Log.d(mTAG, "URI: " + mUri.getPath());
            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(mUri, takeFlags);
            InputStream mIS = getContentResolver().openInputStream(mUri);
            BufferedReader mBR = new BufferedReader(new InputStreamReader(mIS));
            int index = 0;
            String mPrevLabel = res;
            if(lastValue > 0){
                while(index < lastValue + 1) {
                    try {
                        mPrevLabel = res;
                        res = mBR.readLine();
                        // Log.d(mTAG, "Target = " + lastValue + " Prev = " + mPrevLabel + " next = " + res);
                        if(res == null) {
                            res = mPrevLabel;
                            break;
                        }
                    }catch (Exception e) {
                        Log.e(mTAG, "Error reading file: " + e.getLocalizedMessage());
                    }
                    index++;
                }
            } else {
                res = mBR.readLine();
            }

        } catch (Exception e) {
            Log.e(mTAG, "error: " + e.getLocalizedMessage());
        }
        return res;
    }

    private final View.OnClickListener mDecButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {

            switch(mRecordingMethod) {
                case "List":
                    decListIndex();
            }
        }
    };

    private void decListIndex() {
        SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(FisheyeLabelActivity.this);
        int mPrevIndex = Integer.parseInt(mSharedPrefs.getString(PREFERENCES_LAST_RECORDING_VALUE, "0"));
        mPrevIndex--;
        TextView mLabelText = (TextView)findViewById(R.id.label_text);
        String mPrevLabel = mLabelText.getText().toString();
        String mNewLabel = getListItem(mPrevIndex);

        if(mNewLabel.equals(mPrevLabel))
            mPrevIndex++;

        // Log.d(mTAG, "Index: " + mPrevIndex + " - new label: " + mNewLabel);

        mLabelText.setText(mNewLabel);

        String mCurrentIndex = Integer.toString(mPrevIndex);
        mSharedPrefs.edit().putString(PREFERENCES_LAST_RECORDING_VALUE, mCurrentIndex).apply();
    }

    private final View.OnClickListener mIncButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(mRecordingMethod.equals("List")) {

                SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(FisheyeLabelActivity.this);
                int mPrevIndex = Integer.parseInt(mSharedPrefs.getString(PREFERENCES_LAST_RECORDING_VALUE, "0"));
                mPrevIndex++;
                TextView mLabelText = (TextView)findViewById(R.id.label_text);
                String mPrevLabel = mLabelText.getText().toString();
                String mNewLabel = getListItem(mPrevIndex);

                if(mNewLabel.equals(mPrevLabel))
                    mPrevIndex--;

                mLabelText.setText(mNewLabel);
                // Log.d(mTAG, "Index: " + mPrevIndex + " - new label: " + mNewLabel);

                String mCurrentIndex = Integer.toString(mPrevIndex);
                mSharedPrefs.edit().putString(PREFERENCES_LAST_RECORDING_VALUE, mCurrentIndex).apply();
            }
        }
    };
}

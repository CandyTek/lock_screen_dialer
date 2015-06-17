package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.os.Handler;


public class LockScreenKeypadPinActivity extends LockScreenActivity
        implements View.OnClickListener, View.OnTouchListener {

    private final static String TAG = "LSKeypadPinActivity";

    // UI elements
/*    private TextView mPinDisplayView;
    private Button mDeleteButton;
    private Button mOkButton;
    private Button[] mkeypadButtons;*/

    // variables relating to the logic of the lockscreen
    private String mPinStored;
    private String mPinEntered;
    private boolean mPinInvalidDisplayFlag;
    private int mNumTries;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);
        View wrapperView = getWrapperView();
/*
        try {
            wrapperView = getWindow().getDecorView().getRootView();
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible root view used with this activity.", e);
            finish();
            return;
        }
*/
                //(RelativeLayout) ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);



        // Initialize some basic variables
        mNumTries = 0;  // Possibly modified later by onRestoreInstanceState
        //mkeypadButtons = new Button[10];
        mPinStored = getStoredPin();
        mPinInvalidDisplayFlag = false;

        Button[] keypadButtons;
        TextView pinDisplayView;
        Button deleteButton, okButton;

        // In case returning to this display from elsewhere, want to reset
        // Will also catch error when there is improper layout
        //Log.d(TAG, "onCreate() calling resetPinEntry()");
        resetPinEntry(getString(R.string.lock_screen_pin_default_display));

        keypadButtons = getKeypadButtons(wrapperView);
        deleteButton = getDeleteButton(wrapperView);
        okButton = getOkButton(wrapperView);

        // Set the onClickListeners to the appropriate views
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);

        for (int i=0; i < 10; i++) {
            try {
                keypadButtons[i].setOnClickListener(this); // all buttons will have an onClickListener
                String filename = getString(R.string.key_number_store_prefix_phone) + i;

                if (sharedPref == null) {
                    Log.w(TAG, "Unable to access shared preferences file"
                            + getString(R.string.speed_dial_preference_file_key) + "; returned null.");
                    continue;
                } else if (sharedPref.getString(filename, null) != null) { //only set the long click where necessary
                    //Log.d(TAG, "Setting long click on key " + i);
                    keypadButtons[i].setOnTouchListener(this);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Keypad button " + i + " is invalid.", e);
                onFatalError();
                return;
            }
        }

        try {
            deleteButton.setOnClickListener(this);
            okButton.setOnClickListener(this);
        } catch (NullPointerException e) {
            Log.e(TAG, "Delete and/or OK button invalid.", e);
            onFatalError();
            return;
        }

        if (mPinStored == null) {
            Log.e(TAG, "Stored PIN is null.");
            onFatalError(); //For now, just exit.  TODO: find good way to handle these errors.
            return;
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        //Log.d(TAG, "onResume() called.");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mNumTries = savedInstanceState.getInt("numTries");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("numTries", mNumTries);
    }

    @Override
    public void onBackPressed() {  // Overrides the back button to prevent exit
        return;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Called onConfigurationChanged.");

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d(TAG, "Landscape/Portrait parameter sent.");
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

    }

    @Override
    public void onClick (View view) {
        super.onClick(view); // Need to call the super to catch click of end call button

        if (mLongPressFlag) {
            mLongPressFlag = false;
            //Log.d(TAG, "onClick() calling resetPinEntry() on long click");
            resetPinEntry(getString(R.string.lock_screen_pin_default_display));
            return;
        }
        int num = getSpeedDialButtonPressed(view.getId(), -1);
        if (num == -1) { //Not a speed dial number
            switch (view.getId()) {
                case R.id.lock_screen_pin_button_OK:
                    if (mPinEntered.length() != 0){ // We don't care if the OK button was pressed without any PIN entered
                        wrongPinEntered();  // Because our functionality will automatically accept a PIN when it is entered, we can assume error
                    }
                    break;
                case R.id.lock_screen_pin_button_delete:
                    //Log.d(TAG, "onClick() calling resetPinEntry() on delete button pressed.");
                    resetPinEntry(getString(R.string.lock_screen_pin_default_display));
                    break;
            }
            return;
        }

        mPinEntered += num;

        //Log.d(TAG, "onClick() -- digit " + num + "entered, pin display tag is " + mPinInvalidDisplayFlag);
        // Display a new "digit" on the text view
        if (!mPinInvalidDisplayFlag) {// meaning the display is not taken by displaying an invalid pin message
            TextView pinDisplayView = getPinDisplayView(getWrapperView());
            try {
                if (pinDisplayView.getText().toString().
                        equals(getString(R.string.lock_screen_pin_default_display))) {
                    pinDisplayView.setText("*");
                    setPinDisplayToPasswordView();
                } else if (pinDisplayView.getText().length() <=
                        getResources().getInteger(R.integer.lock_screen_pin_max_pin_display)){  // TODO: put a cap on the PIN -- implemented in settings as well
                    pinDisplayView.setText(pinDisplayView.getText().toString() + "*");
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "pin display text view invalid.", e);
                onFatalError();
                return;
            }
        }

        //  Now check whether the PIN entered so far matches the stored PIN
        //Log.d(TAG, mPinEntered + " vs " + mPinStored);
        if (mPinEntered.equals(mPinStored)) {
            onCorrectPasscode();
        }
    }

    /**
     * Method that implements a longer-press version of onLongClick. Method works by creating
     * a runnable to be invoked in the future, based on the delay specified in the resources.
     * In the event that the user lifts up from the click before the runnable runs, the runnable
     * is revoked.
     * @param v
     * @param event
     * @return
     */
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mHandler == null) { // means there is no pending handler
                    mHandler = new Handler();
                    mRunnable = new DialerRunnable(getSpeedDialButtonPressed(v.getId(), -1));
                    mHandler.postDelayed(
                            mRunnable,
                            getResources().getInteger(R.integer.lock_screen_pin_long_press_delay));
                    mLongPressFlag = false;  // flag to let
                }

                break;
            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mRunnable);
                mHandler = null;
                mRunnable = null;
                /*if (mLongPressFlag) { // Long press was consumed by the runnable!
                    mLongPressFlag = false; // reset it
                    return true; // Hopefully this consumes the event so onClick is not triggered
                }*/

                //Now it is just a normal button press

                break;
        }

        return false;
    }

    private Button[] getKeypadButtons(View wrapperView) {
        Button[] keypadButtons = new Button[10];

        try {
            keypadButtons[0] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_0);
            keypadButtons[1] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_1);
            keypadButtons[2] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_2);
            keypadButtons[3] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_3);
            keypadButtons[4] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_4);
            keypadButtons[5] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_5);
            keypadButtons[6] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_6);
            keypadButtons[7] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_7);
            keypadButtons[8] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_8);
            keypadButtons[9] = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_9);
        } catch (NullPointerException e) {
            Log.e(TAG, "Wrapper view could not be located in this activity.", e);
            onFatalError();  // TODO: find way to gracefully handle these exceptions
            return null;
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible layout used with this activity.", e);
            onFatalError();
            return null;
        }
        return keypadButtons;
    }

    private TextView getPinDisplayView(View wrapperView) {
        TextView t;
        try {
            t = (TextView) wrapperView.findViewById(R.id.lock_screen_pin_display);
        } catch (NullPointerException e) {
            Log.e(TAG, "Wrapper view could not be located in this activity.", e);
            onFatalError();  // TODO: find way to gracefully handle these exceptions
            return null;
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible layout used with this activity.", e);
            onFatalError();
            return null;
        }

        if (t == null) {
            Log.e(TAG, "Incompatible layout use with this activity - pin display view null.");
            onFatalError();
        }
        return t;
    }

    private Button getOkButton(View wrapperView) {
        Button b;
        try {
            b = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_OK);
        } catch (NullPointerException e) {
            Log.e(TAG, "Wrapper view could not be located in this activity.", e);
            onFatalError();  // TODO: find way to gracefully handle these exceptions
            return null;
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible layout used with this activity.", e);
            onFatalError();
            return null;
        }
        return b;
    }

    private Button getDeleteButton(View wrapperView) {
        Button b;
        try {
            b = (Button) wrapperView.findViewById(R.id.lock_screen_pin_button_delete);
        } catch (NullPointerException e) {
            Log.e(TAG, "Wrapper view could not be located in this activity.", e);
            onFatalError();  // TODO: find way to gracefully handle these exceptions
            return null;
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible layout used with this activity.", e);
            onFatalError();
            return null;
        }
        return b;
    }



    private void setPinDisplayToPasswordView(){
        TextView pinDisplayView = getPinDisplayView(getWrapperView());
        Button deleteButton = getDeleteButton(getWrapperView());
        try {
            pinDisplayView.setTransformationMethod(new PasswordTransformationMethod());  // Turns the text to dots
            pinDisplayView.setTextScaleX(1.2f); // sets space between the characters
            deleteButton.setVisibility(View.VISIBLE);
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout is incompatible with activity.", e);
            onFatalError();
        }
    }

    private String getStoredPin(){
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        return sharedPref.getString( getString(R.string.lock_screen_passcode_value_key), null);

    }
    private void wrongPinEntered() {
        // TODO: implement switch statement below
        /*int delay;
        String message;
        switch (mNumTries / 3) {
            case 0:  // meaning there have been less than 3 tries
                delay = getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay);
                message = getString(R.string.lock_screen_wrong_pin_entered);
                break;
            case 1:

        }*/
        resetPinEntry(getString(R.string.lock_screen_wrong_pin_entered));
        mPinInvalidDisplayFlag = true;
        mNumTries++;
        SetTextInViewRunnable r = new SetTextInViewRunnable(
                getString(R.string.lock_screen_pin_default_display),
                getPinDisplayView(getWrapperView()));
        Handler h = new Handler();
        h.postDelayed(r, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    /**
     * Method that resets the Pin Entry
     */
    private void resetPinEntry(String displayText) {
        TextView pinDisplayView = getPinDisplayView(getWrapperView());
        Button deleteButton = getDeleteButton(getWrapperView());
        try {
            pinDisplayView.setText(displayText);
            pinDisplayView.setTransformationMethod(null);
            pinDisplayView.setTextScaleX(1.0f);
            deleteButton.setVisibility(View.INVISIBLE);
        } catch (NullPointerException e) {
            Log.e(TAG, "Incompatible layout with this activity.", e);
            onFatalError();
        }
        //Log.d(TAG, "pinDisplayView text is reset to " + pinDisplayView.getText());
        mPinEntered = "";
    }

    private void resetPinInvalidDisplayFlag() {
        mPinInvalidDisplayFlag = false;
    }

    private int getSpeedDialButtonPressed(int id, int defaultReturn){
        switch (id) {
            // TODO: set zero key as a lock screen dialer?
            case R.id.lock_screen_pin_button_0:
                return 0;
            case R.id.lock_screen_pin_button_1:
                return 1;
            case R.id.lock_screen_pin_button_2:
                return 2;
            case R.id.lock_screen_pin_button_3:
                return 3;
            case R.id.lock_screen_pin_button_4:
                return 4;
            case R.id.lock_screen_pin_button_5:
                return 5;
            case R.id.lock_screen_pin_button_6:
                return 6;
            case R.id.lock_screen_pin_button_7:
                return 7;
            case R.id.lock_screen_pin_button_8:
                return 8;
            case R.id.lock_screen_pin_button_9:
                return 9;
            default:
                return defaultReturn;
        }
    }

    private int getPinEnteredLength(){
        return mPinEntered.length();
    }

    private class SetTextInViewRunnable implements Runnable {
        private String text;
        private TextView view;

        public SetTextInViewRunnable (String t, TextView v) {
            text = t;
            view = v;
        }
        public void run() {
            int i = getPinEnteredLength();
            if (i == 0) {
                view.setText(text);
                //Log.d(TAG, "Runnable has set pinDisplayView to " + text);
            } else { // we need to set the string to the appropriate length
                String s = "";
                for (int j = 0; j < i; j++) {
                    s += "*";
                }
                view.setText(s);
                setPinDisplayToPasswordView();
                //Log.d(TAG, "Runnable has set pinDisplayVyew to " + s);
            }
            resetPinInvalidDisplayFlag();
        }
    }

}


package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
    private TextView mPinDisplayView;
    private Button mDeleteButton;
    private Button mOkButton;
    private Button[] mkeypadButtons;

    // variables relating to the logic of the lockscreen
    private String mPinStored;
    private String mPinEntered;
    private boolean mPinInvalidDisplayFlag;
    private int mNumTries;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);

        // Initialize some basic variables
        mNumTries = 0;  // Possibly modified later by onRestoreInstanceState
        mkeypadButtons = new Button[10];
        mPinStored = getStoredPin();
        mPinInvalidDisplayFlag = false;

        mPinDisplayView = (TextView) getWrapperView().findViewById(R.id.lock_screen_pin_display);
        mPinDisplayView.setText(getString(R.string.lock_screen_pin_default_display)); // In case returning to this display from elsewhere, want to reset
        mPinEntered = ""; // Same -- we want to reset

        mDeleteButton = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_delete);
        mOkButton = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_OK);

        mkeypadButtons[0] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_0);
        mkeypadButtons[1] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_1);
        mkeypadButtons[2] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_2);
        mkeypadButtons[3] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_3);
        mkeypadButtons[4] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_4);
        mkeypadButtons[5] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_5);
        mkeypadButtons[6] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_6);
        mkeypadButtons[7] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_7);
        mkeypadButtons[8] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_8);
        mkeypadButtons[9] = (Button) getWrapperView().findViewById(R.id.lock_screen_pin_button_9);

        // Set the onClickListeners to the appropriate views
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE
        );

        for (int i=0; i < 10; i++) {
            mkeypadButtons[i].setOnClickListener(this); // all buttons will have an onClickListener

            String filename = getString(R.string.key_number_store_prefix_phone) + i;
            //Log.d(TAG, "Attempting to access " + filename);
            if (sharedPref.getString(filename, null) != null) { //only set the long click where necessary
                //Log.d(TAG, "Setting long click on key " + i);
                mkeypadButtons[i].setOnTouchListener(this);
            }
        }
        mDeleteButton.setOnClickListener(this);
        mOkButton.setOnClickListener(this);

        if (mPinStored == null) {
            Log.d(TAG, "Stored PIN is null.");
            onCorrectPasscode(); //For now, just exit.  TODO: find good way to handle these errors.
        }
    }

    @Override
    public void onResume(){
        super.onResume();
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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onClick (View view) {
        super.onClick(view); // Need to call the super to catch click of end call button

        if (mLongPressFlag) {
            mLongPressFlag = false;
            resetPinEntry(getString(R.string.lock_screen_pin_default_display));
            return;
        }
        int num = getSpeedDialButtonPressed(view.getId(), -1);
        if (num == -1) { //Not a speed dial number
            if (view.getId() == R.id.lock_screen_pin_button_OK) {
                wrongPinEntered();  // Because our functionality will automatically accept a PIN when it is entered, we can assume error
            } else {
                //Log.d(TAG, "delete button pressed.");
                resetPinEntry(getString(R.string.lock_screen_pin_default_display));
            }
            return;
        }

        mPinEntered += num;

        // Display a new "digit" on the text view
        if (!mPinInvalidDisplayFlag) {// meaning the display is not taken by displaying an invalid pin message
                if (mPinDisplayView.getText().toString().
                        equals(getString(R.string.lock_screen_pin_default_display))){
                    mPinDisplayView.setText("*");
                    setPinDisplayToPasswordView();
                } else {
                    mPinDisplayView.setText(mPinDisplayView.getText().toString() + "*");
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

    private void setPinDisplayToPasswordView(){
        mPinDisplayView.setTransformationMethod(new PasswordTransformationMethod());  // Turns the text to dots
        mPinDisplayView.setTextScaleX(1.2f); // sets space between the characters
        mDeleteButton.setVisibility(View.VISIBLE);
    }

    private String getStoredPin(){
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        return sharedPref.getString( getString(R.string.lock_screen_passcode_value_key), null);

    }
    private void wrongPinEntered() {
        resetPinEntry(getString(R.string.lock_screen_wrong_pin_entered));
        mPinInvalidDisplayFlag = true;
        mNumTries++;
        SetTextInViewRunnable r = new SetTextInViewRunnable(
                getString(R.string.lock_screen_pin_default_display),
                mPinDisplayView);
        Handler h = new Handler();
        h.postDelayed(r, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    /**
     * Method that resets the Pin Entry
     */
    private void resetPinEntry(String displayText) {
        mPinDisplayView.setText(displayText);
        mPinDisplayView.setTransformationMethod(null);
        mPinDisplayView.setTextScaleX(1.0f);
        mPinEntered = "";
        mDeleteButton.setVisibility(View.INVISIBLE);
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
            } else { // we need to set the string to the appropriate length
                String s = "";
                for (int j = 0; j < i; j++) {
                    s += "*";
                }
                view.setText(s);
                setPinDisplayToPasswordView();
                resetPinInvalidDisplayFlag();
            }
        }
    }

}


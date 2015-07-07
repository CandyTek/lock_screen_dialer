package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


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
    private int mLastBtnTouchedNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate() called.");
        //mLayoutId = R.layout.activity_lock_screen_keypad_pin;  // must be set before calling super
        super.onCreate(savedInstanceState);

        // Initialize some basic variables
        mNumTries = 0;  // Possibly modified later by onRestoreInstanceState
        mPinStored = getStoredPin();
        mPinInvalidDisplayFlag = false;

        Button[] keypadButtons;
        Button deleteButton, okButton;

        // In case returning to this display from elsewhere, want to reset
        // Will also catch error when there is improper layout
        //Log.d(TAG, "onCreate() calling resetPinEntry()");
        resetPinEntry(getString(R.string.lock_screen_pin_default_display));

        keypadButtons = getKeypadButtons();
        deleteButton = getDeleteButton();
        okButton = getOkButton();

        // Set the onClickListeners to the appropriate views
        SharedPreferences dialPrefs = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);
        SharedPreferences genPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        String font = genPrefs.getString(
                getString(R.string.key_select_lock_screen_fonts),
                getString(R.string.font_default));

        for (int i=0; i < 10; i++) {
            try {
                keypadButtons[i].setOnClickListener(this); // all buttons will have an onClickListener
                String filename = getString(R.string.key_number_store_prefix_phone) + i;

                if (dialPrefs == null) {
                    Log.w(TAG, "Unable to access shared preferences file"
                            + getString(R.string.speed_dial_preference_file_key) + "; returned null.");
                    continue;
                } else if (dialPrefs.getString(filename, null) != null) { //only set the long click where necessary
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

        try {
            if (!font.equals(getString(R.string.font_default))) {
                TextView pinDisplayView = getPinDisplayView();

                TextView textNum0 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_0);
                TextView textNum1 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_1);
                TextView textNum2 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_2);
                TextView textNum3 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_3);
                TextView textNum4 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_4);
                TextView textNum5 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_5);
                TextView textNum6 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_6);
                TextView textNum7 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_7);
                TextView textNum8 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_8);
                TextView textNum9 =
                        (TextView) getView(R.id.lock_screen_pin_text_num_9);
                TextView textNumOK =
                        (TextView) getView(R.id.lock_screen_pin_text_OK);

                TextView alphabetics2 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_2);
                TextView alphabetics3 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_3);
                TextView alphabetics4 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_4);
                TextView alphabetics5 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_5);
                TextView alphabetics6 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_6);
                TextView alphabetics7 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_7);
                TextView alphabetics8 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_8);
                TextView alphabetics9 =
                        (TextView) getView(R.id.lock_screen_pin_alphabetics_9);

                deleteButton.setTypeface(Typeface.create(font, Typeface.NORMAL));
                okButton.setTypeface(Typeface.create(font, Typeface.NORMAL));
                pinDisplayView.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum0.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum1.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum2.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum3.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum4.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum5.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum6.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum7.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum8.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNum9.setTypeface(Typeface.create(font, Typeface.NORMAL));
                textNumOK.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics2.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics3.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics4.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics5.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics6.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics7.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics8.setTypeface(Typeface.create(font, Typeface.NORMAL));
                alphabetics9.setTypeface(Typeface.create(font, Typeface.NORMAL));
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Pin display, delete button, or OK button invalid", e);
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
    public void onClick (View view) {
        super.onClick(view); // Need to call the super to catch click of end call button

        // If this method was called by virtue of someone invoking a long press, we swallow it
        /*if (mLongPressFlag) {
            mLongPressFlag = false;
            resetPinEntry(getString(R.string.lock_screen_pin_default_display));
            return;
        }*/
        if (mLongPressFlag) {
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
            TextView pinDisplayView = getPinDisplayView();
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
        Log.d(TAG, mPinEntered + " vs " + mPinStored);
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
        if (super.onTouch(v, event)) {
            return true;
        }
        if (!isSpeedDialEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mLongPressFlag) {
                    break;
                }
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(1);
                if (!getPhoneCallActiveFlag() && mHandler == null) { // means there is no pending handler
                    int num = getSpeedDialButtonPressed(v.getId(), -1);
                    if (num != -1) {
                        mHandler = new Handler();
                        mRunnable = new DialerRunnable(this, num);
                        mHandler.postDelayed(
                                mRunnable,
                                getResources().getInteger(R.integer.lock_screen_pin_long_press_delay));
                        mLongPressFlag = false;
                        mLastBtnTouchedNum = num;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler = null;
                    mRunnable = null;
                }
                if (mLongPressFlag) {
                    resetPinEntry(getString(R.string.lock_screen_initiate_call));
                    SetTextInViewRunnable r = new SetTextInViewRunnable(
                            getString(R.string.lock_screen_pin_default_display),
                            getPinDisplayView());
                    Handler h = new Handler();
                    h.postDelayed(r, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));
                }
                break;
            case MotionEvent.ACTION_MOVE:
                // Sole purpose is to prevent speed dial if finger press wanders outside of button

                if (mLongPressFlag) {
                    break;
                }
                // Check if user has left last button
                int[] coord = new int[2];
                Button[] buttons = getKeypadButtons();
                buttons[mLastBtnTouchedNum].getLocationOnScreen(coord);
                Rect r = new Rect(
                        coord[0],
                        coord[1],
                        coord[0] + buttons[mLastBtnTouchedNum].getWidth(),
                        coord[1] + buttons[mLastBtnTouchedNum].getHeight());
                if (!r.contains((int) event.getRawX(), (int) event.getRawY())) { // outside last button
                    if (mHandler != null) {
                        mHandler.removeCallbacks(mRunnable);
                        mHandler = null;
                        mRunnable = null;
                    }
                }
        }
        return false;
    }

    private Button[] getKeypadButtons() {
        Button[] keypadButtons = new Button[10];

        try {
            keypadButtons[0] = (Button) getView(R.id.lock_screen_pin_button_0);
            keypadButtons[1] = (Button) getView(R.id.lock_screen_pin_button_1);
            keypadButtons[2] = (Button) getView(R.id.lock_screen_pin_button_2);
            keypadButtons[3] = (Button) getView(R.id.lock_screen_pin_button_3);
            keypadButtons[4] = (Button) getView(R.id.lock_screen_pin_button_4);
            keypadButtons[5] = (Button) getView(R.id.lock_screen_pin_button_5);
            keypadButtons[6] = (Button) getView(R.id.lock_screen_pin_button_6);
            keypadButtons[7] = (Button) getView(R.id.lock_screen_pin_button_7);
            keypadButtons[8] = (Button) getView(R.id.lock_screen_pin_button_8);
            keypadButtons[9] = (Button) getView(R.id.lock_screen_pin_button_9);
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

    private TextView getPinDisplayView() {
        TextView t;
        try {
            t = (TextView) getView(R.id.lock_screen_pin_display);
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

    private Button getOkButton() {
        Button b;
        try {
            b = (Button) getView(R.id.lock_screen_pin_button_OK);
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

    private Button getDeleteButton() {
        Button b;
        try {
            b = (Button) getView(R.id.lock_screen_pin_button_delete);
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
        TextView pinDisplayView = getPinDisplayView();
        Button deleteButton = getDeleteButton();
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
                getString(R.string.file_lock_screen_type),
                Context.MODE_PRIVATE);

        return sharedPref.getString(getString(R.string.value_lock_screen_passcode), null);

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
                getPinDisplayView());
        Handler h = new Handler();
        h.postDelayed(r, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    /**
     * Method that resets the Pin Entry
     */
    private void resetPinEntry(String displayText) {
        TextView pinDisplayView = getPinDisplayView();
        Button deleteButton = getDeleteButton();
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

    @Override
    int getFragmentLayout() {
        return R.layout.fragment_lock_screen_keypad_pin;
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
            mLongPressFlag = false;
        }
    }

}


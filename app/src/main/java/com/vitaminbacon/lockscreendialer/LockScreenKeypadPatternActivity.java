package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class LockScreenKeypadPatternActivity extends LockScreenActivity
        implements View.OnClickListener, View.OnTouchListener {

    private final static String TAG = "LSPatternActivity";

    // UI elements
/*    private TextView mPinDisplayView;
    private Button mDeleteButton;
    private Button mOkButton;
    private Button[] mkeypadButtons;*/

    // variables relating to the logic of the lockscreen
    private String mPatternStored;
    private String mPatternEntered;
    private boolean mPatternInvalidDisplayFlag;
    private int mNumTries;
    //private Rect[] mPatternDimens;
    private Button[] mPatternBtns;
    //private boolean mIsDrawingPattern;
    //private Button mLastTouchedBtn;
    private int mLastBtnTouchedNum;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate() called.");
        //mLayoutId = R.layout.activity_lock_screen_keypad_pin;  // must be set before calling super
        super.onCreate(savedInstanceState);

        View wrapperView = getWrapperView();

        // Initialize some basic variables
        mNumTries = 0;  // Possibly modified later by onRestoreInstanceState
        mPatternStored = getStoredPattern();
        mPatternInvalidDisplayFlag = false;

        // In case returning to this display from elsewhere, want to reset
        // Will also catch error when there is improper layout
        //Log.d(TAG, "onCreate() calling resetPinEntry()");
        resetPatternEntry(getString(R.string.lock_screen_pin_default_display));

        mPatternBtns = getPatternButtons(wrapperView);
        //mPatternDimens = new Rect[9];

        // Set the onClickListeners to the appropriate views
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);

        for (int i = 0; i < 9; i++) {
            mPatternBtns[i].setOnTouchListener(this);
            try {
                String filename = getString(R.string.key_number_store_prefix_phone) + (i + 1);

                if (sharedPref == null) {
                    Log.w(TAG, "Unable to access shared preferences file "
                            + getString(R.string.speed_dial_preference_file_key) + "; returned null.");
                    continue;
                } else if (sharedPref.getString(filename, null) != null) { //only set the long click where necessary
                    //Log.d(TAG, "Setting long click on key " + i);

                    /*int[] coord = new int[2];
                    mPatternBtns[i].getLocationOnScreen(coord);
                    mPatternDimens[i] = new Rect(
                            coord[0],
                            coord[1],
                            coord[0] + mPatternBtns[i].getWidth(),
                            coord[1] + mPatternBtns[i].getHeight());*/
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Keypad button " + (i + 1) + " is invalid.", e);
                onFatalError();
                return;
            }
        }

        /*try {
            View touchContainer = wrapperView.findViewById(R.id.lock_screen_pattern_touch_area);
            touchContainer.setOnTouchListener(this);
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout does not contain a touch area for this lock screen.", e);
            onFatalError();
        }*/

        if (mPatternStored == null) {
            Log.e(TAG, "Stored pattern is null.");
            onFatalError(); //For now, just exit.  TODO: find good way to handle these errors.
            return;
        }
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

    /*@Override
    public void onClick (View view) {
        super.onClick(view); // Need to call the super to catch click of end call button

        // If this method was called by virtue of someone invoking a long press, we swallow it
        if (mLongPressFlag) {
            mLongPressFlag = false;
            resetPatternEntry(getString(R.string.lock_screen_pin_default_display));
            return;
        }
        int num = getSpeedDialButtonPressed(view.getId(), -1);
        if (num == -1) { //Not a speed dial number
            switch (view.getId()) {
                case R.id.lock_screen_pin_button_OK:
                    if (mPatternEntered.length() != 0){ // We don't care if the OK button was pressed without any PIN entered
                        //wrongPatternEntered();  // Because our functionality will automatically accept a PIN when it is entered, we can assume error
                    }
                    break;
                case R.id.lock_screen_pin_button_delete:
                    //Log.d(TAG, "onClick() calling resetPinEntry() on delete button pressed.");
                    resetPatternEntry(getString(R.string.lock_screen_keypad_pattern_instruction_1));
                    break;
            }
            return;
        }

        mPatternEntered += num;

        //Log.d(TAG, "onClick() -- digit " + num + "entered, pin display tag is " + mPinInvalidDisplayFlag);
        // Display a new "digit" on the text view
        if (!mPatternInvalidDisplayFlag) {// meaning the display is not taken by displaying an invalid pin message
            TextView pinDisplayView = getPinDisplayView(getWrapperView());
            try {
                if (pinDisplayView.getText().toString().
                        equals(getString(R.string.lock_screen_pin_default_display))) {
                    pinDisplayView.setText("*");
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
        if (mPatternEntered.equals(mPatternStored)) {
            onCorrectPasscode();
        }
    }*/

    /**
     * Method that implements a longer-press version of onLongClick. Method works by creating
     * a runnable to be invoked in the future, based on the delay specified in the resources.
     * In the event that the user lifts up from the click before the runnable runs, the runnable
     * is revoked.
     *
     * @param v
     * @param event
     * @return
     */
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Event only happens once at beginning, so good place to initialize this variable
                //mIsDrawingPattern = false;
                try {
                    mHandler = new Handler();
                    mRunnable = new DialerRunnable(this, getSpeedDialButtonPressed(v.getId(), -1));
                    mHandler.postDelayed(
                            mRunnable,
                            getResources().getInteger(R.integer.lock_screen_pattern_long_press_delay));
                    mLastBtnTouchedNum = Integer.getInteger(((Button) v).getText().toString());
                    if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                        Log.e(TAG, "Pattern button contains improper digit");
                        onFatalError();
                    } else {
                        mPatternEntered += mLastBtnTouchedNum;
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Unable to case Button to view to get value.", e);
                    onFatalError();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler = null;
                    mRunnable = null;
                }
                if (mPatternEntered.equals(mPatternStored)) {
                    onCorrectPasscode();
                } else {
                    onWrongPatternEntered();
                }
                break;

            case MotionEvent.ACTION_MOVE:

                // Check if user has left last button
                int index = mLastBtnTouchedNum - 1;
                int[] coord = new int[2];
                mPatternBtns[index].getLocationOnScreen(coord);
                Rect r = new Rect(
                        coord[0],
                        coord[1],
                        coord[0] + mPatternBtns[index].getWidth(),
                        coord[1] + mPatternBtns[index].getHeight());
                if (!r.contains((int) event.getRawX(), (int) event.getRawY())) { // outside last button

                    if (mHandler != null) { // Means first onTouch call outside of button
                        mHandler.removeCallbacks(mRunnable);
                    }

                    // Now check if touch is in other buttons
                    // Brute force, but over 9 elements hardly a problem
                    for (int i = 0; i < 9; i++) {
                        if (i == index) {
                            continue;
                        }
                        mPatternBtns[i].getLocationOnScreen(coord);
                        r = new Rect(
                                coord[0],
                                coord[1],
                                coord[0] + mPatternBtns[i].getWidth(),
                                coord[1] + mPatternBtns[i].getHeight());
                        if (r.contains((int) event.getRawX(), (int) event.getRawY())) {
                            mLastBtnTouchedNum = Integer
                                    .getInteger(mPatternBtns[i].getText().toString());
                            if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                                Log.e(TAG, "Pattern button contains improper digit");
                                onFatalError();
                            } else if (!mPatternEntered.contains(mPatternBtns[i].getText().toString())) {
                                // Makes sure the digit doesn't already exist in the pattern entered
                                mPatternEntered += mLastBtnTouchedNum;
                                break;
                            }
                        }
                    }
                }
        }
        return true;
    }

    private Button[] getPatternButtons(View wrapperView) {
        Button[] patternButtons = new Button[9];

        try {
            patternButtons[0] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_1);
            patternButtons[1] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_2);
            patternButtons[2] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_3);
            patternButtons[3] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_4);
            patternButtons[4] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_5);
            patternButtons[5] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_6);
            patternButtons[6] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_7);
            patternButtons[7] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_8);
            patternButtons[8] = (Button) wrapperView.findViewById(R.id.lock_screen_pattern_button_9);
        } catch (NullPointerException e) {
            Log.e(TAG, "Wrapper view could not be located in this activity.", e);
            onFatalError();  // TODO: find way to gracefully handle these exceptions
            return null;
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible layout used with this activity.", e);
            onFatalError();
            return null;
        }
        return patternButtons;
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

    private String getStoredPattern() {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        return sharedPref.getString(getString(R.string.lock_screen_passcode_value_key), null);

    }


    private void onWrongPatternEntered() {
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
        resetPatternEntry(getString(R.string.lock_screen_wrong_pattern_entered));
        mPatternInvalidDisplayFlag = true;
        mNumTries++;
        SetTextInViewRunnable r = new SetTextInViewRunnable(
                getString(R.string.lock_screen_keypad_pattern_instruction_1),
                getPinDisplayView(getWrapperView()));
        Handler h = new Handler();
        h.postDelayed(r, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    /**
     * Method that resets the Pin Entry
     */
    private void resetPatternEntry(String displayText) {
        TextView pinDisplayView = getPinDisplayView(getWrapperView());
        try {
            pinDisplayView.setText(displayText);
            pinDisplayView.setTransformationMethod(null);
            pinDisplayView.setTextScaleX(1.0f);
        } catch (NullPointerException e) {
            Log.e(TAG, "Incompatible layout with this activity.", e);
            onFatalError();
        }
        //Log.d(TAG, "pinDisplayView text is reset to " + pinDisplayView.getText());
        mPatternEntered = "";
    }

    private void resetPinInvalidDisplayFlag() {
        mPatternInvalidDisplayFlag = false;
    }

    private int getSpeedDialButtonPressed(int id, int defaultReturn) {
        switch (id) {
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

    private int getPinEnteredLength() {
        return mPatternEntered.length();
    }

    @Override
    int getFragmentLayout() {
        return R.layout.fragment_lock_screen_keypad_pin;
    }

    private class SetTextInViewRunnable implements Runnable {
        private String text;
        private TextView view;

        public SetTextInViewRunnable(String t, TextView v) {
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
                //Log.d(TAG, "Runnable has set pinDisplayVyew to " + s);
            }
            resetPinInvalidDisplayFlag();
        }
    }

}


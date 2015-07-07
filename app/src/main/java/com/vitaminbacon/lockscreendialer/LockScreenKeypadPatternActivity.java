package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vitaminbacon.lockscreendialer.helpers.BitmapToViewHelper;
import com.vitaminbacon.lockscreendialer.views.DrawView;


public class LockScreenKeypadPatternActivity extends LockScreenActivity
        implements View.OnClickListener, View.OnTouchListener {

    private final static String TAG = "LSPatternActivity";

    // variables relating to the logic of the lockscreen
    private String mPatternStored;
    private String mPatternEntered;
    private int mNumTries;
    private Button[] mPatternBtns;
    private int mLastBtnTouchedNum;
    private DrawView mPatternDrawView, mTouchDrawView;
    private boolean mPhoneCallInterruptFlag;
    private boolean mDisplayPatternFlag;
    private int mDrawColor;
    private int mButtonColor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate() called.");
        //mLayoutId = R.layout.activity_lock_screen_keypad_pin;  // must be set before calling super
        super.onCreate(savedInstanceState);

        View wrapperView = getContainerView();

        if (wrapperView == null) {
            // Means the super onCreate is shutting down
            return;
        }

        // Initialize some basic variables
        mNumTries = 0;  // Possibly modified later by onRestoreInstanceState
        mPatternStored = getStoredPattern();
        mPatternEntered = "";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDisplayPatternFlag = prefs.getBoolean(getString(R.string.key_enable_pattern_draw), true);
        mDrawColor = prefs.getInt(getString(R.string.key_select_pattern_draw_color),
                getResources().getColor(R.color.green));
        mButtonColor = prefs.getInt(getString(R.string.key_select_pattern_button_pressed_color),
                getResources().getColor(R.color.lava_red));

        // In case returning to this display from elsewhere, want to reset
        // Will also catch error when there is improper layout
        //Log.d(TAG, "onCreate() calling resetPinEntry()");
        resetPatternEntry(getString(R.string.lock_screen_keypad_pattern_instruction_1));

        mPatternBtns = getPatternButtons();
        try {
            mPatternDrawView = (DrawView) getView(R.id.lock_screen_pattern_canvas);
            mTouchDrawView = (DrawView) getView(R.id.lock_screen_touch_canvas);
            TextView patternInstruction =
                    (TextView) getView(R.id.lock_screen_pattern_display);


            String font = prefs.getString(
                    getString(R.string.key_select_lock_screen_fonts),
                    getString(R.string.font_default));
            if (!font.equals(getString(R.string.font_default))) {
                patternInstruction.setTypeface(Typeface.create(font, Typeface.NORMAL));
            }


        } catch (NullPointerException e) {
            Log.e(TAG, "Layout has improper elements for this activity", e);
            onFatalError();
        }

        // Set the onClickListeners to the appropriate views
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);

        for (int i = 0; i < 9; i++) {
            mPatternBtns[i].setOnTouchListener(this);
        }

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

    @Override
    int getFragmentLayout() {
        return R.layout.fragment_lock_screen_pattern;
    }

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
        if (super.onTouch(v, event)) {
            // If consumed by the super, then return
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Check if phone call is active for the interrupt flag logic
                /*if (!getPhoneCallActiveFlag()) {
                    mPhoneCallInterruptFlag = true;
                } else {
                    mPhoneCallInterruptFlag = false;
                }*/
                if (mLongPressFlag == true) {
                    break;
                }

                try {
                    Button b = (Button) v;
                    // Check to see if we set a dialer runnable
                    if (isSpeedDialEnabled()) {
                        SharedPreferences sharedPref = getSharedPreferences(
                                getString(R.string.speed_dial_preference_file_key),
                                Context.MODE_PRIVATE);
                        String filename = getString(R.string.key_number_store_prefix_phone)
                                + getSpeedDialButtonPressed(b.getId(), -1);
                        //Log.d(TAG, "Setting dialer runnable click on key " + b.getText());
                        if (sharedPref.getString(filename, null) != null) { //only set the long click where necessary
                            //Log.d(TAG, "Setting dialer runnable click on key " + b.getText());
                            mHandler = new Handler();
                            mRunnable = new DialerRunnable(this, getSpeedDialButtonPressed(v.getId(), -1));
                            mHandler.postDelayed(
                                    mRunnable,
                                    getResources().getInteger(R.integer.lock_screen_pattern_long_press_delay));
                            Vibrator vibrator =
                                    (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(1);
                        }
                    }

                    // Now handle pattern logic
                    mLastBtnTouchedNum = Integer.parseInt(b.getText().toString());
                    if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                        Log.e(TAG, "Pattern button contains improper digit");
                        onFatalError();
                        return false;
                    } else {
                        mPatternEntered += mLastBtnTouchedNum;
                        // Draw the pattern
                        if (mDisplayPatternFlag) {
                            b.setPressed(true);
                        }
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Unable to cast Button to view to get value.", e);
                    onFatalError();
                } catch (NullPointerException e) {
                    Log.e(TAG, "Shared preference variable had invalid input");
                    onFatalError();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler = null;
                    mRunnable = null;
                }
                if (mDisplayPatternFlag) {
                    mTouchDrawView.clearLines();
                    mTouchDrawView.invalidate();
                }
                if (mPatternEntered.equals(mPatternStored)) {
                    Log.d(TAG, "Correct passcode called");
                    onCorrectPasscode();
                } else {
                    /*if (getPhoneCallActiveFlag() && mPhoneCallInterruptFlag) {
                        // Phone=inactive when pattern started, and phone=active after pattern end
                        onWrongPatternEntered(false);  // No error message*/
                    if (!getPhoneCallActiveFlag() && mLongPressFlag) {
                        onWrongPatternEntered(getString(R.string.lock_screen_initiate_call));
                    } else {
                        onWrongPatternEntered(getString(R.string.lock_screen_wrong_pattern_entered));
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mLongPressFlag) {
                    break;
                }

                // Check if user has left last button
                int index = mLastBtnTouchedNum - 1;
                int[] coord = new int[2];
                Button last = mPatternBtns[index];
                last.getLocationOnScreen(coord);
                Rect r = new Rect(
                        coord[0],
                        coord[1],
                        coord[0] + last.getWidth(),
                        coord[1] + last.getHeight());
                if (!r.contains((int) event.getRawX(), (int) event.getRawY())) { // outside last button
                    if (mHandler != null) {
                        mHandler.removeCallbacks(mRunnable);
                        mHandler = null;
                        mRunnable = null;
                    }
                    // Flag to determine whether to draw a line to user touch
                    boolean drawToTouch = true;

                    // Brute force, but over 9 elements hardly a problem
                    for (int i = 0; i < 9; i++) {
                        if (i == index) { // means it is the "last" button
                            continue;
                        }
                        mPatternBtns[i].getLocationOnScreen(coord);
                        r = new Rect(
                                coord[0],
                                coord[1],
                                coord[0] + mPatternBtns[i].getWidth(),
                                coord[1] + mPatternBtns[i].getHeight());
                        if (r.contains((int) event.getRawX(), (int) event.getRawY())) {
                            Button b = mPatternBtns[i];

                            if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                                Log.e(TAG, "Pattern button contains improper digit");
                                onFatalError();
                                return false;
                            } else if (!mPatternEntered.contains(mPatternBtns[i].getText().toString())) {
                                // Makes sure the digit doesn't already exist in the pattern entered
                                mLastBtnTouchedNum = Integer.parseInt(b.getText().toString());
                                mPatternEntered += mLastBtnTouchedNum;
                                //Log.d(TAG, "Pattern now = " + mPatternEntered);

                                Vibrator vibrator =
                                        (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                vibrator.vibrate(1);

                                if (mDisplayPatternFlag) {
                                    //b.getBackground().mutate();
                                    b.setPressed(true);
                                    Paint p = new Paint();
                                    //p.setColor(getResources().getColor(R.color.green));
                                    p.setColor(mDrawColor);
                                    p.setStrokeWidth(3f);

                                    int[] startCoord = new int[2];
                                    int[] endCoord = new int[2];
                                    last.getLocationOnScreen(startCoord);
                                    b.getLocationOnScreen(endCoord);
                                    mPatternDrawView.addLineWithAbsoluteCoords(
                                            startCoord[0] + last.getWidth() / 2f,
                                            startCoord[1] + last.getHeight() / 2f,
                                            endCoord[0] + b.getWidth() / 2f,
                                            endCoord[1] + b.getHeight() / 2f,
                                            p);
                                    mPatternDrawView.invalidate();
                                    mTouchDrawView.clearLines();
                                    mTouchDrawView.invalidate();
                                    drawToTouch = false;
                                }
                                break;
                            }
                        }
                    }
                    if (mDisplayPatternFlag && drawToTouch) {
                        Paint p = new Paint();
                        p.setColor(mDrawColor);
                        p.setStrokeWidth(3f);

                        int[] startCoord = new int[2];
                        last.getLocationOnScreen(startCoord);
                        mTouchDrawView.clearLines();
                        mTouchDrawView.addLineWithAbsoluteCoords(
                                startCoord[0] + last.getWidth() / 2f,
                                startCoord[1] + last.getHeight() / 2f,
                                event.getRawX(),
                                event.getRawY(),
                                p);
                        mTouchDrawView.invalidate();
                    }
                }
        }
        return true;
    }

    private Button[] getPatternButtons() {
        Button[] patternButtons = new Button[9];
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String font = prefs.getString(
                getString(R.string.key_select_lock_screen_fonts),
                getString(R.string.font_default));

        //((ToggleButton) view).setBackgroundDrawable(sld);

        try {
            patternButtons[0] = (Button) getView(R.id.lock_screen_pattern_button_1);
            patternButtons[1] = (Button) getView(R.id.lock_screen_pattern_button_2);
            patternButtons[2] = (Button) getView(R.id.lock_screen_pattern_button_3);
            patternButtons[3] = (Button) getView(R.id.lock_screen_pattern_button_4);
            patternButtons[4] = (Button) getView(R.id.lock_screen_pattern_button_5);
            patternButtons[5] = (Button) getView(R.id.lock_screen_pattern_button_6);
            patternButtons[6] = (Button) getView(R.id.lock_screen_pattern_button_7);
            patternButtons[7] = (Button) getView(R.id.lock_screen_pattern_button_8);
            patternButtons[8] = (Button) getView(R.id.lock_screen_pattern_button_9);


            for (int i = 0; i < 9; i++) {
                LayerDrawable layerList = (LayerDrawable) getResources()
                        .getDrawable(R.drawable.pattern_button_pressed);
                GradientDrawable shape = (GradientDrawable) layerList
                        .findDrawableByLayerId(R.id.pattern_button_pressed);
                StateListDrawable sld = new StateListDrawable();
                sld.addState(new int[]{android.R.attr.state_pressed}, layerList);
                sld.addState(new int[]{},
                        getResources().getDrawable(R.drawable.pattern_button_normal));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    patternButtons[i].setBackground(sld);
                } else {
                    patternButtons[i].setBackgroundDrawable(sld);
                }

                // We thought mutate was critical here, but it caused a strange error where
                // one of the buttons' color would not be changed.  By commenting out mutate(), the
                // problem goes away...
                //sld.mutate();
                int strokeWidth = (int) BitmapToViewHelper.convertDpToPixel(1, this);
                shape.setStroke(strokeWidth, mButtonColor);

                if (!font.equals(getString(R.string.font_default))) {
                    patternButtons[i].setTypeface(Typeface.create(font, Typeface.NORMAL));
                }
            }

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

    private String getStoredPattern() {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.file_lock_screen_type),
                Context.MODE_PRIVATE);

        return sharedPref.getString(getString(R.string.value_lock_screen_passcode), null);

    }


    private void onWrongPatternEntered(String displayMessage) {
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
        resetPatternEntry(displayMessage);

        mNumTries++;
        mPatternEntered = "";
        mPatternDrawView.clearLines();
        mPatternDrawView.invalidate();
        mTouchDrawView.clearLines();
        mTouchDrawView.invalidate();

        for (int i = 0; i < 9; i++) {
            mPatternBtns[i].setPressed(false);
        }

        try {
            SetTextInViewRunnable r = new SetTextInViewRunnable(
                    getString(R.string.lock_screen_keypad_pattern_instruction_1),
                    (TextView) getView(R.id.lock_screen_pattern_display));
            Handler h = new Handler();
            h.postDelayed(r, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));

        } catch (ClassCastException e) {
            Log.e(TAG, "Layout element of wrong type to implement pattern display", e);
            onFatalError();
        }

    }

    /**
     * Method that resets the Pattern Entry
     */
    private void resetPatternEntry(String displayText) {
        TextView tv = (TextView) getView(R.id.lock_screen_pattern_display);
        try {
            tv.setText(displayText);
        } catch (NullPointerException e) {
            Log.e(TAG, "Incompatible layout with this activity.", e);
            onFatalError();
        }
    }


    private int getSpeedDialButtonPressed(int id, int defaultReturn) {
        switch (id) {
            case R.id.lock_screen_pattern_button_1:
                return 1;
            case R.id.lock_screen_pattern_button_2:
                return 2;
            case R.id.lock_screen_pattern_button_3:
                return 3;
            case R.id.lock_screen_pattern_button_4:
                return 4;
            case R.id.lock_screen_pattern_button_5:
                return 5;
            case R.id.lock_screen_pattern_button_6:
                return 6;
            case R.id.lock_screen_pattern_button_7:
                return 7;
            case R.id.lock_screen_pattern_button_8:
                return 8;
            case R.id.lock_screen_pattern_button_9:
                return 9;
            default:
                return defaultReturn;
        }
    }


    private class SetTextInViewRunnable implements Runnable {
        private String text;
        private TextView view;

        public SetTextInViewRunnable(String t, TextView v) {
            text = t;
            view = v;
        }

        public void run() {
            view.setText(text);
            mLongPressFlag = false;
        }
    }

}


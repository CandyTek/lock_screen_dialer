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
    private boolean mTouchInactiveFlag;
    private int mDrawColor;
    private int mButtonColor;
    private Handler mSetTextInViewHandler;
    private SetTextInViewRunnable mSetTextInViewRunnable;



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
        mTouchInactiveFlag = false;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mDisplayPatternFlag = prefs.getBoolean(getString(R.string.key_enable_pattern_draw), true);
        mDrawColor = prefs.getInt(getString(R.string.key_select_pattern_draw_color),
                getResources().getColor(R.color.green));
        mButtonColor = prefs.getInt(getString(R.string.key_select_pattern_button_pressed_color),
                getResources().getColor(R.color.lava_red));

        // In case returning to this display from elsewhere, want to reset
        // Will also catch error when there is improper layout
        //Log.d(TAG, "onCreate() calling resetPinEntry()");
        resetPatternInstruction(getString(R.string.lock_screen_keypad_pattern_instruction_1));

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
        if (super.onTouch(v, event) || mTouchInactiveFlag) {
            // If consumed by the super, then return
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mLongPressFlag == true) {
                    break;
                }

                try {
                    Button b = (Button) v;
                    setDialerRunnable(
                            getSpeedDialButtonPressed(v.getId(), -1),
                            getResources().getInteger(R.integer.lock_screen_pattern_long_press_delay)
                    );

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
                            b.setTextColor(mButtonColor);
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
                /*if (mHandler != null) {
                    mHandler.removeCallbacks(mRunnable);
                    mHandler = null;
                    mRunnable = null;
                }*/
                disableDialerRunnable();
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

                                //Log.d(TAG, "Pattern now = " + mPatternEntered);

                                Vibrator vibrator =
                                        (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                vibrator.vibrate(1);

                                if (mDisplayPatternFlag) {
                                    //b.getBackground().mutate();
                                    b.setPressed(true);
                                    b.setTextColor(mButtonColor);


                                    int[] startCoord = new int[2];
                                    int[] endCoord = new int[2];
                                    last.getLocationOnScreen(startCoord);
                                    b.getLocationOnScreen(endCoord);

                                    float startX = startCoord[0] + last.getWidth() / 2f;
                                    float startY = startCoord[1] + last.getHeight() / 2f;
                                    float endX = endCoord[0] + b.getWidth() / 2f;
                                    float endY = endCoord[1] + b.getHeight() / 2f;
                                    if (!lineRequiresArc(i + 1, mLastBtnTouchedNum)) {
                                        // Draw a line
                                        Paint p = new Paint();
                                        p.setColor(mDrawColor);
                                        p.setStrokeWidth(6f);
                                        mPatternDrawView.addLineWithAbsoluteCoords(
                                                startX,
                                                startY,
                                                endX,
                                                endY,
                                                p);
                                    } else {
                                        // Now we must draw an arc
                                        drawArc(mLastBtnTouchedNum, i + 1, startX, startY, endX, endY);
                                    }
                                    mPatternDrawView.invalidate();
                                    mTouchDrawView.clearLines();
                                    mTouchDrawView.invalidate();
                                    drawToTouch = false;
                                    // update the last touched button and add the button to the pattern
                                    mLastBtnTouchedNum = Integer.parseInt(b.getText().toString());
                                    mPatternEntered += mLastBtnTouchedNum;
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


    private void onWrongPatternEntered(final String displayMessage) {
        int delay;
        final String message;
        switch (mNumTries / 3) {
            case 0:  // meaning there have been less than 3 tries
                message = displayMessage;
                delay = 0;
                break;
            case 1:
                delay = getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay_begin);
                message = getString(R.string.lock_screen_wrong_entry_3_times);
                break;
            case 2: // meaning there have been at least 6 attempts
                delay = getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay_plus);
                message = getString(R.string.lock_screen_wrong_entry_6_times);
                break;
            default: // many many tries
                delay = getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay_max);
                message = getString(R.string.lock_screen_wrong_entry_max_times);
        }
        mNumTries++;
        mPatternEntered = "";
        resetPatternInstruction(message);
        mTouchInactiveFlag = true;
        if (mSetTextInViewHandler != null && mSetTextInViewRunnable != null) {
            mSetTextInViewHandler.removeCallbacks(mSetTextInViewRunnable);
        }
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                mPatternDrawView.clearLines();
                mPatternDrawView.invalidate();
                mTouchDrawView.clearLines();
                mTouchDrawView.invalidate();
                mTouchInactiveFlag = false;


                for (int i = 0; i < 9; i++) {
                    mPatternBtns[i].setPressed(false);
                    mPatternBtns[i].setTextColor(getResources().getColor(R.color.white));
                }
                resetPatternInstruction(message);
                try {
                    mSetTextInViewRunnable = new SetTextInViewRunnable(
                            getString(R.string.lock_screen_keypad_pattern_instruction_1),
                            (TextView) getView(R.id.lock_screen_pattern_display));
                    if (mSetTextInViewHandler == null) {
                        mSetTextInViewHandler = new Handler();
                    }
                    mSetTextInViewHandler.postDelayed(mSetTextInViewRunnable, getResources().getInteger(R.integer.lock_screen_pin_wrong_entry_delay));

                } catch (ClassCastException e) {
                    Log.e(TAG, "Layout element of wrong type to implement pattern display", e);
                    onFatalError();
                }
            }
        };
        handler.postDelayed(runnable, delay);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);

    }

    /**
     * Method that resets the Pattern Entry
     */
    private void resetPatternInstruction(String displayText) {
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

    /**
     * Draws an arc between start and end.  Integers a and b represent the digits touched, with "a"
     * first
     *
     * @param a
     * @param b
     * @param startX
     * @param startY
     * @param endX
     * @param endY
     */
    private void drawArc(int a, int b, float startX, float startY, float endX, float endY) {
        float left, top, right, bottom, startAngle, sweepAngle;
        float rotation = 0;
        // the dimensions of the oval's sides
        left = startX < endX ? startX : endX;
        right = endX > startX ? endX : startX;
        top = startY < endY ? startY : endY;
        bottom = endY > startY ? endY : startY;

        // Need to modify the width of the oval to suit the way the arc will be drawn
        int diff = Math.abs(a - b);
        boolean isRotatedArc = false;  // Flag so right function is called later
        if (diff == 2) {
            // Make horizontal arc
            top -= getSpaceAvailableY();
            bottom += getSpaceAvailableY();
        } else if (diff == 6) {
            // Make vertical arc
            left -= getSpaceAvailableX();
            right += getSpaceAvailableX();
        } else {
            // Make diagonal arc, needing rotated oval!
            isRotatedArc = true;
            // Get dimensions of the RectF
            float height = (float) Math.sqrt((left - right) * (left - right)
                    + (top - bottom) * (top - bottom));
            int width = mPatternBtns[5].getWidth(); // add some padding
            int[] centerCoords = new int[2];
            mPatternBtns[4].getLocationOnScreen(centerCoords); // the center button
            int diagPadding = getResources()
                    .getInteger(R.integer.pattern_diagonal_drawing_padding);
            // Reassign ltrb to be a column in the middle
            rotation = getRotation(a, b, right - left, bottom - top);
            left = centerCoords[0] - diagPadding;
            right = centerCoords[0] + width + diagPadding;
            top = centerCoords[1] - height / 2 + width / 2;
            bottom = centerCoords[1] + height / 2 + width / 2;
        }

        startAngle = getStartAngle(a, b);
        sweepAngle = getSweepAngle(a, b);

        Log.d(TAG, "left = " + left + " top = " + top + " right = "
                + right + " bottom = " + bottom + " startAngle = "
                + startAngle + " sweepAngle = " + sweepAngle);

        if (startAngle != -1 && sweepAngle != -1) {
            Paint p = new Paint();
            p.setColor(mDrawColor);
            p.setStrokeWidth(6f);
            p.setAntiAlias(true);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStyle(Paint.Style.STROKE);
            if (!isRotatedArc) {
                mPatternDrawView
                        .addArcWithAbsoluteCoords(left, top, right, bottom,
                                startAngle, sweepAngle, false, p);
            } else {
                mPatternDrawView
                        .addRotatedArcWithAbsoluteCoords(left, top,
                                right, bottom, rotation, startAngle,
                                sweepAngle, false, p);
            }
        } else {
            Log.e(TAG, "Error drawing arc; startAngle or sweepAngle invalid");
        }
    }

    /**
     * Returns true if, based on a square 9 digit keypad, int a and b requires an arc to draw a line
     * between them without traversing another digit.
     *
     * @param a
     * @param b
     * @return
     */
    private boolean lineRequiresArc(int a, int b) {
        int difference = Math.abs(a - b);
        switch (difference) {
            case 6:
                return true;
            case 4:
                if (a + b != 10) {
                    return false;
                }
                // Continue on, must be 3 & 7 so return true!
            case 8:
                return true;
            case 2:
                if ((a % 3 == 1 && b % 3 == 0) || (b % 3 == 1 && a % 3 == 0)) {
                    return true;
                }
                break;
        }
        return false;
    }

    /**
     * Method assumes that an arc is appropriate already
     *
     * @param a
     * @param b
     * @return
     */
    private float getStartAngle(int a, int b) {
        int difference = Math.abs(a - b);
        switch (difference) {
            case 2:
                if (a < b) {
                    return 180;
                } else {
                    return 0;
                }

            case 6:
                if (a < b) {
                    return 270;
                } else {
                    return 90;
                }

            case 4:
                if (a + b == 10) {
                    if (a < b) {
                        return 315;
                    } else {
                        return 135;
                    }

                }
                break;
            case 8:
                if (a < b) {
                    return 225;
                } else {
                    return 45;
                }
        }
        return -1;
    }

    private float getSweepAngle(int a, int b) {
        int difference = Math.abs(a - b);
        switch (difference) {
            case 2:
                if (a < b) {
                    // Note: Samsung Galaxy S4 exhibited strange error if we tried to put the arc
                    // b/t 7 and 9 below the digits in that it would continue to display the line
                    // after it was cleared.  This implementation is therefore not ideal, but
                    // necessary unless we want to go obscure bug chasing on what appears to be
                    // one device
                    return 180;
                } else {
                    return -180;
                }

            case 6:
                if ((a < b && (a == 1 || a == 2)) || (b < a && a == 9)) {
                    return -180;
                } else {
                    return 180;
                }

            case 4:
                if (a + b != 10) {
                    break;
                }
            case 8:
                return 180;
        }
        return -1;
    }

    /**
     * Returns oval rotation based on configuring the dominating length of the oval in the y direction
     *
     * @param a
     * @param b
     * @return
     */
    private float getRotation(int a, int b, float width, float height) {

        int difference = Math.abs(a - b);
        int multiplier;
        switch (difference) {
            case 8:
                //return -45;
                multiplier = -1;
                break;
            case 4:
                if (a + b == 10) {
                    //return 45;
                    multiplier = 1;
                    break;
                }
            default:
                return 0;
        }

        float returnValue = (float) Math.toDegrees(Math.atan(width / height)) * multiplier;
        Log.d(TAG, "rotation is " + returnValue);
        return returnValue;

    }

    private float getSpaceAvailableX() {
        int margin = (int) getResources().getDimension(R.dimen.pattern_buttons_layout_margin);
        int width = mPatternBtns[0].getWidth();
        int colSpacer = getView(R.id.lock_screen_pattern_col_spacer).getWidth();
        return margin + width / 2 + colSpacer / 2;
    }

    private float getSpaceAvailableY() {
        int margin = (int) getResources().getDimension(R.dimen.pattern_buttons_layout_margin);
        int height = mPatternBtns[0].getHeight();
        int rowSpacer = getView(R.id.lock_screen_pattern_row_spacer).getHeight();
        return margin + height / 2 + rowSpacer / 2;
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


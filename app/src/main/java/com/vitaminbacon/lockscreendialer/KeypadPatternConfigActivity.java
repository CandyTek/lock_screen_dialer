package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vitaminbacon.lockscreendialer.views.DrawView;


public class KeypadPatternConfigActivity extends Activity implements View.OnTouchListener {

    private static final String TAG = "KeypadPatternConfig";
    private TextView mKeyPadEntryInstructions;
    private Button[] mPatternBtns;
    private DrawView mPatternDrawView, mTouchDrawView;
    private int mLastBtnTouchedNum;
    private String mPatternEntered, mPatternStored;
    private boolean mTouchInactiveFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keypad_pattern_config);
        mTouchInactiveFlag = false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mKeyPadEntryInstructions = (TextView) this.findViewById(R.id.keypad_pin_config_instruction);
        mPatternBtns = getPatternButtons();
        mPatternDrawView = (DrawView) this.findViewById(R.id.lock_screen_pattern_canvas);
        mTouchDrawView = (DrawView) this.findViewById(R.id.lock_screen_touch_canvas);

        for (int i = 0; i < 9; i++) {
            mPatternBtns[i].setOnTouchListener(this);
        }

        setActivityToFirstState(getString(R.string.lock_screen_keypad_pattern_instruction_1));  // calls method to ensure that entry onResume is always in first state
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_keypad_pattern_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        super.onBackPressed();
    }

    /**
     * Sets the activity to the first state
     */
    private void setActivityToFirstState(String text) {
        mKeyPadEntryInstructions.setText(text);
        mPatternEntered = "";  //Can test for this to see if Activity is in re-enter PIN state.
        mPatternStored = null;
        resetPatternButtons();
        mPatternDrawView.clearLines();
        mPatternDrawView.invalidate();
        mTouchDrawView.clearLines();
        mTouchDrawView.invalidate();
    }

    /**
     * Sets the activity to the second state
     *
     * @param
     */
    private void setActivityToSecondState(String pattern) {
        mKeyPadEntryInstructions.setText(getString(R.string.keypad_pattern_config_instructions_2));
        mPatternStored = pattern;
        mPatternEntered = "";
        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            public void run() {
                resetPatternButtons();
                mPatternDrawView.clearLines();
                mPatternDrawView.invalidate();
                mTouchDrawView.clearLines();
                mTouchDrawView.invalidate();
                mTouchInactiveFlag = false;
            }
        };
        handler.postDelayed(
                runnable,
                getResources().getInteger(R.integer.lock_screen_pattern_confirm_delay));
        mTouchInactiveFlag = true;

    }

    private Button[] getPatternButtons() {
        Button[] patternButtons = new Button[9];

        try {
            patternButtons[0] = (Button) findViewById(R.id.lock_screen_pattern_button_1);
            patternButtons[1] = (Button) findViewById(R.id.lock_screen_pattern_button_2);
            patternButtons[2] = (Button) findViewById(R.id.lock_screen_pattern_button_3);
            patternButtons[3] = (Button) findViewById(R.id.lock_screen_pattern_button_4);
            patternButtons[4] = (Button) findViewById(R.id.lock_screen_pattern_button_5);
            patternButtons[5] = (Button) findViewById(R.id.lock_screen_pattern_button_6);
            patternButtons[6] = (Button) findViewById(R.id.lock_screen_pattern_button_7);
            patternButtons[7] = (Button) findViewById(R.id.lock_screen_pattern_button_8);
            patternButtons[8] = (Button) findViewById(R.id.lock_screen_pattern_button_9);
        } catch (NullPointerException e) {
            Log.e(TAG, "Wrapper view could not be located in this activity.", e);
            finish();
            return null;
        } catch (ClassCastException e) {
            Log.e(TAG, "Incompatible layout used with this activity.", e);
            finish();
            return null;
        }
        return patternButtons;
    }

    private void resetPatternButtons() {
        if (mPatternBtns == null) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            mPatternBtns[i].setPressed(false);
            mPatternBtns[i].setTextColor(getResources().getColor(R.color.white));
        }
    }

    public boolean onTouch(View v, MotionEvent event) {

        if (mTouchInactiveFlag) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Event only happens once at beginning, so good place to initialize this variable
                //mIsDrawingPattern = false;
                try {
                    Button b = (Button) v;
                    mLastBtnTouchedNum = Integer.parseInt(b.getText().toString());
                    if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                        Log.e(TAG, "Pattern button contains improper digit");
                        finish();
                        return false;
                    } else {
                        mPatternEntered += mLastBtnTouchedNum;
                        b.setPressed(true);
                        b.setTextColor(getResources().getColor(R.color.lava_red));
                        //Log.d(TAG, "Pattern now = " + mPatternEntered);
                        Vibrator vibrator =
                                (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        vibrator.vibrate(1);
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Unable to case Button to view to get value.", e);
                    finish();
                    return false;
                }
                break;

            case MotionEvent.ACTION_UP:
                onPatternSubmitted();
                mTouchDrawView.clearLines();
                mTouchDrawView.invalidate();
                break;

            case MotionEvent.ACTION_MOVE:

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
                            // Outside last button AND inside new button
                            /*mLastBtnTouchedNum = Integer
                                    .getInteger(mPatternBtns[i].getText().toString());*/
                            Button b = mPatternBtns[i];

                            if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                                Log.e(TAG, "Pattern button contains improper digit");
                                finish();
                                return false;
                            } else if (!mPatternEntered.contains(mPatternBtns[i].getText().toString())) {
                                //Log.d(TAG, "Pattern now = " + mPatternEntered);
                                b.setPressed(true);
                                b.setTextColor(getResources().getColor(R.color.lava_red));

                                Vibrator vibrator =
                                        (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                vibrator.vibrate(1);

                                int[] startCoord = new int[2];
                                int[] endCoord = new int[2];
                                last.getLocationOnScreen(startCoord);
                                b.getLocationOnScreen(endCoord);

                                float startX = startCoord[0] + last.getWidth() / 2f;
                                float startY = startCoord[1] + last.getHeight() / 2f;
                                float endX = endCoord[0] + b.getWidth() / 2f;
                                float endY = endCoord[1] + b.getHeight() / 2f;

                                if (!lineRequiresArc(i + 1, mLastBtnTouchedNum)) {
                                    // draw a line
                                    Paint p = new Paint();
                                    p.setColor(getResources().getColor(R.color.green));
                                    p.setStrokeWidth(6f);
                                    mPatternDrawView
                                            .addLineWithAbsoluteCoords(startX, startY, endX, endY, p);
                                } else {
                                    // draw an arc
                                    float left, top, right, bottom, startAngle, sweepAngle;
                                    float rotation = 0;
                                    // the dimensions of the oval's sides
                                    left = startX < endX ? startX : endX;
                                    right = endX > startX ? endX : startX;
                                    top = startY < endY ? startY : endY;
                                    bottom = endY > startY ? endY : startY;

                                    // Need to modify the width of the oval to suit the way the arc will be drawn
                                    int diff = Math.abs(mLastBtnTouchedNum - (i + 1));
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
                                        rotation = getRotation(mLastBtnTouchedNum, i + 1,
                                                right - left, bottom - top);
                                        left = centerCoords[0] - diagPadding;
                                        right = centerCoords[0] + width + diagPadding;
                                        top = centerCoords[1] - height / 2 + width / 2;
                                        bottom = centerCoords[1] + height / 2 + width / 2;
                                    }

                                    startAngle = getStartAngle(mLastBtnTouchedNum, i + 1);
                                    sweepAngle = getSweepAngle(mLastBtnTouchedNum, i + 1);

                                    Log.d(TAG, "left = " + left + " top = " + top + " right = "
                                            + right + " bottom = " + bottom + " startAngle = "
                                            + startAngle + " sweepAngle = " + sweepAngle);

                                    if (startAngle != -1 && sweepAngle != -1) {
                                        Paint p = new Paint();
                                        p.setColor(getResources().getColor(R.color.green));
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
                                    /*float radius = 20;
                                    int startAngle = (int) (180 / Math.PI * Math.atan2(endCoord[1]
                                            - startCoord[1], endCoord[0] - startCoord[0]));
                                    final RectF oval = new RectF();
                                    oval.set(startCoord[0] - radius, startCoord[1] - radius,
                                            startCoord[0] + radius, startCoord[1] + radius);
                                    Path myPath = new Path();
                                    myPath.arcTo(oval, startAngle, -(float) sweepAngle, true);*/
                                }
                                mPatternDrawView.invalidate();
                                mTouchDrawView.clearLines();
                                mTouchDrawView.invalidate();
                                drawToTouch = false;
                                // update the last touched button and add the button to the pattern
                                mLastBtnTouchedNum = Integer.parseInt(b.getText().toString());
                                mPatternEntered += mLastBtnTouchedNum;
                                break;
                            }
                        }
                    }
                    if (drawToTouch) {
                        Paint p = new Paint();
                        p.setColor(getResources().getColor(R.color.lava_red));
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
                        /*Log.d(TAG, "(" + (startCoord[0] + last.getWidth() / 2f)
                                + ", " + (startCoord[1] + last.getHeight() / 2f)
                                + ") --> (" + event.getRawX() + ", " + event.getRawY() + ")");*/
                    }
                }
        }
        return true;
    }

    private void onPatternSubmitted() {
        if (mPatternStored == null) { // We are in the first state
            if (mPatternEntered.length() < 3) { // Pattern too short
                setActivityToFirstState(getString(R.string.lock_screen_pattern_entry_error_too_short));
                SetTextInViewRunnable r = new SetTextInViewRunnable(
                        getString(R.string.lock_screen_keypad_pattern_instruction_1),
                        mKeyPadEntryInstructions);
                Handler h = new Handler();
                h.postDelayed(r, getResources()
                        .getInteger(R.integer.lock_screen_pattern_wrong_entry_delay));
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(200);
                return;
            }
            setActivityToSecondState(mPatternEntered);
        } else { // We are in the second state
            if (mPatternStored.equals(mPatternEntered)) { // pattern is good
                SharedPreferences sharedPref = this.getSharedPreferences(
                        getString(R.string.file_lock_screen_type),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                // Set the preferences to indicate Keypad Pattern is the type of entry
                editor.putString(
                        getString(R.string.key_lock_screen_type),
                        getString(R.string.value_lock_screen_type_keypad_pattern));
                // Store the PIN
                editor.putString(
                        getString(R.string.value_lock_screen_passcode),
                        mPatternEntered);
                editor.commit();
                Intent returnIntent = new Intent();
                setResult(RESULT_OK, returnIntent);
                finish();
                return;
            } else {
                setActivityToFirstState(getString(R.string.lock_screen_pattern_entry_not_matching));
                SetTextInViewRunnable r = new SetTextInViewRunnable(
                        getString(R.string.lock_screen_keypad_pattern_instruction_1),
                        mKeyPadEntryInstructions);
                Handler h = new Handler();
                h.postDelayed(r, getResources()
                        .getInteger(R.integer.lock_screen_pattern_wrong_entry_delay));
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(200);
                return;
            }

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
        int colSpacer = findViewById(R.id.lock_screen_config_col_spacer).getWidth();
        return margin + width / 2 + colSpacer / 2;
    }

    private float getSpaceAvailableY() {
        int margin = (int) getResources().getDimension(R.dimen.pattern_buttons_layout_margin);
        int height = mPatternBtns[0].getHeight();
        int rowSpacer = findViewById(R.id.lock_screen_config_row_spacer).getHeight();
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
        }
    }
}

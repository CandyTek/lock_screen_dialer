package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.vitaminbacon.lockscreendialer.helpers.DrawView;


public class KeypadPatternConfigActivity extends ActionBarActivity implements View.OnTouchListener {

    private static final String TAG = "KeypadPatternConfig";
    private TextView mKeyPadEntryInstructions;
    private Button[] mPatternBtns;
    private DrawView mPatternDrawView, mTouchDrawView;
    private int mLastBtnTouchedNum;
    private String mPatternEntered, mPatternStored;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keypad_pattern_config);
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
        resetPatternButtons();
        mPatternDrawView.clearLines();
        mPatternDrawView.invalidate();
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
        }
    }

    public boolean onTouch(View v, MotionEvent event) {

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
                        Log.d(TAG, "Pattern now = " + mPatternEntered);
                    }
                } catch (ClassCastException e) {
                    Log.e(TAG, "Unable to case Button to view to get value.", e);
                    finish();
                    return false;
                }
                break;

            case MotionEvent.ACTION_UP:
                onPatternSubmitted();

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
                            /*mLastBtnTouchedNum = Integer
                                    .getInteger(mPatternBtns[i].getText().toString());*/
                            Button b = (Button) mPatternBtns[i];

                            if (mLastBtnTouchedNum < 1 || mLastBtnTouchedNum > 9) {
                                Log.e(TAG, "Pattern button contains improper digit");
                                finish();
                                return false;
                            } else if (!mPatternEntered.contains(mPatternBtns[i].getText().toString())) {
                                // Makes sure the digit doesn't already exist in the pattern entered
                                mLastBtnTouchedNum = Integer.parseInt(b.getText().toString());
                                mPatternEntered += mLastBtnTouchedNum;
                                //Log.d(TAG, "Pattern now = " + mPatternEntered);
                                b.setPressed(true);

                                Paint p = new Paint();
                                p.setColor(getResources().getColor(R.color.green));
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
                        Log.d(TAG, "(" + (startCoord[0] + last.getWidth() / 2f)
                                + ", " + (startCoord[1] + last.getHeight() / 2f)
                                + ") --> (" + event.getRawX() + ", " + event.getRawY() + ")");
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
                return;
            }
            setActivityToSecondState(mPatternEntered);
        } else { // We are in the second state
            if (mPatternStored.equals(mPatternEntered)) { // pattern is good
                SharedPreferences sharedPref = this.getSharedPreferences(
                        getString(R.string.lock_screen_type_file_key),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                // Set the preferences to indicate Keypad Pattern is the type of entry
                editor.putString(
                        getString(R.string.lock_screen_type_value_key),
                        getString(R.string.lock_screen_type_value_keypad_pattern));
                // Store the PIN
                editor.putString(
                        getString(R.string.lock_screen_passcode_value_key),
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
                return;
            }

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
        }
    }
}

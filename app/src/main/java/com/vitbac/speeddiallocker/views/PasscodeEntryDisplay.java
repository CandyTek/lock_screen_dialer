package com.vitbac.speeddiallocker.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/7/15.
 */
public class PasscodeEntryDisplay extends RelativeLayout implements View.OnClickListener {

    private static final String TAG = "PasscodeEntryDisplay";
    private static final String DEF_INSTRUCTION = "Enter passcode";
    private static final String DEF_WRONG_PASSCODE = "Wrong passcode";
    private static final String DEF_LOCKOUT_1 = "Invalid: phase 1 lockout";
    private static final String DEF_LOCKOUT_2 = "Invalid: phase 2 lockout";
    private static final String DEF_LOCKOUT_3 = "Invalid: phase 3 lockout";
    private static final int DEF_LOCKOUT_1_DELAY = 5000;
    private static final int DEF_LOCKOUT_2_DELAY = 10000;
    private static final int DEF_LOCKOUT_3_DELAY = 15000;
    private static final int DEF_LOCKOUT_LEVEL_THRESHOLD = 3;
    private static final int DEF_DISPLAY_TIME = 3000;

    private TextView mTextView;
    private Button mDeleteButton;

    private String mInstructionText, mWrongPasscodeText, mLockout1Text, mLockout2Text, mLockout3Text;
    private int mDisplayTime, mLockout1Delay, mLockout2Delay, mLockout3Delay;
    private int mNumTries, mLockoutThreshold;
    private Handler mHandler;
    private Runnable mRunnable;
    private OnLockoutListener mLockoutListener;
    private OnDeletePressed mDeleteListener;

    public PasscodeEntryDisplay(Context context) {
        super(context);
        init();
    }

    public PasscodeEntryDisplay(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray attributeArray = context.obtainStyledAttributes(attrs,
                R.styleable.PasscodeEntryDisplay, 0, 0);
        mInstructionText = attributeArray.getString(R.styleable.PasscodeEntryDisplay_instruction);
        mWrongPasscodeText = attributeArray.getString(R.styleable.PasscodeEntryDisplay_wrongPasscode);
        mLockout1Text = attributeArray.getString(R.styleable.PasscodeEntryDisplay_lockout1Text);
        mLockout2Text = attributeArray.getString(R.styleable.PasscodeEntryDisplay_lockout2Text);
        mLockout3Text = attributeArray.getString(R.styleable.PasscodeEntryDisplay_lockout3Text);
        mLockoutThreshold =
                attributeArray.getInt(R.styleable.PasscodeEntryDisplay_lockoutLevelThreshold,
                        DEF_LOCKOUT_LEVEL_THRESHOLD);
        mLockout1Delay = attributeArray.getInt(R.styleable.PasscodeEntryDisplay_lockout1Delay,
                DEF_LOCKOUT_1_DELAY);
        mLockout2Delay = attributeArray.getInt(R.styleable.PasscodeEntryDisplay_lockout2Delay,
                DEF_LOCKOUT_2_DELAY);
        mLockout3Delay = attributeArray.getInt(R.styleable.PasscodeEntryDisplay_lockout3Delay,
                DEF_LOCKOUT_3_DELAY);
        mDisplayTime = attributeArray.getInt(R.styleable.PasscodeEntryDisplay_displayTime,
                DEF_DISPLAY_TIME);
        init();
        if (attributeArray.getBoolean(R.styleable.PasscodeEntryDisplay_displayDeleteButton, false)) {
            // If visibility is set to View.GONE, then we never address it and it stays gone.
            mDeleteButton.setVisibility(View.INVISIBLE);
        }
    }

    private void init(){

        if (mInstructionText == null) {
            mInstructionText = DEF_INSTRUCTION;
        }
        if (mWrongPasscodeText == null) {
            mWrongPasscodeText = DEF_WRONG_PASSCODE;
        }
        if (mLockout1Text == null) {
            mLockout1Text = DEF_LOCKOUT_1;
        }
        if (mLockout2Text == null) {
            mLockout2Text = DEF_LOCKOUT_2;
        }
        if (mLockout3Text == null) {
            mLockout3Text = DEF_LOCKOUT_3;
        }

        inflate(getContext(), R.layout.view_passcode_display, this);
        mTextView = (TextView) findViewById(R.id.passcode_entry_display_textview);
        mDeleteButton = (Button) findViewById(R.id.passcode_entry_display_delete_button);
        mTextView.setText(mInstructionText);
        mDeleteButton.setOnClickListener(this);
        mNumTries = 0;
    }

    public interface OnLockoutListener {
        void onLockout(int delay);
    }

    public interface OnDeletePressed {
        void onDeletePressed();
    }

    public void setOnLockoutListener(OnLockoutListener listener) {
        mLockoutListener = listener;
    }

    public void setOnDeletePressedListener(OnDeletePressed listener) {
        mDeleteListener = listener;
    }

    public void setInstructionText(String text) {
        mInstructionText = text;
    }

    public void displayInstructionText() {
        if (mInstructionText != null) {
            mTextView.setText(mInstructionText);
        } else {
            mTextView.setText("");
        }
    }

    public void setWrongPasscodeText(String text) {
        mWrongPasscodeText = text;
    }

    public void setLockoutText(String text, int i) {
        switch (i) {
            case 1:
                mLockout1Text = text;
                break;
            case 2:
                mLockout2Text = text;
                break;
            case 3:
                mLockout3Text = text;
                break;
        }
    }

    public String getInstructionText() {
        return mInstructionText;
    }

    public String getWrongPasscodeText() {
        return mWrongPasscodeText;
    }

    public String getLockoutText(int i) {
        switch (i) {
            case 1:
                return mLockout1Text;
            case 2:
                return mLockout2Text;
            case 3:
                return mLockout3Text;
        }
        return null;
    }


    /**
     * Returns true if no delay has been imposed on the display
     * @return
     */
    public boolean wrongEntry() {
        int delay;
        final String message;
        boolean isDelayed = true;

        switch (mNumTries / mLockoutThreshold) {
            case 0:  // less tries than the threshold
                message = mWrongPasscodeText;
                delay = mDisplayTime;
                isDelayed = false;
                break;
            case 1: // raised to level 1
                Log.d(TAG, "lockout delay level 1");
                delay = mLockout1Delay;
                message = mLockout1Text;
                if (mLockoutListener != null) {
                    mLockoutListener.onLockout(delay);
                }
                break;
            case 2:  // raised to level 2
                delay = mLockout2Delay;
                message = mLockout2Text;
                if (mLockoutListener != null) {
                    mLockoutListener.onLockout(delay);
                }
                break;
            default: // raised to level 3
                delay = mLockout3Delay;
                message = mLockout3Text;
                if (mLockoutListener != null) {
                    mLockoutListener.onLockout(delay);
                }
        }
        mNumTries++;
        displayMessage(message, delay);

        return isDelayed;
    }

    public void displayMessage(String message) {
        displayMessage(message, mDisplayTime);
    }

    public void displayMessage(String message, int delay) {
        clearTransformationMethod();
        mTextView.setText(message);
        // Hide the delete button if necessary
        hideDeleteButton();

        // Clear any pending runnables
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        mHandler = new Handler();
        mRunnable = new Runnable() {
            public void run() {
                // Only set the text if we aren't typing in a password!
                if (mTextView.getTransformationMethod() == null) {
                    mTextView.setText(mInstructionText);
                }
            }
        };
        mHandler.postDelayed(mRunnable, delay);
    }

    public void setPasscodeText(String text) {
        mTextView.setTransformationMethod(new PasswordTransformationMethod());  // Turns the text to dots
        mTextView.setTextScaleX(1.2f);
        mTextView.setText(text);
        // Show the delete button if necessary
        showDeleteButton();
    }

    private void clearTransformationMethod() {
        if (mTextView.getTransformationMethod() == null) {
            return;
        }
        mTextView.setTransformationMethod(null);  // Turns the text to dots
        mTextView.setTextScaleX(1.0f);
    }

    public void setTypeface(Typeface typeface) {
        mTextView.setTypeface(typeface);
        mDeleteButton.setTypeface(typeface);
    }

    public void onClick (View view) {
        if (view.getId() == mDeleteButton.getId()) {
            backspace();
            if (mDeleteListener != null) {
                mDeleteListener.onDeletePressed();
            }
        }
    }

    public void backspace() {
        String text = (String) mTextView.getText();
        if (text != null && text.length() > 0) {
            mTextView.setText(text.substring(0, text.length()-1));

            // Now some clean up if the length of the text is now 0;
            if (text.length() == 1) {
                hideDeleteButton();
                mTextView.setText(mInstructionText);
                clearTransformationMethod();
            }
        }
    }

    public void setMaxLines(int lines) {
        mTextView.setMaxLines(lines);
    }

    public void setTextColor(int color) {
        mTextView.setTextColor(color);
    }

    public void setShadowLayer (float radius, float dx, float dy, int color) {
        mTextView.setShadowLayer(radius, dx, dy, color);
    }

    public void displayDeleteButton(boolean display) {
        // Display it depending on whether a passcode is being entered
        if (mTextView.getTransformationMethod() == null) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        } else {
            mDeleteButton.setVisibility(View.VISIBLE);
        }
    }



    private void hideDeleteButton() {
        if (mDeleteButton.getVisibility() == View.VISIBLE) {
            mDeleteButton.setVisibility(View.INVISIBLE);
        }
    }

    private void showDeleteButton() {
        if (mDeleteButton.getVisibility() == View.INVISIBLE) {
            mDeleteButton.setVisibility(View.VISIBLE);
        }
    }

}

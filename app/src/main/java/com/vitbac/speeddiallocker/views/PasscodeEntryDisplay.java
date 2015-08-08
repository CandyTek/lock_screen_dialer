package com.vitbac.speeddiallocker.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/7/15.
 */
public class PasscodeEntryDisplay extends TextView {

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

    private String mInstructionText, mWrongPasscodeText, mLockout1Text, mLockout2Text, mLockout3Text;
    private int mDisplayTime, mLockout1Delay, mLockout2Delay, mLockout3Delay;
    private int mNumTries, mLockoutThreshold;
    private Handler mHandler;
    private Runnable mRunnable;
    private OnLockoutListener mListener;

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

        setText(mInstructionText);
        mNumTries = 0;
    }

    public interface OnLockoutListener {
        void onLockout(int delay);
    }

    public void setOnLockoutListener(OnLockoutListener listener) {
        mListener = listener;
    }
    public void setInstructionText(String text) {
        mInstructionText = text;
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

    /*public void displayInstructionText() {
        setText(mInstructionText);
    }

    public void displayWrongPasscodeText() {
        setText(mWrongPasscodeText);
    }

    public void displayLockoutText(int i) {
        String text = "";
        switch (i) {
            case 1:
                text = mLockout1Text;
                break;
            case 2:
                text = mLockout2Text;
                break;
            case 3:
                text = mLockout3Text;
                break;
        }
        setText(text);
    }*/

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
                if (mListener != null) {
                    mListener.onLockout(delay);
                }
                break;
            case 2:  // raised to level 2
                delay = mLockout2Delay;
                message = mLockout2Text;
                if (mListener != null) {
                    mListener.onLockout(delay);
                }
                break;
            default: // raised to level 3
                delay = mLockout3Delay;
                message = mLockout3Text;
                if (mListener != null) {
                    mListener.onLockout(delay);
                }
        }
        mNumTries++;
        displayMessage(message, delay);

        return isDelayed;
    }

    public void displayMessage(String message) {
        displayMessage(message,mDisplayTime);
    }

    public void displayMessage(String message, int delay) {
        setText(message);

        // Clear any pending runnables
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }

        mHandler = new Handler();
        mRunnable = new Runnable() {
            public void run() {
                setText(mInstructionText);
            }
        };
        mHandler.postDelayed(mRunnable, delay);
    }


}

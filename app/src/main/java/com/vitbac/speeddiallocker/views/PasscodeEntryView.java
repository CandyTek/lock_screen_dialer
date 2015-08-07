package com.vitbac.speeddiallocker.views;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/5/15.
 */
public abstract class PasscodeEntryView extends RelativeLayout implements View.OnTouchListener{

    private static final String TAG = "PasscodeEntryView";
    private String mPasscode;

    private Handler mLongPressHandler;
    private Runnable mLongPressRunnable;
    private int mLongPressDelay;
    private View mLastViewPressed;

    protected View[] mKeys;

    protected String mFont;

    protected onPasscodeEntryListener mPasscodeListener;
    protected onInputReceivedListener mInputListener;
    protected onLongPressListener mLongPressListener;

    public PasscodeEntryView (Context context) {
        super(context);
        init();
    }

    public PasscodeEntryView (Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO: do styled attributes
        init();
    }

    private void init() {
        inflate(getContext(), getLayout(), this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mLongPressDelay = getResources().getInteger(R.integer.lock_screen_pattern_long_press_delay);
        mKeys = initKeys();
        mFont = prefs.getString(
                getContext().getString(R.string.key_select_lock_screen_fonts),
                getContext().getString(R.string.font_default));
    }

    public interface onPasscodeEntryListener {
        void onPasscodeEntered(boolean isCorrect);
    }

    public interface onInputReceivedListener {
        void onInputReceived(String input);
    }

    public interface onLongPressListener {
        void onLongPress(int digit);
    }

    public void setOnPassCodeEntryListener(onPasscodeEntryListener listener) {
        mPasscodeListener = listener;
    }

    public void setOnInputReceivedListener(onInputReceivedListener listener) {
        mInputListener = listener;
    }

    public void setOnLongPressListener(onLongPressListener listener) {
        mLongPressListener = listener;
    }

    public void setPasscode (String passcode) throws IllegalArgumentException {
        if (passcode.matches("[0-9]+")) {
            mPasscode = passcode;
        } else {
            throw new IllegalArgumentException("Passcode can only contain digits; " + passcode
            + " provided is improper.");
        }
    }

    public void clearPasscode() {
        mPasscode = null;
    }

    protected boolean matchesPasscode(String input) {
        if (mPasscode == null || !mPasscode.equals(input)) {
            return false;
        }

        return true;
    }

    protected void onPasscodeCorrect() {
        if (mPasscodeListener != null) {
            mPasscodeListener.onPasscodeEntered(true);
        }
    }

    protected void onPasscodeFail() {
        if (mPasscodeListener != null) {
            mPasscodeListener.onPasscodeEntered(false);
        }
    }

    // Method that assigns the View[] mKeys field
    abstract View[] initKeys();
    abstract int getLayout();


    /**
     * We want to implement on touch here so far as to tell a listener whether a long touch
     * has taken place.  It will never consume the event.
     * @param view
     * @param event
     * @return
     */
    public boolean onTouch(View view, MotionEvent event) {
        if (mLongPressListener == null) {
            return false;
        }

        int index = getKeyPressed(view.getId(), -1);
        if (index == -1){
            return false;
        }

        // Get the key pressed and make sure that it is valid
        final int key;
        try {
            key = ((KeyNumber) (mKeys[index].getTag())).keyNumber;
        } catch (ClassCastException e) {
            Log.e(TAG, "Invalid tag applied to key bearing index " + index);
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mLongPressHandler == null) {
                    mLongPressHandler = new Handler();
                } else if (mLongPressRunnable != null) {
                    // Clear the callback train
                    mLongPressHandler.removeCallbacks(mLongPressRunnable);
                }

                mLongPressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mLongPressListener.onLongPress(key);
                    }
                };
                mLongPressHandler.postDelayed(mLongPressRunnable, mLongPressDelay);
                mLastViewPressed = view;
                break;

            case MotionEvent.ACTION_UP:
                if (mLongPressRunnable != null) {
                    try {
                        mLongPressHandler.removeCallbacks(mLongPressRunnable);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Long press handler was null on Action Up");
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int x = (int)event.getRawX(), y =(int)event.getRawY();
                if (isTouchOutsideView(mLastViewPressed, x, y)) {
                    try {
                        mLongPressHandler.removeCallbacks(mLongPressRunnable);
                        mLongPressRunnable = null;
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Long press handler was null on Action Move");
                    }
                    break;
                }
            }
        return false;
    }


    protected int getKeyPressed(int id, int def) {
        for (int i=0; i < mKeys.length; i++) {
            if (mKeys[i].getId() == id) {
                try {
                    return ((KeyNumber) mKeys[i].getTag()).keyNumber;
                } catch (ClassCastException e) {
                    Log.e(TAG, "View has invalid tag to identify a key");
                    break;
                }
            }
        }
        return def;
    }

    protected int getKeyNumber(View view, int def) {
        try {
            return ((KeyNumber) view.getTag()).keyNumber;
        } catch (ClassCastException e) {
            Log.e(TAG, "View has invalid tag to identify a key");
        }
        return def;
    }

    protected String getKeyNumber(View view) {
        try {
            return Integer.toString(((KeyNumber) view.getTag()).keyNumber);
        } catch (ClassCastException e) {
            Log.e(TAG, "View has invalid tag to identify a key");
        }
        return "";
    }

    /**
     * Returns true if the x and y coordinates are outside of mLastViewPressed.  Returns true also
     * if mLastViewPressed is null
     * @param x
     * @param y
     * @return
     */
    protected boolean isTouchOutsideView(View view, int x, int y) {
        if (view == null) {
            return true;
        }
        int[] coord = new int[2];
        view.getLocationOnScreen(coord);
        Rect r = new Rect(
                coord[0],
                coord[1],
                coord[0] + view.getWidth(),
                coord[1] + view.getHeight());
        if (!r.contains(x, y)) {
            return true;
        }
        return false;
    }


    protected class KeyNumber {
        public int keyNumber;

        public KeyNumber (int num) {
            keyNumber = num;
        }
    }

}

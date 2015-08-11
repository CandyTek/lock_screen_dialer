package com.vitbac.speeddiallocker.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/5/15.
 */
public abstract class PasscodeEntryWidget extends RelativeLayout implements View.OnTouchListener{

    private static final String TAG = "PasscodeEntryView";
    private String mPasscode;

    private Handler mLongPressHandler;
    private Runnable mLongPressRunnable;
    private boolean mLongPressFlag;
    private int mLongPressDelay;
    private View mLastViewPressed;

    private boolean mBlockInputFlag;

    protected Button[] mKeys;

    protected String mFont;

    protected OnPasscodeEntryListener mPasscodeListener;
    protected OnInputReceivedListener mInputListener;
    protected OnLongPressListener mLongPressListener;

    public PasscodeEntryWidget(Context context) {
        super(context);
        init();
    }

    public PasscodeEntryWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray attributeArray = context.obtainStyledAttributes(attrs,
                R.styleable.PasscodeEntryWidget, 0, 0);
        mLongPressDelay = attributeArray.getInt(
                R.styleable.PasscodeEntryWidget_longPressDelay,
                getResources().getInteger(R.integer.lock_screen_pattern_long_press_delay));
        mFont = attributeArray.getString(R.styleable.PasscodeEntryWidget_fontFamily);
        validateFont(); // Checks that the font is valid, assigning mFont to the def font if not
        init();
        attributeArray.recycle();
    }

    private void init() {
        inflate(getContext(), getLayout(), this);
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        mKeys = initKeys();
        for (int i=0; i < mKeys.length; i++) {
            mKeys[i].setOnTouchListener(this);
        }
        /*mFont = prefs.getString(
                getContext().getString(R.string.key_select_lock_screen_fonts),
                getContext().getString(R.string.font_default));*/
        mLongPressFlag = false;
        mBlockInputFlag = false;
    }

    public interface OnPasscodeEntryListener {
        void onPasscodeEntered(boolean isCorrect);
    }

    public interface OnInputReceivedListener {
        void onInputReceived(String input);
    }

    public interface OnLongPressListener {
        void onLongPress(int digit);
    }

    public void setOnPassCodeEntryListener(OnPasscodeEntryListener listener) {
        mPasscodeListener = listener;
    }

    public void setOnInputReceivedListener(OnInputReceivedListener listener) {
        mInputListener = listener;
    }

    public void setOnLongPressListener(OnLongPressListener listener) {
        mLongPressListener = listener;
    }

    public void setPasscode (String passcode) throws IllegalArgumentException {
        if (passcode != null && passcode.matches("[0-9]+")) {
            mPasscode = passcode;
        } else {
            throw new IllegalArgumentException("Passcode can only contain digits; " + passcode
            + " provided is improper.");
        }
    }

    public void blockInput() {
        Log.d(TAG, "blockInput()");
        mBlockInputFlag = true;
    }

    public void unblockInput() {
        Log.d(TAG, "unblockInput()");
        mBlockInputFlag = false;
    }

    public boolean isInputBlocked(){
        return mBlockInputFlag;
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

    public void setTypeface(Typeface tf) {
        for (int i=0; i < mKeys.length; i++) {
            mKeys[i].setTypeface(tf);
        }
        /*String currentFont = mFont;

        mFont = font;
        validateFont();
        if (!mFont.equals(font)) {
            mFont = currentFont;
        } else {
            for (int i=0; i < mKeys.length; i++) {
                mKeys[i].setTypeface(Typeface.create(mFont, Typeface.NORMAL));
            }
        }*/
    }

    // Method that assigns the View[] mKeys field
    abstract Button[] initKeys();
    abstract int getLayout();
    public abstract void resetView();
    public abstract void backspace();


    /**
     * We want to implement on touch here so far as to tell a listener whether a long touch
     * has taken place.  It will never consume the event.
     * @param view
     * @param event
     * @return
     */
    public boolean onTouch(View view, MotionEvent event) {
        if (mBlockInputFlag) {
            return true;
        }

        if (mLongPressListener == null) {
            return false;
        }

        final int key = getKeyPressed(view.getId(), -1);
        if (key == -1){
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
                        mLongPressFlag = true;
                    }
                };
                mLongPressHandler.postDelayed(mLongPressRunnable, mLongPressDelay);
                mLastViewPressed = view;
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
                }
                if (mLongPressFlag) {
                    // Consume the event until an action up.
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mLongPressRunnable != null) {
                    try {
                        mLongPressHandler.removeCallbacks(mLongPressRunnable);
                    } catch (NullPointerException e) {
                        Log.e(TAG, "Long press handler was null on Action Up");
                    }
                }
                if (mLongPressFlag) {
                    // Consume the event, but future touches will not be consumed automatically
                    mLongPressFlag = false;
                    Log.d(TAG, "Blocking input from ACTION_UP event after there has been a long press");
                    blockInput();
                    resetView();
                    return true;
                }
                break;
            }
        return false;
    }


    protected int getKeyPressed(int id, int def) {
        for (int i=0; i < mKeys.length; i++) {
            if (mKeys[i].getId() == id) {
                try {
                    return ((KeyMarker) mKeys[i].getTag()).keyNumber;
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
            return ((KeyMarker) view.getTag()).keyNumber;
        } catch (ClassCastException e) {
            return def;
        } catch (NullPointerException e) {
            return def;
        }

    }

    protected String getKeyNumber(View view) {
        try {
            return Integer.toString(((KeyMarker) view.getTag()).keyNumber);
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

    private void validateFont() {
        String defFont = getResources().getString(R.string.font_default);
        if (mFont == null) {
            mFont = defFont;
            return;
        }

        TypedArray fontArray = getResources().obtainTypedArray(R.array.fonts);
        for (int i=0; i < fontArray.length(); i++) {
            if (fontArray.getString(i) != null && mFont.equals(fontArray.getString(i))) {
                return;
            }
        }
        mFont = defFont;
    }

    public KeyMarker getKeyMarker(View view) {
        return (KeyMarker) view.getTag();
    }


    protected class KeyMarker {
        public int keyNumber;
        public boolean isMarked;

        public KeyMarker(int num) {
            keyNumber = num;
            isMarked = false;
        }

        public KeyMarker (int num, boolean marked) {
            keyNumber = num;
            isMarked = marked;
        }
    }

}

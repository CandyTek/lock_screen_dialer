package com.vitbac.speeddiallocker.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/9/15.
 */
public class PinEntryView extends PasscodeEntryView
        implements View.OnClickListener{
    private static final String TAG = "PinEntryView";

    private String mPinEntered;
    private Button mOkButton;
    private boolean mLongPressFlag;

    public PinEntryView (Context context) {
        super(context);
        mLongPressFlag = false;
        resetView();
    }

    public PinEntryView (Context context, AttributeSet attrs) {
        super(context, attrs);
        mLongPressFlag = false;
        resetView();
    }

    protected Button[] initKeys() {
        Button[] buttons = new Button[10];

        buttons[0] = (Button) findViewById(R.id.pin_button_0);
        buttons[1] = (Button) findViewById(R.id.pin_button_1);
        buttons[2] = (Button) findViewById(R.id.pin_button_2);
        buttons[3] = (Button) findViewById(R.id.pin_button_3);
        buttons[4] = (Button) findViewById(R.id.pin_button_4);
        buttons[5] = (Button) findViewById(R.id.pin_button_5);
        buttons[6] = (Button) findViewById(R.id.pin_button_6);
        buttons[7] = (Button) findViewById(R.id.pin_button_7);
        buttons[8] = (Button) findViewById(R.id.pin_button_8);
        buttons[9] = (Button) findViewById(R.id.pin_button_9);
        mOkButton = (Button) findViewById(R.id.pin_button_OK);

        for (int i=0; i < buttons.length; i++) {
            buttons[i].setTag(new KeyMarker(i));
            buttons[i].setOnTouchListener(this);
            buttons[i].setOnClickListener(this);
        }
        mOkButton.setOnClickListener(this);
        setTypeface(Typeface.create(mFont, Typeface.NORMAL));

        return buttons;
    }

    public void onClick (View view) {
        if (mLongPressFlag) { // This flag lets us clear the button
            mLongPressFlag = false;
            return;
        }
        if (isInputBlocked()) {
            return;
        }
        int num = getKeyNumber(view, -1);
        if (num == -1) {
            switch (view.getId()) {
                case R.id.pin_button_OK:
                    if (mPinEntered.length() != 0){ // We don't care if the OK button was pressed without any PIN entered
                        if (matchesPasscode(mPinEntered)) {
                            onPasscodeCorrect();
                        } else {
                            blockInput();  // This must go first!
                            onPasscodeFail();
                        }
                    }
                    break;
            }
            return;
        }

        mPinEntered += num;

        // Check the input listener
        if (mInputListener != null) {
            mInputListener.onInputReceived(mPinEntered);
        }
        // TODO: implement styable for accepting passcode on "OK" press only
        if (matchesPasscode(mPinEntered)) {
            onPasscodeCorrect();
        }
    }

    @Override
    public boolean onTouch (View view, MotionEvent event) {
        //  This is a little hacky, but it prevents onTouch from killing the clicks in this class
        // and thereby freezing the button in pressed mode
        if (super.onTouch(view, event)) {
            mLongPressFlag = true;
        }
        return false;
    }

    @Override
    public void blockInput() {
        super.blockInput();
        for (int i=0; i < mKeys.length; i++) {
            mKeys[i].setClickable(false);
        }
        mOkButton.setClickable(false);
    }

    @Override
    public void unblockInput() {
        super.unblockInput();
        for (int i=0; i < mKeys.length; i++) {
            mKeys[i].setClickable(true);
        }
        mOkButton.setClickable(true);
    }

    @Override
    public void setTypeface(Typeface tf) {
        setFont((ViewGroup) findViewById(R.id.pin_root_view), tf);
    }

    private void setFont(ViewGroup parent, Typeface tf) {
        for (int i=0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                setFont((ViewGroup) child, tf);
            } else if (child instanceof TextView) {
                ((TextView)child).setTypeface(tf);
            }
        }
    }

    protected int getLayout() {
        return R.layout.view_pin_entry;
    }

    public void resetView(){
        unblockInput();
        mPinEntered = "";
    }

    public void backspace() {
        Log.d(TAG, "backspace()");
        if (mPinEntered.length() > 0) {
            mPinEntered = mPinEntered.substring(0, mPinEntered.length()-1);
            Log.d(TAG, "mPinEntered is now " + mPinEntered);
        }
    }

}

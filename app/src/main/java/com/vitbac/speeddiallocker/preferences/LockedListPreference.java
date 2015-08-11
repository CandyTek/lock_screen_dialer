package com.vitbac.speeddiallocker.preferences;

import android.annotation.SuppressLint;
import android.app.FragmentManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.vitbac.speeddiallocker.fragments.PasscodeDialogFragment;

/**
 * Created by nick on 8/11/15.
 */
public class LockedListPreference extends MyListPreference {

    private static final String TAG = "LockedListPref";

    private boolean isLocked;
    private OnPrefAccessRequestedListener mListener;

    public interface OnPrefAccessRequestedListener {
        void onPrefAccessRequested(LockedListPreference preference);
    }

    @SuppressLint("NewApi")
    public LockedListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @SuppressLint("NewApi")
    public LockedListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public LockedListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LockedListPreference(Context context) {
        super(context);
        init();
    }

    private void init() {
        isLocked = false;
    }

    @Override
    protected void onClick() {
        if (isLocked) {
            if (mListener != null) {
                mListener.onPrefAccessRequested(this);
            }
        } else {
            super.onClick();
        }
    }

    public void unlock() {
        isLocked = false;
    }

    public void lock() {
        isLocked = true;
    }

    public void grantAccess() {
        if (isLocked) {
            // Reset to locked after processing the click
            isLocked = false;
            onClick();
            isLocked = true;
        } else {
            onClick();
        }
    }

    public void setOnPrefAccessRequestedListener(OnPrefAccessRequestedListener listener) {
        mListener = listener;
    }

}

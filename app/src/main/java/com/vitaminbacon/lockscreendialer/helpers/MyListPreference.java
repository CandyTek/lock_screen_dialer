package com.vitaminbacon.lockscreendialer.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Allows us to easily invoke a click of ListPreference with .show()
 */
public class MyListPreference extends ListPreference {

    private ListItemClickListener mListItemClickListener;

    @SuppressLint("NewApi")
    public MyListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @SuppressLint("NewApi")
    public MyListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && getEntryValues() != null && mListItemClickListener != null) {
            String value = getValue();
            mListItemClickListener.onListItemClick(value);
        }
    }

    public void show() {
        showDialog(null);
    }

    public void setOnListItemClickListener(ListItemClickListener listener) {
        mListItemClickListener = listener;
    }

    public interface ListItemClickListener {
        public void onListItemClick(String value);
    }
}

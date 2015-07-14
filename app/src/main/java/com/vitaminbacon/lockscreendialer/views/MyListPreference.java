package com.vitaminbacon.lockscreendialer.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

import com.vitaminbacon.lockscreendialer.R;

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
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(R.string.menu_ok, this);
        builder.setNegativeButton(R.string.menu_cancel, this);
        // Get rid of cancel button
        //builder.setNegativeButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult && getEntryValues() != null && mListItemClickListener != null) {
            String value = getValue();
            mListItemClickListener.onListItemClick(value, getKey());
        }
    }

    public void show() {
        showDialog(null);
    }

    public void setOnListItemClickListener(ListItemClickListener listener) {
        mListItemClickListener = listener;
    }

    public interface ListItemClickListener {
        public void onListItemClick(String value, String key);
    }
}

package com.vitbac.speeddiallocker;

import android.app.Activity;
import android.os.Bundle;

import com.vitbac.speeddiallocker.fragments.ColorPickerDialogFragment;
import com.vitbac.speeddiallocker.fragments.FontPickerDialogFragment;
import com.vitbac.speeddiallocker.fragments.SettingsFragment;

public class SettingsActivity extends Activity
        implements ColorPickerDialogFragment.OnColorSelectedListener,
        FontPickerDialogFragment.OnFontSelectedListener {

    private static final String TAG = "SettingsActivity";
    private SettingsFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragment = new SettingsFragment();
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, mFragment)
                .commit();
    }

    /**
     * Inartful but required method, since ColorPickerDialogFragment must be tethered to an activity,
     * but the information sought to be processed must be performed by the PreferenceFragment class
     *
     * @param color
     * @param key
     */
    public void onColorSelected(int color, int key) {
        mFragment.onColorSelected(color, key);
    }

    public void onFontSelected(String font, int key) {
        mFragment.onFontSelected(font, key);
    }

}



package com.vitbac.speeddiallocker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.vitbac.speeddiallocker.fragments.ColorPickerDialogFragment;
import com.vitbac.speeddiallocker.fragments.FontPickerDialogFragment;
import com.vitbac.speeddiallocker.fragments.SettingsFragment;

import fr.nicolaspomepuy.discreetapprate.AppRate;
import fr.nicolaspomepuy.discreetapprate.RetryPolicy;

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

    @Override
    protected void onResume() {
        super.onResume();
        AppRate.with(this)
                .delay(1000)
                .initialLaunchCount(4)
                .retryPolicy(RetryPolicy.INCREMENTAL)
                .checkAndShow();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_instructions:
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setTitle(getString(R.string.alert_dialog_title_instructions));
                dialogBuilder
                        .setMessage(getString(R.string.alert_dialog_message_instructions))
                        .setCancelable(false)
                        .setNegativeButton(getString(R.string.alert_dialog_button_text_instructions),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alertDialog = dialogBuilder.create();
                alertDialog.show();
                return true;
        }
        return true;
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



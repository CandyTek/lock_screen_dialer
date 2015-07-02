package com.vitaminbacon.lockscreendialer.fragments;


import android.app.ActivityManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vitaminbacon.lockscreendialer.KeypadPatternConfigActivity;
import com.vitaminbacon.lockscreendialer.KeypadPinConfigActivity;
import com.vitaminbacon.lockscreendialer.R;
import com.vitaminbacon.lockscreendialer.services.LockScreenService;
import com.vitaminbacon.lockscreendialer.views.ColorPreference;
import com.vitaminbacon.lockscreendialer.views.MyListPreference;

/**
 * A simple {@link Fragment} subclass.
 */
public class SettingsFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener,
        MyListPreference.ListItemClickListener {

    private static final String TAG = "SettingsFragment";
    private static final int PICK_LOCK_SCREEN_PIN = 1;
    private static final int PICK_LOCK_SCREEN_PATTERN = 2;


    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // If settings believes the service is enabled and it is not (due to error of some kind), fix it.
        CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(
                getString(R.string.key_toggle_lock_screen));
        if (!isServiceRunning(LockScreenService.class) && checkPref.isChecked()) {
            //Log.d(TAG, "Turning off lock screen in onResume()");
            checkPref.setChecked(false);
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        try {
            Preference lockScreenTogglePref =
                    getPreferenceScreen().findPreference(getString(R.string.key_toggle_lock_screen));
            lockScreenTogglePref.setOnPreferenceChangeListener(this);
        } catch (NullPointerException e) {
            Log.e(TAG, "Lock screen toggle preference missing from layout");
            throw e;
        }

        try {
            MyListPreference listPref = (MyListPreference)
                    findPreference(getString(R.string.key_select_lock_screen_type));
            listPref.setOnListItemClickListener(this);
        } catch (NullPointerException e) {
            Log.e(TAG, "Lock screen type selection preference missing from layout");
            throw e;
        }

        try {
            Preference speedDialBtnColorPref = getPreferenceScreen()
                    .findPreference(getString(R.string.key_select_speed_dial_button_color));
            speedDialBtnColorPref.setOnPreferenceClickListener(this);
        } catch (NullPointerException e) {
            Log.w(TAG, "Speed dial button color preference missing from layout");
        }

        try {
            Preference patternDrawColorPref = getPreferenceScreen()
                    .findPreference(getString(R.string.key_select_pattern_draw_color));
            patternDrawColorPref.setOnPreferenceClickListener(this);
        } catch (NullPointerException e) {
            Log.w(TAG, "Drawing color preference missing from layout");
        }
    }


    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_LOCK_SCREEN_PIN:
                //Log.d(TAG, "Result from PIN activity, code = " + resultCode);
                if (resultCode == getActivity().RESULT_OK) {
                    try {
                        MyListPreference listPref = (MyListPreference) findPreference(
                                getString(R.string.key_select_lock_screen_type));
                        listPref.setValue(getString(R.string.value_lock_screen_type_keypad_pin));
                        CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(
                                getString(R.string.key_toggle_lock_screen));
                        checkPref.setChecked(true);
                        // Since onChangedListener not registered yet when this call is made, need to create service
                        getActivity().startService(new Intent(getActivity(), LockScreenService.class));
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Lock screen enabled preference of wrong type, unable to modify");
                    }
                }
                break;
            case PICK_LOCK_SCREEN_PATTERN:
                if (resultCode == getActivity().RESULT_OK) {
                    try {
                        MyListPreference listPref = (MyListPreference) findPreference(
                                getString(R.string.key_select_lock_screen_type));
                        listPref.setValue(getString(R.string.value_lock_screen_type_keypad_pattern));
                        CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(
                                getString(R.string.key_toggle_lock_screen));
                        checkPref.setChecked(true);
                        getActivity().startService(new Intent(getActivity(), LockScreenService.class));
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Lock screen enabled preference of wrong type, unable to modify");
                    }
                }
                break;
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(getString(R.string.key_select_speed_dial_button_color))) {
            ColorPickerDialogFragment dialogFragment;
            int color = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getInt(preference.getKey(), getResources().getColor(R.color.blue_diamond));
            dialogFragment = ColorPickerDialogFragment
                    .newInstance(color, R.string.key_select_speed_dial_button_color);
            dialogFragment.show(getFragmentManager(), "fragment_color_list_dialog");
            return true;
        } else if (preference.getKey().equals(getString(R.string.key_select_pattern_draw_color))) {
            ColorPickerDialogFragment dialogFragment;
            int color = PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getInt(preference.getKey(), getResources().getColor(R.color.green));
            dialogFragment = ColorPickerDialogFragment
                    .newInstance(color, R.string.key_select_pattern_draw_color);
            dialogFragment.show(getFragmentManager(), "fragment_color_list_dialog");
            return true;
        }
        return false;
    }


    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference.getKey().equals(getString(R.string.key_toggle_lock_screen))) {
            MyListPreference pref = (MyListPreference)
                    findPreference(getString(R.string.key_select_lock_screen_type));

            // Where no lock screen type has been selected
            if (pref.getValue().equals(getString(R.string.value_lock_screen_type_none))) {
                //Set toast
                LayoutInflater inflater = getActivity().getLayoutInflater();
                View layout = inflater.inflate(
                        R.layout.toast_custom,
                        (ViewGroup) getView().findViewById(R.id.toast_custom));
                TextView text = (TextView) layout.findViewById(R.id.toast_text);
                text.setText(getString(R.string.toast_lock_screen_type_not_selected));
                Toast toast = new Toast(getActivity().getApplicationContext());
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(layout);
                toast.show();

                // Show the lock screen selection dialog
                pref.show();
                return false;
            }
        }
        return true;
    }

    /**
     * Handles toggling of lock screen on/off by taking down the lock screen service
     *
     * @param sharedPreferences
     * @param key
     */
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //Log.d(TAG, "onSharedPreferencesChanged called, key = " + key);
        if (key.equals(getString(R.string.key_toggle_lock_screen))) {
            CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(key);
            //String text;
            if (checkPref.isChecked()) {
                Log.d(TAG, "Lock screen flagged enabled with valid unlocking mech, starting service");
                getActivity().startService(new Intent(getActivity(), LockScreenService.class));
            } else {
                Log.d(TAG, "Lock screen flagged disabled, stopping service");
                getActivity().stopService(new Intent(getActivity(), LockScreenService.class));
            }
        } else if (key.equals(getString(R.string.key_select_lock_screen_type))) {
            MyListPreference listPref = (MyListPreference) findPreference(key);
            if (listPref.getValue().equals(getString(R.string.value_lock_screen_type_none))) {
                CheckBoxPreference checkPref =
                        (CheckBoxPreference) findPreference(getString(R.string.key_toggle_lock_screen));
                if (checkPref.isChecked()) {
                    checkPref.setChecked(false);
                }
            }
        }
    }

    /**
     * Provide activated upon dialog close and sends the value of the item clicked
     *
     * @param value - contains the value of the ListPreference that was clicked
     */
    public void onListItemClick(String value) {
        // Reset to no lock screen regardless until get good result in onActivityResult()
        try {
            CheckBoxPreference pref = (CheckBoxPreference) findPreference(
                    getString(R.string.key_toggle_lock_screen));
            pref.setChecked(false);
            MyListPreference listPref = (MyListPreference) findPreference(
                    getString(R.string.key_select_lock_screen_type));
            listPref.setValue(getString(R.string.value_lock_screen_type_none));
        } catch (ClassCastException e) {
            Log.e(TAG, "Lock screen enabled preference of wrong type, unable to modify");
        }

        if (value.equals(getString(R.string.value_lock_screen_type_keypad_pin))) {
            //Log.d(TAG, "Selected PIN activity");
            // Lock screen PIN was selected, need to go to config for that
            Intent intent = new Intent(getActivity(), KeypadPinConfigActivity.class);
            startActivityForResult(intent, PICK_LOCK_SCREEN_PIN);
        } else if (value.equals(getString(R.string.value_lock_screen_type_keypad_pattern))) {
            // Same logic as above
            Intent intent = new Intent(getActivity(), KeypadPatternConfigActivity.class);
            startActivityForResult(intent, PICK_LOCK_SCREEN_PATTERN);
        }
    }

    public void onColorSelected(int color, int key) {
        if (key == R.string.key_select_speed_dial_button_color
                || key == R.string.key_select_pattern_draw_color) {
            // We can apply same logic to either of these keys
            try {
                ColorPreference pref = (ColorPreference) findPreference(getString(key));
                pref.setColor(color);
            } catch (ClassCastException e) {
                Log.e(TAG, "Wrong preference received to set color, need ColorPreference");
            } catch (NullPointerException e) {
                Log.e(TAG, "Unable to obtain ColorPreference with key");
            }

        }
    }


    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager =
                (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


}

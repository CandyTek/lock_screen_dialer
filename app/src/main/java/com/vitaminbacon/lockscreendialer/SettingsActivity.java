package com.vitaminbacon.lockscreendialer;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vitaminbacon.lockscreendialer.fragments.ColorPickerDialogFragment;
import com.vitaminbacon.lockscreendialer.services.LockScreenService;
import com.vitaminbacon.lockscreendialer.views.ColorPreference;
import com.vitaminbacon.lockscreendialer.views.MyListPreference;

import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
@SuppressWarnings("deprecation")
public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener,
        Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener,
        MyListPreference.ListItemClickListener, ColorPickerDialogFragment.OnColorSelectedListener {
    static final int PICK_LOCK_SCREEN_PIN = 1;
    static final int PICK_LOCK_SCREEN_PATTERN = 2;
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;
    private static final String TAG = "SettingsActivity";
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary("silent"); // TODO: get rid of or utilize for sound of unlocking

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return ALWAYS_SIMPLE_PREFS
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
                || !isXLargeTablet(context);
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupSimplePreferencesScreen();
        CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(
                getString(R.string.key_toggle_lock_screen));
        // If settings believes the service is enabled and it is not (due to error of some kind), fix it.
        if (!isServiceRunning(LockScreenService.class) && checkPref.isChecked()) {
            //Log.d(TAG, "Turning off lock screen in onResume()");
            checkPref.setChecked(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        try {
            Preference lockScreenTogglePref =
                    getPreferenceScreen().findPreference(getString(R.string.key_toggle_lock_screen));
            lockScreenTogglePref.setOnPreferenceChangeListener(this);
        } catch (NullPointerException e) {
            Log.e(TAG, "Lock screen toggle preference missing from layout");
            finish();
            return;
        }

        try {
            MyListPreference listPref = (MyListPreference)
                    findPreference(getString(R.string.key_select_lock_screen_type));
            listPref.setOnListItemClickListener(this);
        } catch (NullPointerException e) {
            Log.e(TAG, "Lock screen type selection preference missing from layout");
            finish();
            return;
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
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        PreferenceCategory speedDialHeader = new PreferenceCategory(this);
        speedDialHeader.setTitle(getString(R.string.pref_header_speed_dial));
        getPreferenceScreen().addPreference(speedDialHeader);
        addPreferencesFromResource(R.xml.pref_speed_dial);

        // Add 'display' preferences.
        PreferenceCategory displayHeader = new PreferenceCategory(this);
        displayHeader.setTitle(R.string.pref_header_display);  // Changed from template
        getPreferenceScreen().addPreference(displayHeader);
        addPreferencesFromResource(R.xml.pref_display);  //Changed from template
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        if (!isSimplePreferences(this)) {
            loadHeadersFromResource(R.xml.pref_headers, target);
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
            /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(getString(key), color);
            editor.commit();*/
        }
    }

    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference.getKey().equals(getString(R.string.key_toggle_lock_screen))) {
            MyListPreference pref = (MyListPreference)
                    findPreference(getString(R.string.key_select_lock_screen_type));

            // Where no lock screen type has been selected
            if (pref.getValue().equals(getString(R.string.value_lock_screen_type_none))) {
                //Set toast
                LayoutInflater inflater = getLayoutInflater();
                View layout = inflater.inflate(
                        R.layout.toast_custom,
                        (ViewGroup) findViewById(R.id.toast_custom));
                TextView text = (TextView) layout.findViewById(R.id.toast_text);
                text.setText(getString(R.string.toast_lock_screen_type_not_selected));
                Toast toast = new Toast(getApplicationContext());
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
                startService(new Intent(this, LockScreenService.class));
                //text = getString(R.string.toast_lock_screen_enabled);

            } else {
                Log.d(TAG, "Lock screen flagged disabled, stopping service");
                stopService(new Intent(this, LockScreenService.class));
                //text = getString(R.string.toast_lock_screen_disabled);
            }
            // Display toast
            /*LayoutInflater inflater = getLayoutInflater();
            View layout = inflater.inflate(
                    R.layout.toast_custom,
                    (ViewGroup) findViewById(R.id.toast_custom));
            TextView textView = (TextView) layout.findViewById(R.id.toast_text);
            textView.setText(text);
            Toast toast = new Toast(getApplicationContext());
            toast.setDuration(Toast.LENGTH_SHORT);
            toast.setView(layout);
            toast.show();*/
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
     * This fragment shows data and sync preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     * Not needed so commented-out.
     *
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DataSyncPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_data_sync);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
        }
    }
    */

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
            Intent intent = new Intent(this, KeypadPinConfigActivity.class);
            startActivityForResult(intent, PICK_LOCK_SCREEN_PIN);
        } else if (value.equals(getString(R.string.value_lock_screen_type_keypad_pattern))) {
            // Same logic as above
            Intent intent = new Intent(this, KeypadPatternConfigActivity.class);
            startActivityForResult(intent, PICK_LOCK_SCREEN_PATTERN);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_LOCK_SCREEN_PIN:
                //Log.d(TAG, "Result from PIN activity, code = " + resultCode);
                if (resultCode == RESULT_OK) {
                    try {
                        MyListPreference listPref = (MyListPreference) findPreference(
                                getString(R.string.key_select_lock_screen_type));
                        listPref.setValue(getString(R.string.value_lock_screen_type_keypad_pin));
                        CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(
                                getString(R.string.key_toggle_lock_screen));
                        checkPref.setChecked(true);
                        // Since onChangedListener not registered yet when this call is made, need to create service
                        startService(new Intent(this, LockScreenService.class));
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Lock screen enabled preference of wrong type, unable to modify");
                    }
                }
                break;
            case PICK_LOCK_SCREEN_PATTERN:
                if (resultCode == RESULT_OK) {
                    try {
                        MyListPreference listPref = (MyListPreference) findPreference(
                                getString(R.string.key_select_lock_screen_type));
                        listPref.setValue(getString(R.string.value_lock_screen_type_keypad_pattern));
                        CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(
                                getString(R.string.key_toggle_lock_screen));
                        checkPref.setChecked(true);
                        startService(new Intent(this, LockScreenService.class));
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Lock screen enabled preference of wrong type, unable to modify");
                    }
                }
                break;
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            bindPreferenceSummaryToValue(findPreference("example_text"));
            bindPreferenceSummaryToValue(findPreference("example_list"));
        }
    }

    /**
     * This fragment shows display preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     * Modified to reflect display header
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class DisplayPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_display);


            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            // Commented out; does not appear to be needed.
            //bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
        }
    }
}

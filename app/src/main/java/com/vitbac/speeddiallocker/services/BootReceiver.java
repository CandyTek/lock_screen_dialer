package com.vitbac.speeddiallocker.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.vitbac.speeddiallocker.ErrorPageActivity;
import com.vitbac.speeddiallocker.LockScreenActivity2;
import com.vitbac.speeddiallocker.R;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive()");
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent lockScreenIntent;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean lockScreenEnabled = prefs.getBoolean(
                    context.getString(R.string.key_toggle_lock_screen),
                    false);
            Log.d(TAG, "Lock screen enabled = " + lockScreenEnabled);

            if (lockScreenEnabled) {
                // Start the lock screen service
                context.startService(new Intent(context, LockScreenService.class));

                // Change prefs to the different file type
                prefs = context.getSharedPreferences(context.getString(R.string.file_lock_screen_type),
                        Context.MODE_PRIVATE);
                String lockScreenType = prefs.getString(
                        context.getString(R.string.key_lock_screen_type),
                        null);
                if (lockScreenType != null) {
                    lockScreenIntent = new Intent(context, LockScreenActivity2.class);
                    if (lockScreenType.equals(
                            context.getString(R.string.value_lock_screen_type_keypad_pin))) {
                        //newIntent = new Intent(context, LockScreenKeypadPinActivity.class);
                        lockScreenIntent.putExtra(
                                context.getString(R.string.key_lock_screen_type),
                                context.getString(R.string.value_lock_screen_type_keypad_pin)
                        );
                    } else if (lockScreenType.equals(
                            context.getString(R.string.value_lock_screen_type_keypad_pattern))) {
                        //newIntent = new Intent(context, LockScreenKeypadPatternActivity.class);
                        //newIntent = new Intent(context, LockScreenActivity2.class);
                        lockScreenIntent.putExtra(
                                context.getString(R.string.key_lock_screen_type),
                                context.getString(R.string.value_lock_screen_type_keypad_pattern)
                        );
                    } else { //An error of some kind
                        /*Log.e(TAG, "Invalid value for key " + context
                                .getString(R.string.key_lock_screen_type) + ": " + lockScreenType);
                        lockScreenIntent = new Intent(context, ErrorPageActivity.class);*/
                        throw new IllegalArgumentException("Invalid value for key " + context
                                .getString(R.string.key_lock_screen_type) + ": " + lockScreenType);
                    }
                } else {
                    /*Log.e(TAG, "No value for key " + context
                            .getString(R.string.key_lock_screen_type));
                    lockScreenIntent = new Intent(context, ErrorPageActivity.class);*/
                    throw new IllegalArgumentException("No value for key " + context
                            .getString(R.string.key_lock_screen_type));
                }
                lockScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lockScreenIntent);
            } else {
                Log.d(TAG, "Lock screen is not enabled.");
            }
        }
    }
}

package com.vitbac.speeddiallocker.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.vitbac.speeddiallocker.LockScreenLauncherActivity;
import com.vitbac.speeddiallocker.R;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BroadcastReceiver";

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            /*SharedPreferences prefs = context
                    .getSharedPreferences(context.getString(R.string.key_toggle_lock_screen),
                            Context.MODE_PRIVATE);*/
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean lockScreenEnabled = prefs.getBoolean(
                    context.getString(R.string.key_toggle_lock_screen),
                    false);

            Log.d(TAG, "Lock screen enabled = " + lockScreenEnabled);
            if (lockScreenEnabled) {
                // Start the lock screen service
                context.startService(new Intent(context, LockScreenService.class));

                // Initiate the lock screen
                Intent lockScreenIntent = new Intent(context, LockScreenLauncherActivity.class);
                lockScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lockScreenIntent);
            }
        }
    }
}

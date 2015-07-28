package com.vitbac.speeddiallocker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

/**
 * This activity is solely for the purpose of fulfilling the logic that decides which lock screen
 * activity is shown.  Error handling of the instance where a lock screen type is not specified is
 * performed by instantiating an error page activity.
 */

public class LockScreenLauncherActivity extends Activity {

    private static final String TAG = "LauncherActivity";
    private static final int SPLASH_TIME_OUT = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "LAUNCHER CALLED.");

        setContentView(R.layout.activity_lock_screen_launcher);
        //startActivity(new Intent(this, ErrorPageActivity.class));


        // Get the lock screen type from sharedPref
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.file_lock_screen_type),
                Context.MODE_PRIVATE);

        final String lockScreenType = sharedPref.getString(
                getString(R.string.key_lock_screen_type),
                null);

        final Intent intent;
        if (lockScreenType != null) {

            if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pin))) { // Now enable the correct lock screen
                intent = new Intent(getApplicationContext(), LockScreenKeypadPinActivity.class);
            } else if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pattern))) {
                intent = new Intent(getApplicationContext(), LockScreenKeypadPatternActivity.class);
            } else { //An error of some kind
                Log.d(TAG, "No value for key " + getString(R.string.key_lock_screen_type));
                intent = new Intent(getApplicationContext(), ErrorPageActivity.class);
            }
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            if (getIntent().getExtras() != null) {
                intent.putExtras(getIntent().getExtras()); //places the extras, if any, passed to the activity
            } else {
                Log.d(TAG, "Passed intent did not have any extras.");
            }

        } else {
            Log.e(TAG, "Unable to get the lock screen type from shared preferences.");
            finish();
            return;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }

    @Override
    public void onBackPressed() {
        return;
    }


}

package com.vitaminbacon.lockscreendialer;

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
    private static final int SPLASH_TIME_OUT = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "LAUNCHER CALLED.");

        setContentView(R.layout.activity_lock_screen_launcher);

        // Get the lock screen type from sharedPref
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        final String lockScreenType = sharedPref.getString(
                getString(R.string.lock_screen_type_value_key),
                null);

        final Intent intent;
        if (lockScreenType != null) {

            if (lockScreenType.equals(getString(R.string.lock_screen_type_value_keypad_pin))) { // Now enable the correct lock screen
                intent = new Intent(getApplicationContext(), LockScreenKeypadPinActivity.class);
            } else if (lockScreenType.equals(getString(R.string.lock_screen_type_value_keypad_pattern))) {
                intent = new Intent(getApplicationContext(), LockScreenKeypadPatternActivity.class);
            } else { //An error of some kind
                Log.d(TAG, "No value for key " + getString(R.string.lock_screen_type_value_key));
                intent = new Intent(getApplicationContext(), ErrorPageActivity.class);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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


        /*if (lockScreenType != null) {

            if (lockScreenType.equals(getString(R.string.lock_screen_type_value_keypad_pin))) { // Now enable the correct lock screen
                intent = new Intent(this, LockScreenKeypadPinActivity.class);
            } else if (lockScreenType.equals(getString(R.string.lock_screen_type_value_keypad_pattern))) {
                intent = new Intent(this, LockScreenKeypadPatternActivity.class);
            } else { //An error of some kind
                Log.d(TAG, "No value for key " + getString(R.string.lock_screen_type_value_key));
                intent = new Intent(this, ErrorPageActivity.class);
            }
        } else {
            Log.e(TAG, "Unable to get the lock screen type from shared preferences.");
            finish();
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // necessary to add to android's stack of things to do
        //intent.addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR); //this allows the activity to be placed on top of everything -- UGLY HACK??
        if (getIntent().getExtras() != null){
            intent.putExtras(getIntent().getExtras()); //places the extras, if any, passed to the activity
        }
        else {
            Log.d(TAG, "Passed intent did not have any extras.");
        }*/
        /*int delay;
        int phoneState = getIntent().getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, -1);
        if (phoneState != PhoneStateReceiver.PHONE_STATE_IDLE) {
            delay = SPLASH_TIME_OUT;
        } else {
            delay = 0;
        }*/

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
                finish();
            }
        }, SPLASH_TIME_OUT);
    }


}

package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class LockScreenActivity extends FragmentActivity
        implements LockScreenKeypadPinFragment.OnCorrectPasscode {

    private final static String TAG = "LockScreenActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        // Check that the activity is using the layout version with
        // the proper fragment container
        if (findViewById(R.id.lock_screen_fragment_container) != null) {

            /**
             * Check if being restored from a previous inactive state.  According to
             * http://developer.android.com/training/basics/activity-lifecycle/recreating.html,
             * the state of the layout is automatically restored to its previous state with no code
             * code required.
             */
            if (savedInstanceState != null) {
                return;
            }

            // Get the lock screen type from sharedPref
            SharedPreferences sharedPref = this.getSharedPreferences(
                    getString(R.string.lock_screen_type_file_key),
                    Context.MODE_PRIVATE);

            String lockScreenType = sharedPref.getString(
                    getString(R.string.lock_screen_type_value_key),
                    null);

            if (lockScreenType == null) {
                Log.d(TAG, "No value for key " + getString(R.string.lock_screen_type_value_key));
                finish();
                return;
            }

            // Now enable the correct lock screen
            if (lockScreenType.equals(getString(R.string.lock_screen_type_value_keypad_pin))) {
                Log.d(TAG, "Keypad PIN fragment to be implemented.");
                LockScreenKeypadPinFragment pinFragment = new LockScreenKeypadPinFragment();
                pinFragment.setArguments(getIntent().getExtras()); // In case activity started with special instructions
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.lock_screen_fragment_container, pinFragment).commit();
            } //TODO: enable other lock screen types
        }


    }

    public void onCorrectPasscode() {
        finish();
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lock_screen, menu);
        return true;
    }
*/

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}

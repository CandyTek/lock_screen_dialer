package com.vitbac.speeddiallocker.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.vitbac.speeddiallocker.ErrorPageActivity;
import com.vitbac.speeddiallocker.LockScreenKeypadPatternActivity;
import com.vitbac.speeddiallocker.LockScreenKeypadPinActivity;
import com.vitbac.speeddiallocker.R;

/**
 * Created by nick on 8/4/15.
 */
public class LockDelayService extends Service {
    private static final String TAG = "LockDelayService";
    private static Handler mHandler;
    private static Runnable mRunnable;

    public LockDelayService () {

    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        //Log.d(TAG, "onStartCommand called");
        int delay = intent.getIntExtra("delay", 0);
        String lockScreenType = intent.getStringExtra("lockScreenType");
        Intent newIntent;

        if (lockScreenType != null) {
            //newIntent = new Intent(context, LockScreenLauncherActivity.class);
            if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pin))) {
                newIntent = new Intent(this, LockScreenKeypadPinActivity.class);
            } else if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pattern))) {
                newIntent = new Intent(this, LockScreenKeypadPatternActivity.class);
            } else { //An error of some kind
                Log.e(TAG, "No value for key " + getString(R.string.key_lock_screen_type));
                newIntent = new Intent(this, ErrorPageActivity.class);
            }
        } else {
            Log.e(TAG, "Unable to get valid lock screen type from intent data.");
            newIntent = new Intent(this, ErrorPageActivity.class);
            //return super.onStartCommand(intent, flags, startId);
        }

        // Start the new intent

        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (delay <= 0) { //Robust
            startActivity(newIntent);
            stopSelf(); // Kill the service because it is not needed
        } else {
            Log.d(TAG, "delaying lock screen");
            final Intent delayIntent = newIntent;

            // For robustness -- we don't want to set a bunch of sleepers to lock the screen in the future
            if (mHandler != null && mRunnable != null) {
                mHandler.removeCallbacks(mRunnable);
            }

            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    startActivity(delayIntent);
                    stopSelf();
                }
            };
            mHandler.postDelayed(mRunnable, delay);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // Not a binding service; should run indefinitely, not tied to the activity
    }

    @Override
    public void onDestroy() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        super.onDestroy();
    }
}

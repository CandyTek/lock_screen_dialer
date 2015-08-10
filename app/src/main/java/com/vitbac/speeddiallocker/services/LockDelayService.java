package com.vitbac.speeddiallocker.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.vitbac.speeddiallocker.ErrorPageActivity;
import com.vitbac.speeddiallocker.LockScreenActivity;
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
        long startTime = intent.getLongExtra("startTime", System.currentTimeMillis());
        String lockScreenType = intent.getStringExtra("lockScreenType");
        Intent newIntent;

        if (lockScreenType != null) {
            newIntent = new Intent(this, LockScreenActivity.class);
            if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pin))) {
                newIntent.putExtra(
                        getString(R.string.key_lock_screen_type),
                        getString(R.string.value_lock_screen_type_keypad_pin)
                );
            } else if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pattern))) {
                newIntent.putExtra(
                        getString(R.string.key_lock_screen_type),
                        getString(R.string.value_lock_screen_type_keypad_pattern)
                );
            } else { //An error of some kind
                Log.e(TAG, "No value for key " + getString(R.string.key_lock_screen_type));
                newIntent = new Intent(this, ErrorPageActivity.class);
            }
        } else {
            Log.e(TAG, "Unable to get valid lock screen type from intent data.");
            newIntent = new Intent(this, ErrorPageActivity.class);
            //return super.onStartCommand(intent, flags, startId);
        }

        // Reconfigure the delay to account for service restarting or other delay
        long timeToLock = delay - (System.currentTimeMillis() - startTime);

        // Start the new intent
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (timeToLock <= 0) {
            // Start the lock screen immediately and kill the service
            startActivity(newIntent);
            stopSelf();
        } else {
            Log.d(TAG, "delaying lock screen to start in " + timeToLock + "ms");
            final Intent delayIntent = newIntent;

            // For robustness -- we don't want to set a bunch of sleepers to lock the screen in the future
            if (mHandler != null && mRunnable != null) {
                mHandler.removeCallbacks(mRunnable);
            }

            mHandler = new Handler();
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "lock screen starting after delay");
                    startActivity(delayIntent);
                    stopSelf();
                }
            };
            mHandler.postDelayed(mRunnable, timeToLock);
        }
        //return super.onStartCommand(intent, flags, startId);
        return START_REDELIVER_INTENT;
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

package com.vitbac.speeddiallocker.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.vitbac.speeddiallocker.ErrorPageActivity;
import com.vitbac.speeddiallocker.LockScreenActivity2;
import com.vitbac.speeddiallocker.R;

public class ScreenEventReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenEventRecevier";

    private static boolean mIssueIntentOnScreenOn = false;

    private static Handler mDelayHandler;
    private static Runnable mDelayRunnable;


    public ScreenEventReceiver() {
        mDelayHandler = new Handler();
    }

    /**
     * ============================================================================================
     * This method handles the two events that trigger this class:
     * (1) The event the screen is turned off, as denoted by "Intent.ACTION_SCREEN_OFF";
     * (2) The event the screen is turned on, as denoted by "Intent.ACTION_SCREEN_ON";
     *
     * Note that the primary method for issuing an intent to the lock screen is via
     * ACTION_SCREEN_OFF.  However, ACTION_SCREEN_ON is invoked in the situation where the screen
     * off event could not be invoked because the user was on the phone.  There, a static flag is
     * set to allow ACTION_SCREEN_ON to also issue the intent to the lock screen.  This logic
     * prevents the lock screen from interfering with the user's use of the phone during a phone
     * call, but also prevents the lock screen from never being invoked when the user completes the
     * call with the screen remaining off.
     * ===========================================================================================
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean startLockScreenIntent = false;


        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            //Log.d(TAG, "onReceive() obtained screen off event");
            if (tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK &&
                    tm.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
                startLockScreenIntent = true;
                mIssueIntentOnScreenOn = false;
            } else {
                /*Log.d(TAG, "Screen service could not be implemented because phone call active = "
                        + tm.getCallState());*/
                startLockScreenIntent = false;
                mIssueIntentOnScreenOn = true;
            }
        }

        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            //Log.d(TAG, "onReceive() obtained screen on event");
            if (mIssueIntentOnScreenOn
                    && tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK
                    && tm.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
                startLockScreenIntent = true;
                //mIssueIntentOnScreenOn = false;  // Moving below to prevent delay timer in this event
            }

            // Stop the delay lock service
            context.stopService(new Intent(context, LockDelayService.class));
            /*
            // Regardless, we should be clearing any runnables seeking to turn the screen off
            if (mDelayHandler != null && mDelayRunnable != null) {
                mDelayHandler.removeCallbacks(mDelayRunnable);
            }*/
        }
        else {
            Log.e(TAG, "onReceive() received unanticipated event.");
            startLockScreenIntent = false;
        }

        if (startLockScreenIntent) {

            SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.file_lock_screen_type),
                    Context.MODE_PRIVATE);
            String lockScreenType;
            Intent newIntent;

            lockScreenType = prefs.getString(
                    context.getString(R.string.key_lock_screen_type),
                    null);

            if (lockScreenType != null) {
                newIntent = new Intent(context, LockScreenActivity2.class);
                if (lockScreenType.equals(
                        context.getString(R.string.value_lock_screen_type_keypad_pin))) {
                    //newIntent = new Intent(context, LockScreenKeypadPinActivity.class);
                    newIntent.putExtra(
                            context.getString(R.string.key_lock_screen_type),
                            context.getString(R.string.value_lock_screen_type_keypad_pin)
                    );
                } else if (lockScreenType.equals(
                        context.getString(R.string.value_lock_screen_type_keypad_pattern))) {
                    //newIntent = new Intent(context, LockScreenKeypadPatternActivity.class);
                    //newIntent = new Intent(context, LockScreenActivity2.class);
                    newIntent.putExtra(
                            context.getString(R.string.key_lock_screen_type),
                            context.getString(R.string.value_lock_screen_type_keypad_pattern)
                    );
                } else { //An error of some kind
                    Log.e(TAG, "No value for key " + context
                            .getString(R.string.key_lock_screen_type));
                    newIntent = new Intent(context, ErrorPageActivity.class);
                }
            } else {
                Log.e(TAG, "Unable to get the lock screen type from shared preferences.");
                return;
            }
            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // Now handle any screen off delay
            int delay = getScreenOffDelay(context);

            if (delay == 0 || mIssueIntentOnScreenOn) { // If it wanted to lock on screen on, then we better do it immediately.
                context.startActivity(newIntent);
                mIssueIntentOnScreenOn = false;
            } else {
                Intent serviceIntent = new Intent(context, LockDelayService.class);
                serviceIntent.putExtra("delay", delay);
                serviceIntent.putExtra("startTime", System.currentTimeMillis());
                serviceIntent.putExtra("lockScreenType", lockScreenType);
                Log.d(TAG, "starting delay service");
                context.startService(serviceIntent);
            }
        }
    }

    private int getScreenOffDelay(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int seconds = prefs.getInt(context.getString(R.string.key_screen_off_timer), 0);
        //Log.d(TAG, "Screen off delay is " + seconds + "s");
        return seconds * 1000;
    }
}

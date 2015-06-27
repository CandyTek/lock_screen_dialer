package com.vitaminbacon.lockscreendialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ScreenEventReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenEventRecevier";

    private static boolean mIssueIntentOnScreenOn = false;

    public ScreenEventReceiver() {
    }

    /**
     * ============================================================================================
     * This method handles the three events that trigger this class:
     * (1) The event the screen is turned off, as denoted by "Intent.ACTION_SCREEN_OFF";
     * (2) The event the screen is turned on, as denoted by "Intent.ACTION_SCREEN_ON";
     * (3) The event that device boot has completed, as denoted by "Intent.ACTION_BOOT_COMPLETED"
     * <p/>
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
        Log.d(TAG, "onReceive called");
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        boolean startLockScreenIntent = false;


        if ( intent.getAction().equals(Intent.ACTION_SCREEN_OFF) ||
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            if (tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK &&
                    tm.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
                startLockScreenIntent = true;
                mIssueIntentOnScreenOn = false;
            } else {
                Log.d(TAG, "Screen service could not be implemented because phone call active = "
                        + tm.getCallState());
                startLockScreenIntent = false;
                mIssueIntentOnScreenOn = true;
            }
        }

        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            if (mIssueIntentOnScreenOn
                    && tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK
                    && tm.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
                startLockScreenIntent = true;
                mIssueIntentOnScreenOn = false;
            }
        }
        else {
            Log.d(TAG, "onReceive() received unanticipated event.");
            startLockScreenIntent = false;
        }

        if (startLockScreenIntent) {

            SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.lock_screen_type_file_key),
                    Context.MODE_PRIVATE);
            String lockScreenType;
            Intent newIntent;

            try {
                lockScreenType = prefs.getString(
                        context.getString(R.string.lock_screen_type_value_key),
                        null);
            } catch (NullPointerException e) {
                Log.e(TAG, "Unable to access shared preferences for lock screen type");
                return;
            }

            if (lockScreenType != null) {

                if (lockScreenType.equals(
                        context.getString(R.string.lock_screen_type_value_keypad_pin))) {
                    newIntent = new Intent(context, LockScreenKeypadPinActivity.class);
                } else if (lockScreenType.equals(
                        context.getString(R.string.lock_screen_type_value_keypad_pattern))) {
                    newIntent = new Intent(context, LockScreenKeypadPatternActivity.class);
                } else { //An error of some kind
                    Log.d(TAG, "No value for key " + context
                            .getString(R.string.lock_screen_type_value_key));
                    newIntent = new Intent(context, ErrorPageActivity.class);
                }
            } else {
                Log.e(TAG, "Unable to get the lock screen type from shared preferences.");
                return;
            }

            newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(newIntent);
        }
    }
}

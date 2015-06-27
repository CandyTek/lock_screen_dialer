package com.vitaminbacon.lockscreendialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateReceiver extends BroadcastReceiver {

    public static final String TAG = "PhoneStateReceiver";
    public static final String EXTRA_PHONE_DATA_NUMBER =
            "com.vitaminbacon.lockscreendialer.PHONE_NUM_DATA";
    public static final String EXTRA_PHONE_STATE =
            "com.vitaminbacon.lockscreendialer.PHONE_STATE";
    public static final int PHONE_STATE_IDLE = 1;
    public static final int PHONE_STATE_RINGING = 2;
    public static final int PHONE_STATE_OFFHOOK = 3;
    public static final String EXTRA_PHONE_STATE_RINGING =
            "com.vitaminbacon.lockscreendialer.PHONE_STATE_RINGING";

    public PhoneStateReceiver() {

        //Log.d(TAG, "Constructor called.");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Phone state receiver onReceive called");


        if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.d(TAG, "PhoneStateReceiver**Call State=" + state);

            if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Log.d(TAG, "PhoneStateReceiver**Idle");
                Intent lockScreenIntent = getLockScreenIntent(context);
                // Now we need to call the lock screen activity back to the foreground
                //Strange error -- if this is set along with manifest setting, it doesn't want to call onNewIntent
                lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PHONE_STATE_IDLE);

                context.startActivity(lockScreenIntent);

                // Now
            }

            else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // Incoming call
                String incomingNumber =
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);


                Intent lockScreenIntent = getLockScreenIntent(context);
                lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PHONE_STATE_RINGING);
                lockScreenIntent.putExtra(EXTRA_PHONE_DATA_NUMBER, incomingNumber);

                context.startActivity(lockScreenIntent);


            } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {

            }
        } else if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            // Outgoing call -- we don't care about this also
            String outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d(TAG, "PhoneStateReceiver **Outgoing call " + outgoingNumber);
        } else {
            Log.d(TAG, "PhoneStateReceiver **unexpected intent.action=" + intent.getAction());
        }
    }

    /**
     * I suspect this can just be done in the lock screen activity
     * @param context
     * @return
     */
    /*public boolean killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager =
                    (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass =
                    Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);

        } catch (Exception ex) { // Many things can go wrong with reflection calls
            Log.d(TAG, "PhoneStateReceiver **" + ex.toString());
            return false;
        }
        return true;
    }*/

    private Intent getLockScreenIntent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);
        String lockScreenType;

        try {
            lockScreenType = prefs.getString(
                    context.getString(R.string.lock_screen_type_value_key),
                    null);
        } catch (NullPointerException e) {
            Log.e(TAG, "Unable to access shared preferences for lock screen type");
            return null;
        }

        Intent newIntent;
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
            return null;
        }
        return newIntent;
    }
}

package com.vitbac.speeddiallocker.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.vitbac.speeddiallocker.ErrorPageActivity;
import com.vitbac.speeddiallocker.LockScreenActivity;
import com.vitbac.speeddiallocker.R;

import java.util.Date;

public class PhoneCallReceiver extends PhoneStateReceiver {

    public static final String TAG = "PhoneCallReceiver";
    public static final String EXTRA_PHONE_DATA_NUMBER =
            "com.vitaminbacon.lockscreendialer.PHONE_NUM_DATA";
    public static final String EXTRA_PHONE_STATE =
            "com.vitaminbacon.lockscreendialer.PHONE_STATE";
    public static final String EXTRA_PHONE_STATE_RINGING =
            "com.vitaminbacon.lockscreendialer.PHONE_STATE_RINGING";

    // Last state, designed to handle the receipt of extras in Lollipop release


    public PhoneCallReceiver() {
        resetLastState();
    }

    /**
     * Some anomalies here.  In Lollipop, for every phone state change, two notifications are sent out
     * however during the first the TelephonyManager state does not change, only an intent is sent
     * indicating a state change.  Thus, a way to account for the Lollipop irregularity and to
     * maintain functionality with former versions is just to check the TelephonyManager.getCallState()
     * function and the intent function to make sure they match.
     */
    /*@Override
    public void onReceive(Context context, Intent intent) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Log.d(TAG, "Phone state receiver onReceive called; TelephonyManager state: " + tm.getCallState() + ", Intent state: " + intent.getStringExtra(TelephonyManager.EXTRA_STATE));


        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            //boolean isEchoState = state.equals(lastState);

            //if (state.equals(TelephonyManager.EXTRA_STATE_IDLE) && !isEchoState) {
            if (tm.getCallState() == TelephonyManager.CALL_STATE_IDLE
                    && state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                // Call ended, but need to check that we still aren't on the other line.
                Log.d(TAG, "PhoneStateReceiver**Idle");
                //Intent lockScreenIntent = new Intent(context, LockScreenLauncherActivity.class);
                Intent lockScreenIntent = getLockScreenIntent(context);
                // Now we need to call the lock screen activity back to the foreground
                //Strange error -- if this is set along with manifest setting, it doesn't want to call onNewIntent
                lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PHONE_STATE_IDLE);

                context.startActivity(lockScreenIntent);
                //} else if (state.equals(TelephonyManager.EXTRA_STATE_RINGING) && !isEchoState) {
            } else if (tm.getCallState() == TelephonyManager.CALL_STATE_RINGING
                    && state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // Incoming call
                Log.d(TAG, "PhoneStateReceiver**Ringing");
                String incomingNumber =
                        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                // Go to launcher activity, since we may need to clear any landscape orientation to avoid Galaxy S4 error
                Intent lockScreenIntent = getLockScreenIntent(context);
                lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PHONE_STATE_RINGING);
                lockScreenIntent.putExtra(EXTRA_PHONE_DATA_NUMBER, incomingNumber);

                context.startActivity(lockScreenIntent);


                //} else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK) && !isEchoState) {
            } else if (tm.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK
                    && state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                Log.d(TAG, "PhoneStateReceiver**Offhook");

            }
            // Doesn't matter whether it is an echo or not, same result
            lastState = state;

        } else if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            // Outgoing call -- we don't care about this also
            String outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d(TAG, "PhoneStateReceiver **Outgoing call " + outgoingNumber);
        } else {
            Log.d(TAG, "PhoneStateReceiver **unexpected intent.action=" + intent.getAction());
        }
    }
    */
    protected void onIncomingCallStarted(Context ctx, String number, Date start) {
        Intent lockScreenIntent = getLockScreenIntent(ctx);
        lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PhoneStateReceiver.STATE_STARTED_INCOMING_CALL);
        lockScreenIntent.putExtra(EXTRA_PHONE_DATA_NUMBER, number);

        ctx.startActivity(lockScreenIntent);
        Log.d(TAG, "Incoming call started, state " + PhoneStateReceiver.STATE_STARTED_INCOMING_CALL);
    }

    protected void onOutgoingCallStarted(Context ctx, String number, Date start) {

        Intent lockScreenIntent = getLockScreenIntent(ctx);
        lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PhoneStateReceiver.STATE_STARTED_OUTGOING_CALL);
        lockScreenIntent.putExtra(EXTRA_PHONE_DATA_NUMBER, number);

        ctx.startActivity(lockScreenIntent);
        Log.d(TAG, "Outgoing call started, state " + PhoneStateReceiver.STATE_STARTED_OUTGOING_CALL);
    }

    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end) {
        Intent lockScreenIntent = getLockScreenIntent(ctx);
        // Now we need to call the lock screen activity back to the foreground
        //Strange error -- if this is set along with manifest setting, it doesn't want to call onNewIntent
        lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PhoneStateReceiver.STATE_ENDED_INCOMING_CALL);

        ctx.startActivity(lockScreenIntent);
        Log.d(TAG, "Incoming call ended, state " + PhoneStateReceiver.STATE_ENDED_INCOMING_CALL);
    }

    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end) {
        Intent lockScreenIntent = getLockScreenIntent(ctx);
        // Now we need to call the lock screen activity back to the foreground
        //Strange error -- if this is set along with manifest setting, it doesn't want to call onNewIntent
        lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PhoneStateReceiver.STATE_ENDED_OUTGOING_CALL);
        ctx.startActivity(lockScreenIntent);
        Log.d(TAG, "Outgoing call ended, state " + PhoneStateReceiver.STATE_ENDED_OUTGOING_CALL);
    }

    protected void onMissedCall(Context ctx, String number, Date start) {
        Intent lockScreenIntent = getLockScreenIntent(ctx);
        // Now we need to call the lock screen activity back to the foreground
        //Strange error -- if this is set along with manifest setting, it doesn't want to call onNewIntent
        lockScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        lockScreenIntent.putExtra(EXTRA_PHONE_STATE, PhoneStateReceiver.STATE_MISSED_CALL);
        ctx.startActivity(lockScreenIntent);
        Log.d(TAG, "Device missed call, state " + PhoneStateReceiver.STATE_MISSED_CALL);
    }

    /**
     * Because of strange black out errors on the Galaxy S4 when the lock screen is activated over
     * a rotated activity, we need to route the intent through the defunct lock screen launcher
     * activity, which now displays a splash screen.
     *
     * @param context
     * @return
     */
    private Intent getLockScreenIntent(Context context) {

        SharedPreferences prefs = context.getSharedPreferences(context.getString(R.string.file_lock_screen_type),
                Context.MODE_PRIVATE);
        String lockScreenType;

        try {
            lockScreenType = prefs.getString(
                    context.getString(R.string.key_lock_screen_type),
                    null);
        } catch (NullPointerException e) {
            Log.e(TAG, "Unable to access shared preferences for lock screen type");
            return null;
        }

        Intent newIntent;
        if (lockScreenType != null) {
            newIntent = new Intent(context, LockScreenActivity.class);
            if (lockScreenType.equals(
                    context.getString(R.string.value_lock_screen_type_keypad_pin))) {
                newIntent.putExtra(
                        context.getString(R.string.key_lock_screen_type),
                        context.getString(R.string.value_lock_screen_type_keypad_pin)
                );
            } else if (lockScreenType.equals(
                    context.getString(R.string.value_lock_screen_type_keypad_pattern))) {
                //newIntent = new Intent(context, LockScreenKeypadPatternActivity.class);
                newIntent.putExtra(
                        context.getString(R.string.key_lock_screen_type),
                        context.getString(R.string.value_lock_screen_type_keypad_pattern)
                );
                //Log.d(TAG, "Phone receiver starting intent to pattern activity");
            } else { //An error of some kind
                Log.d(TAG, "No value for key " + context
                        .getString(R.string.key_lock_screen_type));
                newIntent = new Intent(context, ErrorPageActivity.class);
            }
        } else {
            Log.e(TAG, "Unable to get the lock screen type from shared preferences.");
            return null;
        }
        //Intent newIntent = new Intent(context, LockScreenLauncherActivity.class);
        return newIntent;
    }
}

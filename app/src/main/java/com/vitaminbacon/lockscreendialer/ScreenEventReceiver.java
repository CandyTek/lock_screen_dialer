package com.vitaminbacon.lockscreendialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class ScreenEventReceiver extends BroadcastReceiver {

    private static final String TAG = "ScreenEventRecevier";

    public static boolean wasScreenOn = true;

    public ScreenEventReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        /*
        ============================================================================================
        This method handles the three events that trigger this class:
            (1) The event the screen is turned off, as denoted by "Intent.ACTION_SCREEN_OFF";
            (2) The event the screen is turned on, as denoted by "Intent.ACTION_SCREEN_ON";
            (3) The event that device boot has completed, as denoted by "Intent.ACTION_BOOT_COMPLETED"

        Note that this scrivener's current understanding of the best manner to invoke an activity in
         response to these events is to do so when the screen is turned off, so that the user does
         not experience a possible modest delay in transitioning to the new activity.  Naturally,
         this class can only handle the boot completion event when it occurs, transition delay or
         not.  With respect to handling the screen on event, scrivener believes this is to prevent
         other applications from utilizing this event.
         ===========================================================================================
         */



        // Create a new intent that directs to the lockscreen
        if ( intent.getAction().equals(Intent.ACTION_SCREEN_OFF) ||
                intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            //Log.d(TAG, "onReceive() received ACTION_SCREEN_OFF or ACTION_BOOT_COMPLETED");
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            // Don't lock while on the phone TODO: handle situation where screen stays black after phone call, or just get rid of this
            if (tm.getCallState() != TelephonyManager.CALL_STATE_OFFHOOK &&
                    tm.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
                Intent lockScreenIntent = new Intent(context, LockScreenLauncherActivity.class);
                lockScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(lockScreenIntent);
            } else {
                Log.d(TAG, "Screen service could not be implemented because phone call active = " + tm.getCallState());
            }

        }
/*        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

            Log.d(LockScreenEventReceiver.class.getSimpleName(), "onReceive() received event ACTION_SCREEN_OFF.");

            Intent lockScreenIntent = getLockScreenActivityIntent(context);
            context.startActivity(lockScreenIntent);

        }
        else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            Log.d(LockScreenEventReceiver.class.getSimpleName(), "onReceive() received event ACTION_BOOT_COMPLETED.");
            Intent lockScreenIntent = getLockScreenActivityIntent(context);
            context.startActivity(lockScreenIntent);
        }*/
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            //Log.d(TAG, "onReceive() received event ACTION_SCREEN_ON.");

            // Do nothing, since this event should have been handled after ACTION_SCREEN_OFF
        }
        else {
            Log.d(TAG, "onReceive() received unanticipated event.");
        }
    }

/*    private Intent getLockScreenActivityIntent(Context context){

        Intent intent;
        // Get the lock screen type from sharedPref

        SharedPreferences sharedPref = context.getSharedPreferences(
                context.getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        String lockScreenType = sharedPref.getString(
                context.getString(R.string.lock_screen_type_value_key),
                null);

        if(lockScreenType != null &&
                lockScreenType.equals(context.getString(R.string.lock_screen_type_value_keypad_pin))){ // Now enable the correct lock screen
            intent = new Intent (context, LockScreenKeypadPinActivity.class);
            Log.d(TAG, "Keypad PIN fragment to be implemented.");
        } //TODO: enable other lock screen types
        else { //An error of some kind
            Log.d(TAG, "No value for key " + context.getString(R.string.lock_screen_type_value_key));
            intent = new Intent (context, ErrorPageActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // necessary to add to android's stack of things to do
        intent.addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR); //this allows the activity to be placed on top of everything -- UGLY HACK??
        return intent;
    }*/
}

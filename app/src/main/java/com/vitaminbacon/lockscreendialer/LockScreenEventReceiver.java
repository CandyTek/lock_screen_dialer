package com.vitaminbacon.lockscreendialer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LockScreenEventReceiver extends BroadcastReceiver {
    public static boolean wasScreenOn = true;

    public LockScreenEventReceiver() {
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
        Intent lockscreenIntent = new Intent(context, LockScreenActivity.class);
        lockscreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // necessary to add to android's stack of things to do

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {

            Log.d(LockScreenEventReceiver.class.getSimpleName(), "onReceive() received event ACTION_SCREEN_OFF.");
            context.startActivity(lockscreenIntent);
        }
        else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            Log.d(LockScreenEventReceiver.class.getSimpleName(), "onReceive() received event ACTION_BOOT_COMPLETED.");
            context.startActivity(lockscreenIntent);
        }
        else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

            Log.d(LockScreenEventReceiver.class.getSimpleName(), "onReceive() received event ACTION_SCREEN_ON.");

            // Do nothing, since this event should have been handled after ACTION_SCREEN_OFF
        }
        else {
            Log.d(LockScreenEventReceiver.class.getSimpleName(), "onReceive() received unanticipated event.");
        }
    }
}

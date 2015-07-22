package com.vitbac.speeddiallocker;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

/**
 * Created by nick on 5/27/15.
 * Looks like we have to do a broadcast receiver situation, even though it is more difficult, because
 * to fire off an intent to return to the lock screen on completion of the call must be done in receiver
 */
public class MyPhoneStateListener extends PhoneStateListener {
    private OnPhoneStateChangeInterface mListener;

    public MyPhoneStateListener(Context context) {
        super();

        try {
            mListener = (OnPhoneStateChangeInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnPhoneStateChangeInterface");
        }
    }

    @Override
    public void onCallStateChanged (int state, String incomingNumber) {

        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:  // Change to no pending phone activity

                break;

            case TelephonyManager.CALL_STATE_OFFHOOK:  // At least one conversation ongoing

                break;

            case TelephonyManager.CALL_STATE_RINGING:  // Incoming call detected

                break;
        }
    }

    public interface OnPhoneStateChangeInterface {
        public void callStateIdle();
        public void callStateRinging();
    }


}

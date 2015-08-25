package com.vitbac.speeddiallocker.services;

/**
 * Created by nick on 7/6/15.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Date;

public abstract class PhoneStateReceiver extends BroadcastReceiver {

    public static final int STATE_STARTED_INCOMING_CALL = 0;
    public static final int STATE_STARTED_OUTGOING_CALL = 1;
    public static final int STATE_ENDED_INCOMING_CALL = 2;
    public static final int STATE_ENDED_OUTGOING_CALL = 3;
    public static final int STATE_MISSED_CALL = 4;


    //The receiver will be recreated whenever android feels like it.  We need a static variable to remember data between instantiations
    private static String TAG = "PhoneStateReceiver";
    private static int lastState = TelephonyManager.CALL_STATE_IDLE;
    private static Date callStartTime;
    private static boolean isIncoming;
    private static String savedNumber;  //because the passed incoming is only valid in ringing


    @Override
    public void onReceive(Context context, Intent intent) {

        //We listen to two intents.  The new outgoing call only tells us of an outgoing call.  We use it to get the number.
        /*if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
            savedNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
        }
        else{*/
        String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
        String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

        int state = 0;
        if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
            state = TelephonyManager.CALL_STATE_IDLE;
            Log.d(TAG, "*******Received IDLE intent re: phone number " + number);
        } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
            state = TelephonyManager.CALL_STATE_OFFHOOK;
            Log.d(TAG, "*******Received OFFHOOK intent re: phone number " + number);
        } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            state = TelephonyManager.CALL_STATE_RINGING;
            Log.d(TAG, "*******Received RINGING intent re: phone number " + number);
        }

        //  Need to add robustness to deal with Lollipop's issuing of two intents for each state
        // change -- we know it is a valid intent if TelephonyManager.getCallState() equals
        // the value derived above
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getCallState() == state) {
            //Log.d(TAG, "Received intent with state " + state);
            onCallStateChanged(context, state, number);
        } else {
            Log.d(TAG, "!!!!!!!!!!Received intent with state " + state
                    + " that is inconsistent with TelephonyManager.getCallState() "
                    + tm.getCallState());
        }
        //}
    }

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        //Log.d(TAG, "onCallStateChanged:  lastState = " + lastState + " state = " + state);
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                savedNumber = number;
                if (lastState == TelephonyManager.CALL_STATE_OFFHOOK) {
                    // Then it is a call waiting state
                    onCallWaitingStarted(context, number, callStartTime);
                } else {
                    onIncomingCallStarted(context, number, callStartTime);
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    onOutgoingCallStarted(context, savedNumber, callStartTime);
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    onMissedCall(context, savedNumber, callStartTime);
                } else if (isIncoming) {
                    onIncomingCallEnded(context, savedNumber, callStartTime, new Date());
                } else {
                    onOutgoingCallEnded(context, savedNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }

    protected void resetLastState() {
        lastState = TelephonyManager.CALL_STATE_IDLE;
    }

    //Derived classes should override these to respond to specific events of interest
    abstract protected void onIncomingCallStarted(Context ctx, String number, Date start);

    abstract protected void onCallWaitingStarted(Context ctx, String number, Date start);

    abstract protected void onOutgoingCallStarted(Context ctx, String number, Date start);

    abstract protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

    abstract protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

    abstract protected void onMissedCall(Context ctx, String number, Date start);
}

package com.vitaminbacon.lockscreendialer;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateService extends Service {

    private final static String TAG = "PhoneStateService";
    private final IBinder mBinder = new PhoneStateBinder();
    private PhoneStateReceiver mReceiver;


    public PhoneStateService() {
        Log.d(TAG, "Constructor called.");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate called");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand called");
        IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        mReceiver = new PhoneStateReceiver();
        registerReceiver(mReceiver, filter);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service called onDestroy().");
        unregisterReceiver (mReceiver);
        super.onDestroy();
    }


    public class PhoneStateBinder extends Binder {
        PhoneStateService getService() {
            return PhoneStateService.this;
        }
    }
}

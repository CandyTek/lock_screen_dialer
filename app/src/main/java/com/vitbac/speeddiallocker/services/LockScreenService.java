package com.vitbac.speeddiallocker.services;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class LockScreenService extends Service {

    public static final String ACTION_NAME = "com.vitaminbacon.lockscreendialer.services.LockScreenService";
    private static String TAG = "LockScreenService";
    private static ScreenEventReceiver mReceiver;

    public LockScreenService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // Not a binding service; should run indefinitely, not tied to the activity
    }


    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        super.onCreate();

        KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = km.newKeyguardLock("IN");
        keyguardLock.disableKeyguard();

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        //filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mReceiver = new ScreenEventReceiver();
        registerReceiver(mReceiver, filter);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Want service to continue running until it is explicitly stopped
        // This may be default behavior already, but won't hurt
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        try {
            unregisterReceiver(mReceiver);
            //Log.d(TAG, "Screen receiver unregistered");
        } catch (IllegalStateException e) {
            Log.w(TAG, "Lock screen receiver already unregistered", e);
        }
        super.onDestroy();

    }

}

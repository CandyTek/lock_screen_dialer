package com.vitaminbacon.lockscreendialer;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class LockScreenService extends Service {

    private static LockScreenEventReceiver mReceiver;  // TODO: consider making this static?
    public static final String ACTION_NAME = "com.vitaminbacon.lockscreendialer.LockScreenService";

    public LockScreenService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;  // Not a binding service; should run indefinitely, not tied to the activity
    }

    @Override
    public void onCreate() {
        super.onCreate();

        KeyguardManager km = (KeyguardManager)getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock keyguardLock = km.newKeyguardLock("IN");
        keyguardLock.disableKeyguard();

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mReceiver = new LockScreenEventReceiver();
        registerReceiver(mReceiver, filter);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver (mReceiver);
        super.onDestroy();
    }

}

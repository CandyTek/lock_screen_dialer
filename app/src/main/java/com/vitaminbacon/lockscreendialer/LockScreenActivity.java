package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.net.Uri;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Handler;


public class LockScreenActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "LSActivity";


    // Variables to implement TYPE_SYSTEM_ERROR stuff
    private WindowManager winManager;
    private RelativeLayout mWrapperView;

    // Variables to utilize phone state service and handle phone calls
    private boolean mPhoneCallActiveFlag;
    private String mPhoneNumOnCall;
    private String mPhoneTypeOnCall;
    private String mContactNameOnCall;

    // Timer to handle on long clicks using the ontouchlistener -- this will presumably be used by all instances
    protected Handler mHandler;
    protected DialerRunnable mRunnable;
    protected boolean mLongPressFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);

        mWrapperView = new RelativeLayout(getBaseContext());

        // Implement some of the WindowManager TYPE_SYSTEM_ERROR hocus pocus
        WindowManager.LayoutParams localLayoutParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // To avoid notification bar
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // Same
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, //Same
                        PixelFormat.TRANSLUCENT);
        this.winManager = ((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE));
        getWindow().setAttributes(localLayoutParams);
        View.inflate(this, R.layout.activity_lock_screen_keypad_pin, this.mWrapperView);
        this.winManager.addView(this.mWrapperView, localLayoutParams);

        // Check that the layout has the requisite phone-related elements for this activity to function
        if (mWrapperView.findViewById(R.id.lock_screen_end_call_button) == null ||
                mWrapperView.findViewById(R.id.lock_screen_call_display) == null) {
            throw new ClassCastException(this.toString()
                    + " must use appropriate XML layout with correct IDs and correct types." );
        }
        try {
            ImageButton b = (ImageButton)mWrapperView.findViewById(R.id.lock_screen_end_call_button);
            RelativeLayout rl = (RelativeLayout) b.getParent(); // ensures the correct encapsulating layout is there
            TextView v = (TextView)mWrapperView.findViewById(R.id.lock_screen_call_display);
        } catch (ClassCastException e) {
            throw new ClassCastException(this.toString()
                    + " must use appropriate XML layout with correct IDs and correct types.");
        }

        instantiateOptionalViewsInView();

        // begin the phone state service to listen to phone call information; supposedly only one service of a kind can exist
        startService(new Intent(this, PhoneStateService.class));

        // Check if this activity was passed any extra data
        int phoneState = getIntent().getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0);
        if (phoneState == PhoneStateReceiver.PHONE_STATE_IDLE) {
            mPhoneCallActiveFlag = false;
            startService(new Intent(this, LockScreenService.class)); // Could just call this outside if stmt, but probably more efficient here
        }
        else if (isCallActive()) {
            mPhoneCallActiveFlag = true;  // we can handle the display stuff in onResume
        } else {
            mPhoneCallActiveFlag = false; // Possibly revised later by onResumeInstanceState
        }
    }

    @Override
    public void onResume(){
        // if there is still a phone call active, we need to set the display to handle that
        //Log.d(TAG, "onResume() called.");
        if (mPhoneCallActiveFlag) {
            enableCallViewsInView(mPhoneNumOnCall, mContactNameOnCall, mPhoneTypeOnCall);
            disableOptionalViewsInView();
        } else {
            disableCallViewsInView();
            enableOptionalViewsInView();
        }

        super.onResume();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int receivedPhoneState = getIntent().getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0);

        // Only want to reset the old saved flag if this activity wasn't recalled by virtue of the phone being hung up.
        if (receivedPhoneState != PhoneStateReceiver.PHONE_STATE_IDLE) {
            mPhoneCallActiveFlag = savedInstanceState.getBoolean("phoneCallActiveFlag");
        }
        mContactNameOnCall = savedInstanceState.getString("contactNameOnCall");
        mPhoneNumOnCall = savedInstanceState.getString("phoneNumOnCall");
        mPhoneTypeOnCall = savedInstanceState.getString("phoneTypeOnCall");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("phoneCallActiveFlag", mPhoneCallActiveFlag);
        if (mContactNameOnCall != null) {
            outState.putString("contactNameOnCall", mContactNameOnCall);
        }
        if (mPhoneNumOnCall != null) {
            outState.putString("phoneNumOnCall", mPhoneNumOnCall);
        }
        if (mPhoneTypeOnCall != null) {
            outState.putString("phoneTypeOnCall", mPhoneTypeOnCall);
        }
    }

    /**
     * Critical function that gathers data from the phone state receiver as to the status of
     * phone calls
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        int phoneState = intent.getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0); // returns 0 if doesn't exist

        Log.d(TAG, "onNewIntent called, phoneState = " + phoneState);

        if (phoneState == PhoneStateReceiver.PHONE_STATE_IDLE) {
            // Phone was just hung up
            mPhoneCallActiveFlag = false;
            mContactNameOnCall = mPhoneNumOnCall = mPhoneTypeOnCall = null;
            disableCallViewsInView();
            enableOptionalViewsInView();
            startService(new Intent(this, LockScreenService.class)); // reenable the off-screen receiver
        }
        else if (phoneState == PhoneStateReceiver.PHONE_STATE_RINGING) { // a call has been received, we should handle lock screen in case user returns there

            // This implementation ends the lock screen, but it should be recalled by the receiver once the call is over
            stopService(new Intent(this, LockScreenService.class));  // don't want the lock screen to keep popping up during a phone call in this implementation
            finish();

            // Below implementation supports handling the call in the lock screen
            /*mPhoneCallInitiated = true;
            mPhoneNumOnCall = intent.getStringExtra(PhoneStateReceiver.EXTRA_PHONE_DATA_NUMBER);
            enableCallButtonsInView(mPhoneNumOnCall, null, null);*/ // TODO: worth trying to get Contacts data for the number?
            //moveTaskToBack(true); // for now, don't have the activity come to the foreground
            //Log.d(TAG, "New Intent called, Phone State Ringing from " + mPhoneNumOnCall);

            //TODO: implement setting for whether user wants to handle phone calls in lock screen or not?
        }
        else if (phoneState == PhoneStateReceiver.PHONE_STATE_OFFHOOK) {
            // Currently requires no implementation
            return;
        }

    }


    /**
     * Simple functions that makes the call views visible/invisible and hides unnecessary views.
     * This is the implementation of this parent class that requires certain view IDs --
     * @param telNum - a string with the telephone number
     * @param name - a string to display the contact's name
     * @param type - a string to display the phone number type
     */
    private void enableCallViewsInView(String telNum, String name, String type){
        //Display the end call button
        ImageButton endCallBtn = (ImageButton) mWrapperView.
                findViewById(R.id.lock_screen_end_call_button);
        RelativeLayout rl = (RelativeLayout) endCallBtn.getParent();
        rl.setVisibility(View.VISIBLE);  // Needed to set both the parent and child to get the layout to play nice
        endCallBtn.setVisibility(View.VISIBLE);
        endCallBtn.setOnClickListener(this);

        //Display the information regarding the number we are dialing
        TextView dialInfoView = (TextView) mWrapperView.findViewById(R.id.lock_screen_call_display);
        dialInfoView.setText(getDialInfoViewText(telNum, name, type));
        dialInfoView.setVisibility(View.VISIBLE);

        // Hide the clock
        /*mWrapperView.findViewById(R.id.lock_screen_clock).setVisibility(View.GONE);
        // Hide the date
        mWrapperView.findViewById(R.id.lock_screen_date).setVisibility(View.GONE);
        // Hide the other information
        mWrapperView.findViewById(R.id.lock_screen_info).setVisibility(View.GONE);*/


    }

    private void disableCallViewsInView() {
        ImageButton endCallBtn = (ImageButton) mWrapperView.
                findViewById(R.id.lock_screen_end_call_button);
        RelativeLayout rl = (RelativeLayout) endCallBtn.getParent();
        endCallBtn.setVisibility(View.INVISIBLE);
        rl.setVisibility(View.INVISIBLE);

        mWrapperView.findViewById(R.id.lock_screen_call_display).setVisibility(View.GONE);

        // TODO: when clock etc settings functionality implemented, should check for user settings before making visible
        /*mWrapperView.findViewById(R.id.lock_screen_clock).setVisibility(View.VISIBLE);

        mWrapperView.findViewById(R.id.lock_screen_date).setVisibility(View.VISIBLE);

        mWrapperView.findViewById(R.id.lock_screen_info).setVisibility(View.VISIBLE);*/

    }

    private void enableOptionalViewsInView(){
        //Log.d(TAG, "Enabling Optional Views");
        setOptionalViewsInView(View.VISIBLE);
    }

    private void disableOptionalViewsInView() {
        //Log.d(TAG, "Disabling Optional Views");
        setOptionalViewsInView(View.GONE);
    }

    /**
     * Sets the views defined by the IDs in the XML array to the value parameter, typically either
     * View.GONE or View.VISIBILE
     * @param value
     */
    private void setOptionalViewsInView(int value) {

        if (value != View.GONE && value != View.VISIBLE && value != View.INVISIBLE) {
            Log.e(TAG, "Invalid argument passed to setOptionalViewsInView");
            return;
        }

        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);

        if (keys.length() != ids.length()) {  // TODO: excpetion?
            Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i=0; i < keys.length(); i++) {
            //Log.d(TAG, "Iteration " + i + " - value " + value + " - " + keys.getString(i));
            View view = mWrapperView.findViewById(ids.getResourceId(i, -1));
            if (view == null) { // There should always be at least an existing id somewhere for the view in the XML
                Log.e(TAG, "Iteration " + i + " of key " + keys.getString(i)
                        + " in setOptionalViewsInView() has invalid id.");
                continue;
            }

            // access to SharedPrefs throws a class cast exception if stored type is not as sought - FYI
            switch (view.getId()) {
                /*case R.id.lock_screen_clock:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(value);
                    }
                    break;
                case R.id.lock_screen_date:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(value);
                    }
                    break;*/
                case R.id.lock_screen_info:
                    if (!prefs.getString(keys.getString(i), "").equals("")) {
                        view.setVisibility(value);
                    }
                    break;
                /*case R.id.lock_screen_camera:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(value);
                    }
                    break;*/
            }
        }


            /*try {

                if (prefs.getBoolean(keys.getString(i), false) ) {
                    int viewId = ids.getResourceId(i, -1); // gets the corresponding id for that view (hopefully)
                    View view = mWrapperView.findViewById(viewId);

                    // Error check for the existence of the id
                    if (view == null) {
                        Log.e(TAG, "Iteration " + i + " of key " + keys.getString(i)
                                + " in setOptionalViewsInView() has invalid id.");
                        return;
                    }

                    view.setVisibility(value);
                }*/

/*
            } else if ( prefs.getString(keys.getString(i), null) != null ){ // handle other types of preferences
                int viewId = ids.getResourceId(i, -1);
                View view = mWrapperView.findViewById(viewId);
                if (view != null) {
                    view.setVisibility(value);
                } else {
                    Log.e(TAG, "Iteration " + i + " of key " + keys.getString(i)
                            + " in setOptionalViewsInView() has invalid id.");
                }
*/

    }

    private void instantiateOptionalViewsInView() {
        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int viewId;
        View view;

        if (keys.length() != ids.length()) {  // TODO: excpetion?
            Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            return;
        }

        for (int i=0; i < keys.length(); i++){
            viewId = ids.getResourceId(i, -1); // gets the corresponding id for that view (hopefully)
            view = mWrapperView.findViewById(viewId);

            if (view == null) { // Error check
                Log.e(TAG, "Iteration " + i + " of key " + keys.getString(i)
                        + " in setOptionalViewsInView() has invalid id.");
                return;
            }

            switch (viewId) {
                case R.id.lock_screen_date:
                    SimpleDateFormat df = new SimpleDateFormat(getString(R.string.date_format));
                    try {
                        ((TextView) view).setText(df.format(new Date()));
                    } catch (ClassCastException e) {
                        Log.e(TAG, "Illegal casting of TextView to view of lock screen date.");
                    }
                    break;
                case R.id.lock_screen_clock:
                    // do nothing; view already takes care of its own value
                    break;
                case R.id.lock_screen_info:
                    String s = prefs.getString(keys.getString(i), "");
                    if (s.equals("")) {  //equivalent of no value
                        continue;
                    } else {
                        try {
                            ((TextView) view).setText(s);
                            //Log.d(TAG, "Set value for lock screen info.");
                        } catch (ClassCastException e) {
                            Log.e(TAG, "Illegal casting of TextView to view of lock screen info.");
                        }
                    }
                    break;

            }
        }

    }

    /**
     * Method that provides acceptable display information depending on the information available.
     * @param telNum
     * @param name
     * @param type
     * @return
     */
    private String getDialInfoViewText(String telNum, String name, String type){
        if (name != null && type != null ) {
            return "Call with " + name + " on " + type.toLowerCase() + "....";
        }
        else if (name != null & telNum != null) {
            return "Call with " + name + "on phone no. " + telNum + " ....";
        }
        else if (name != null) { // this shouldn't happen, but....
            return "Call with " + name + " ....";
        }
        else if (telNum != null) { // this should be case where call was received
            return "Call with phone no. " + telNum + " ....";
        }

        return "Call ongoing ....";
    }


    private void endPhoneCall() {
        TelephonyManager telephonyManager;
        Class c;
        Method m;
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        try {

            c = Class.forName(telephonyManager.getClass().getName());
            m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephonyManager);
            telephonyService.endCall();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        mPhoneCallActiveFlag = false;


    }

    private boolean isCallActive() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager.getMode() == AudioManager.MODE_IN_CALL) {
            return true;
        } else {
            return false;
        }
    }

    public void onClick (View view) {
        if (view.getId() == R.id.lock_screen_end_call_button) {
            disableCallViewsInView();
            enableOptionalViewsInView();
            endPhoneCall();
        }
    }
    /**
     * Simple method that handles logic when the correct passcode is entered
     */
    protected void onCorrectPasscode() {
        Log.d(TAG, "Finishing activity, clearing service.");
        //unregisterReceiver(mReceiver);
        stopService(new Intent(this, PhoneStateService.class));
        finish();
    }

    protected void enableLongPressFlag () { mLongPressFlag = true; }

    protected void enablePhoneCallActiveFlag() { mPhoneCallActiveFlag = true; }

    protected boolean getPhoneCallActiveFlag() { return mPhoneCallActiveFlag; }

    protected RelativeLayout getWrapperView(){ return mWrapperView; }

    @Override
    public void onBackPressed() {  // Overrides the back button to prevent exit
        return;
    }

    // Implemented to use TYPE_SYSTEM_ERROR hack
    @Override
    public void onDestroy() {
        // TODO: is there a way to animate this?
        this.winManager.removeView(this.mWrapperView);
        this.mWrapperView.removeAllViews();

        super.onDestroy();
    }



    /**
     * Runnable class that initiates the phone call on a long press
     */
    protected class DialerRunnable implements Runnable {
        private int num; // the ID of the button pressed

        public DialerRunnable(int num) {
            this.num = num;
        }

        public void run() {

            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.speed_dial_preference_file_key),
                    Context.MODE_PRIVATE);
            String preferenceKeyPrefix = getString(R.string.key_number_store_prefix_phone);

            if (num == -1) { //error handling
                Log.d(TAG, "Error in finding view id.");
                return;
            }

            String telNum = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_phone) + num,
                    null);
            String name = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_name) + num,
                    "Unknown");
            String type = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_type) + num,
                    "Phone");

            if (telNum == null) {
                Log.d(TAG, "Error in obtaining phone number");
                return;
            }

            if (!getPhoneCallActiveFlag()) {  //Only want to initiate the call if line is idle
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + (telNum.trim())));
                startActivity(intent);

                enableCallViewsInView(telNum, name, type);
                disableOptionalViewsInView();
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(350);
                enableLongPressFlag();
                enablePhoneCallActiveFlag();
            }

        }
    }
}


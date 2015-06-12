package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.internal.telephony.ITelephony;
import com.vitaminbacon.lockscreendialer.exceptions.CallHandlerException;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Handler;
import android.widget.ToggleButton;


public class LockScreenActivity extends Activity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener{

    private final static String TAG = "LSActivity";


    // Variables to implement TYPE_SYSTEM_ERROR stuff
    private WindowManager mWindowManager;
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

    private boolean mBackgroundSetFlag;
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
        mWindowManager = ((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE));
        getWindow().setAttributes(localLayoutParams);
        View.inflate(this, R.layout.activity_lock_screen_keypad_pin, mWrapperView);
        mWindowManager.addView(mWrapperView, localLayoutParams);

        // Check that the layout has the requisite phone-related elements for this activity to function
        if (mWrapperView.findViewById(R.id.lock_screen_end_call_button) == null ||
                mWrapperView.findViewById(R.id.lock_screen_call_display) == null ||
                mWrapperView.findViewById(R.id.lock_screen_background_view) == null) {
            Log.e(TAG, "Layout incompatible with this activity for failing to have proper Views.");
            onFatalError();
            return;
            /*
            throw new ClassCastException(this.toString()
                    + " must use appropriate XML layout with correct IDs and correct types." );*/
        }
        try {
            ImageButton b = (ImageButton)mWrapperView.findViewById(R.id.lock_screen_end_call_button);
            RelativeLayout rl = (RelativeLayout) b.getParent(); // ensures the correct encapsulating layout is there
            TextView v = (TextView)mWrapperView.findViewById(R.id.lock_screen_call_display);
            ImageView iv = (ImageView)mWrapperView.findViewById(R.id.lock_screen_background_view);
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout incompatible with this activity for failing to have proper Views.");
            onFatalError();
            return;
            /*throw new ClassCastException(this.toString()
                    + " must use appropriate XML layout with correct IDs and correct types.");*/
        }

        try {
            instantiateOptionalViewsInView();
        } catch (CallHandlerException e) {
            Log.e(TAG, "Layout renders activity unable to handle calls", e);
            onFatalError();
            return;
        }

        mBackgroundSetFlag = false;

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
        super.onResume();
        try {
            if (mPhoneCallActiveFlag) {
                enableCallViewsInView(mPhoneNumOnCall, mContactNameOnCall, mPhoneTypeOnCall);
                disableOptionalViewsInView();
            } else {
                disableCallViewsInView();
                enableOptionalViewsInView();
            }
            if (!mBackgroundSetFlag) {
                setActivityBackground(
                        (ImageView) mWrapperView.findViewById(R.id.lock_screen_background_view));
            }
        } catch (CallHandlerException e) {
            Log.e(TAG, "Layout renders activity unable to handle calls", e);
            onFatalError();
        }

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
        mBackgroundSetFlag = savedInstanceState.getBoolean("backgroundSetFlag");
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
        outState.putBoolean("backgroundSetFlag", mBackgroundSetFlag);
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
            try {
                enableOptionalViewsInView();
            } catch (CallHandlerException e) {
                Log.e(TAG, "Layout renders activity unable to handle calls", e);
                onFatalError();
            }

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

    @Override
    public void onBackPressed() {  // Overrides the back button to prevent exit
        return;
    }

    // Implemented to use TYPE_SYSTEM_ERROR hack
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();

        // TODO: is there a way to animate this?
        mWindowManager.removeView(mWrapperView);
        ((ImageView) mWrapperView.findViewById(R.id.lock_screen_background_view)).setImageBitmap(null);  // Probably not necessary
        mWrapperView.removeAllViews();

        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mHandler = null;
        mRunnable = null;

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
        ImageButton endCallBtn;
        RelativeLayout rl;
        TextView dialInfoView;

        try {
            endCallBtn = (ImageButton) mWrapperView.findViewById(R.id.lock_screen_end_call_button);
            rl = (RelativeLayout) endCallBtn.getParent();
            dialInfoView = (TextView) mWrapperView.findViewById(R.id.lock_screen_call_display);
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;
        }

        try {
            rl.setVisibility(View.VISIBLE);  // Needed to set both the parent and child to get the layout to play nice
            endCallBtn.setVisibility(View.VISIBLE);
            endCallBtn.setOnClickListener(this);

            //Display the information regarding the number we are dialing
            dialInfoView.setText(getDialInfoViewText(telNum, name, type));
            dialInfoView.setVisibility(View.VISIBLE);
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;
        }
    }

    private void disableCallViewsInView() {
        try {
            ImageButton endCallBtn = (ImageButton) mWrapperView.
                    findViewById(R.id.lock_screen_end_call_button);
            RelativeLayout rl = (RelativeLayout) endCallBtn.getParent();
            endCallBtn.setVisibility(View.INVISIBLE);
            rl.setVisibility(View.INVISIBLE);

            mWrapperView.findViewById(R.id.lock_screen_call_display).setVisibility(View.GONE);
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout incompatible with this activity - CCE in disableCallViewsInView()", e);
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout incompatible with this activity - NPE in disableCallViewsInView()", e);
        }
    }

    private void enableOptionalViewsInView() throws CallHandlerException {
        //Log.d(TAG, "Enabling Optional Views");
        setOptionalViewsInView(View.VISIBLE);
    }

    private void disableOptionalViewsInView() throws CallHandlerException {
        //Log.d(TAG, "Disabling Optional Views");
        setOptionalViewsInView(View.GONE);
    }

    /**
     * Sets the views defined by the IDs in the XML array to the value parameter, typically either
     * View.GONE or View.VISIBILE
     * @param value
     */
    private void setOptionalViewsInView (int value) throws CallHandlerException {

        if (value != View.GONE && value != View.VISIBLE && value != View.INVISIBLE) {
            Log.e(TAG, "Invalid argument passed to setOptionalViewsInView");
            return;
        }

        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);

        if (keys.length() != ids.length()) {  // TODO: excpetion?
            //Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            throw new CallHandlerException(this.toString()
                    + "XML arrays for keys and ids to optional views mismatched.");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i=0; i < keys.length(); i++) {
            //Log.d(TAG, "Iteration " + i + " - value " + value + " - " + keys.getString(i));

            View view = mWrapperView.findViewById(ids.getResourceId(i, -1));
            if (view == null) { // There should always be at least an existing id somewhere for the view in the XML
                Log.w(TAG, "Iteration " + i + " of key " + keys.getString(i)
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
    }

    private void instantiateOptionalViewsInView() throws CallHandlerException {
        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int viewId;
        View view;

        if (keys.length() != ids.length()) {
            //Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            throw new CallHandlerException(this.toString()
                    + "XML arrays for keys and ids to optional views mismatched.");
        }

        for (int i=0; i < keys.length(); i++){
            viewId = ids.getResourceId(i, -1); // gets the corresponding id for that view (hopefully)
            view = mWrapperView.findViewById(viewId);

            if (view == null) { // Error check
                Log.w(TAG, "Iteration " + i + " of key " + keys.getString(i)
                        + " in setOptionalViewsInView() has invalid id.");  // TODO: do we need to make this an error?
                continue;
            }

            try {

                switch (viewId) {
                    case R.id.lock_screen_date:
                        if (prefs.getBoolean(keys.getString(i), false)) {
                            SimpleDateFormat df = new SimpleDateFormat(getString(R.string.date_format));
                            ((TextView) view).setText(df.format(new Date()));
                            view.setVisibility(View.VISIBLE);
                        } else {
                            view.setVisibility(View.INVISIBLE);
                        }

                        break;

                    case R.id.lock_screen_clock:
                        if (prefs.getBoolean(keys.getString(i), false)) {
                            view.setVisibility(View.VISIBLE);
                        } else {
                            view.setVisibility(View.INVISIBLE);
                        }
                        break;

                    case R.id.lock_screen_info:
                        String s = prefs.getString(keys.getString(i), "");
                        if (s.equals("")) {  //equivalent of no value
                            continue;
                        } else {
                            ((TextView) view).setText(s);
                        }
                        break;

                    case R.id.lock_screen_speed_dial_toggle:
                        //Log.d(TAG, "instantiating toggle button");

                        /*ToggleButton toggle = (ToggleButton) mWrapperView.findViewById(
                                R.id.lock_screen_speed_dial_toggle);*/

                        TextView tv = (TextView) mWrapperView.findViewById(
                                R.id.lock_screen_speed_dial_toggle_text);
                        if (prefs.getBoolean(keys.getString(i), false)) {
                            view.setVisibility(View.VISIBLE);
                            ((ToggleButton) view).setOnCheckedChangeListener(this);
                            if (tv != null) {
                                tv.setVisibility(View.VISIBLE);
                                if (prefs.getBoolean(
                                        getString(R.string.key_toggle_speed_dial_enabled),
                                        false)) {
                                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_on));
                                    ((ToggleButton) view).setChecked(true);
                                } else {
                                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_off));
                                    ((ToggleButton) view).setChecked(false);
                                }
                            } else {
                                Log.w(TAG, "Layout does not have toggle button text");
                            }
                        } else {
                            view.setVisibility(View.INVISIBLE);
                            if (tv != null) {
                                tv.setVisibility(View.INVISIBLE);
                            } else {
                                Log.w(TAG, "Layout does not have toggle button text");
                            }
                        }
                        break;
                    case R.id.lock_screen_speed_dial_toggle_text:
                        if (prefs.getBoolean(keys.getString(i), false)) {  // We will use the same key as lock_screen_speed_dial_toggle in the XML array


                        } else {
                            Log.w(TAG, "No toggle-button text view in this layout.");
                        }

                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Layout incompatible with activity", e);
                onFatalError();
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
        switch (view.getId()) {
            case R.id.lock_screen_end_call_button:
                disableCallViewsInView();
                try {
                    enableOptionalViewsInView();
                } catch (CallHandlerException e) {
                    Log.e(TAG, "Exception to activity handling calls", e);
                    onFatalError();
                }
                endPhoneCall();
                break;
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "lock screen speed dial toggle button pressed");
        try {
            TextView tv = (TextView) mWrapperView.findViewById(
                    R.id.lock_screen_speed_dial_toggle_text);

            if (tv != null) { // not necessary to have a text view as we implemented, so can come back null!
                if (isChecked) {
                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_on));
                } else {
                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_off));
                }
            }
        } catch (ClassCastException e) {
            // This isn't a fatal error.
            Log.w(
                    TAG,
                    "Activity does not have valid lock screen toggle text view for custom functionality",
                    e);
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

    protected void onFatalError() {
        Log.d(TAG, "onFatalError() called.");
        stopService(new Intent(this, PhoneStateService.class));
        finish();

        //TODO: some kind of dialog or toast or something?
    }

    protected void enableLongPressFlag () { mLongPressFlag = true; }

    protected void enablePhoneCallActiveFlag() { mPhoneCallActiveFlag = true; }

    protected boolean getPhoneCallActiveFlag() { return mPhoneCallActiveFlag; }

    protected RelativeLayout getWrapperView(){ return mWrapperView; }


    private void setActivityBackground(ImageView view) {
        SharedPreferences prefs = getSharedPreferences(
                getString(R.string.background_file_key),
                MODE_PRIVATE);
        int color = prefs.getInt(getString(R.string.key_background_color), -1);
        String filePath = prefs.getString(getString(R.string.key_background_pic), null);
        int orientation = prefs.getInt(getString(R.string.key_background_orientation), -1);
        File file= null;
        if (filePath != null) {
            file = new File(filePath);
        }

        if (color != -1) { // since color is set, we just set the background to that
            Log.d(TAG, "setting activity to a color");
            view.setImageBitmap(null);
            view.setBackgroundColor(color);
            mBackgroundSetFlag = true;
        } else if (filePath == null) { // then we have the default image situation
            Log.d(TAG, "setting activity to default image");
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.background_default);
            view.setImageBitmap(bitmap);
            mBackgroundSetFlag = true;
        } else if (file != null && file.exists()){ //now we must retrieve and set up the stored picture
            Log.d(TAG, "getting image from stored data");
            Display display = getWindowManager().getDefaultDisplay();
            Bitmap bitmap;
            int w, h;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                Point size = new Point();
                display.getSize(size);
                w = size.x;
                h = size.y;
            } else {
                w = display.getWidth();
                h = display.getHeight();
            }
            BitmapToViewHelper.go(view, filePath, orientation, w, h);
            mBackgroundSetFlag = true;
        } else {
            Log.e(TAG, "no background information to load for lock screen; neither file nor color exist");
        }
    }

    protected boolean isSpeedDialEnabled() {
        ToggleButton toggle = (ToggleButton)mWrapperView.findViewById(
                R.id.lock_screen_speed_dial_toggle);

        // TODO: OK for now, but need to check whether speed dial enabled at all in the settings
        return toggle.isChecked();
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
                Log.e(TAG, "Error in obtaining phone number");
                return;
            }

            if (!getPhoneCallActiveFlag() && isSpeedDialEnabled()) {  //Only want to initiate the call if line is idle and speed dial enabled
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + (telNum.trim())));
                startActivity(intent);

                enableCallViewsInView(telNum, name, type);
                try {
                    disableOptionalViewsInView();
                } catch (CallHandlerException e) {
                    Log.e(TAG, "Activity unable to handle calls", e);
                    onFatalError();
                }
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(350);
                enableLongPressFlag();
                enablePhoneCallActiveFlag();
            }

        }
    }
}


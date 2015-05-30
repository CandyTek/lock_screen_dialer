package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.RemoteException;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import android.os.Handler;


public class LockScreenKeypadPinActivity extends Activity
        implements View.OnClickListener, View.OnTouchListener {

    private final static String TAG = "LSKeypadPinActivity";

    // UI elements
    private TextView mPinDisplayView;
    private Button mDeleteButton;
    private Button mOkButton;
    private Button[] mkeypadButtons;

    // variables relating to the logic of the lockscreen
    private String mPinStored;
    private String mPinEntered;
    private int mNumTries;

    // Variables to implement TYPE_SYSTEM_ERROR stuff
    public WindowManager winManager;
    private RelativeLayout mWrapperView;

    // Variables to utilize phone state service and handle phone calls
    private boolean mPhoneCallInitiated;
    private String mPhoneNumOnCall;
    private String mPhoneTypeOnCall;
    private String mContactNameOnCall;

    // Timer to handle on long clicks using the ontouchlistener
    private Handler mHandler;
    private DialerRunnable mRunnable;
    private final int mLongPressThreshold = 1000; // 1.0 s
    private boolean mLongPressFlag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);

        // Initialize some basic variables
        mNumTries = 0;  // Possibly modified later by onRestoreInstanceState
        mkeypadButtons = new Button[10];
        mPinStored = getStoredPin();
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

        mPinDisplayView = (TextView) mWrapperView.findViewById(R.id.lock_screen_pin_display);
        mPinDisplayView.setText(getString(R.string.lock_screen_pin_default_display)); // In case returning to this display from elsewhere, want to reset
        mPinEntered = ""; // Same -- we want to reset

        mDeleteButton = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_delete);
        mOkButton = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_OK);

        mkeypadButtons[0] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_0);
        mkeypadButtons[1] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_1);
        mkeypadButtons[2] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_2);
        mkeypadButtons[3] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_3);
        mkeypadButtons[4] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_4);
        mkeypadButtons[5] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_5);
        mkeypadButtons[6] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_6);
        mkeypadButtons[7] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_7);
        mkeypadButtons[8] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_8);
        mkeypadButtons[9] = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_9);

        // Set the onClickListeners to the appropriate views
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE
        );

        for (int i=0; i < 9; i++) {
            mkeypadButtons[i].setOnClickListener(this); // all buttons will have an onClickListener

            String filename = getString(R.string.key_number_store_prefix_phone) + i;
            //Log.d(TAG, "Attempting to access " + filename);
            if (sharedPref.getString(filename, null) != null) { //only set the long click where necessary
                //Log.d(TAG, "Setting long click on key " + i);
                mkeypadButtons[i].setOnTouchListener(this);
            }
        }
        mDeleteButton.setOnClickListener(this);
        mOkButton.setOnClickListener(this);



        if (mPinStored == null) {
            Log.d(TAG, "Stored PIN is null.");
            onCorrectPasscode(); //For now, just exit.  TODO: find good way to handle these errors.
        }



        // Initialize the receiver; must always be re-instantiated in onCreate, since activity was destroyed or just started
//        IntentFilter filter = new IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
//        mReceiver = new PhoneStateReceiver();
//        registerReceiver(mReceiver, filter);
        startService(new Intent(this, PhoneStateService.class));
        mPhoneCallInitiated = false; // Possibly revised later by onResumeInstanceState

    }

    @Override
    public void onResume(){



        // if there is still a phone call active, we need to set the display to handle that
        if (mPhoneCallInitiated) {
            enableCallButtonsInView(mPhoneNumOnCall, mContactNameOnCall, mPhoneTypeOnCall);
        }

        super.onResume();
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mNumTries = savedInstanceState.getInt("numTries");
        mPhoneCallInitiated = savedInstanceState.getBoolean("phoneCallInitiated");
        mContactNameOnCall = savedInstanceState.getString("contactNameOnCall");
        mPhoneNumOnCall = savedInstanceState.getString("phoneNumOnCall");
        mPhoneTypeOnCall = savedInstanceState.getString("phoneTypeOnCall");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("numTries", mNumTries);
        outState.putBoolean("phoneCallInitiated", mPhoneCallInitiated);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        int phoneState = intent.getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0); // returns 0 if doesn't exist

        Log.d(TAG, "onNewIntent called, phoneState = " + phoneState);

        if (phoneState == PhoneStateReceiver.PHONE_STATE_IDLE) {
            // Phone was just hung up
            mPhoneCallInitiated = false;
            mContactNameOnCall = mPhoneNumOnCall = mPhoneTypeOnCall = null;
            disableCallButtonsInView();
        }
        else if (phoneState == PhoneStateReceiver.PHONE_STATE_RINGING) { // a call has been received, we should handle lock screen in case user returns there

            // This implementation ends the lock screen, but it should be recalled by the receiver once the call is over
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


    public void onClick (View view) {
        if (mLongPressFlag) {
            mLongPressFlag = false;
            return;
        }
        int num = getSpeedDialButtonPressed(view.getId(), -1);
        if (num == -1) { //Not a speed dial number
            if (view.getId() == R.id.lock_screen_pin_button_OK) {
                wrongPinEntered();  // Because our functionality will automatically accept a PIN when it is entered, we can assume error
            } else if (view.getId() == R.id.lock_screen_end_call_button) {
                disableCallButtonsInView();
                endPhoneCall();
            } else {
                Log.d(TAG, "delete button pressed.");
                resetPinEntry();
            }
            return;
        }

        mPinEntered += num;

        // Display a new "digit" on the text view
        if (mPinDisplayView.getText().toString().equals(getString(R.string.lock_screen_pin_default_display))){
            mPinDisplayView.setText("*");
            mDeleteButton.setVisibility(View.VISIBLE);
        } else {
            mPinDisplayView.setText(mPinDisplayView.getText().toString() + '*');
        }

        //  Now check whether the PIN entered so far matches the stored PIN
        //Log.d(TAG, mPinEntered + " vs " + mPinStored);
        if (mPinEntered.equals(mPinStored)) {
            onCorrectPasscode();
        }
    }

/*    public boolean onLongClick (View view){  // TODO: will need to set a longer longClick
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);
        String preferenceKeyPrefix = getString(R.string.key_number_store_prefix_phone);

        String telNum;
        String name;
        String type;

        int speedDialNum;

        Intent intent = new Intent (Intent.ACTION_CALL);


        switch (view.getId()) {
            // TODO: set zero key as a lock screen dialer?
            case R.id.lock_screen_pin_button_1:
                speedDialNum = 1;
                break;
            case R.id.lock_screen_pin_button_2:
                speedDialNum = 2;
                break;
            case R.id.lock_screen_pin_button_3:
                speedDialNum = 3;
                break;
            case R.id.lock_screen_pin_button_4:
                speedDialNum = 4;
                break;
            case R.id.lock_screen_pin_button_5:
                speedDialNum = 5;
                break;
            case R.id.lock_screen_pin_button_6:
                speedDialNum = 6;
                break;
            case R.id.lock_screen_pin_button_7:
                speedDialNum = 7;
                break;
            case R.id.lock_screen_pin_button_8:
                speedDialNum = 8;
                break;
            case R.id.lock_screen_pin_button_9:
                speedDialNum = 9;
                break;
            default:
                speedDialNum = -1;
                break;
        }

        if (speedDialNum == -1) { //error handling
            Log.d(TAG, "Error in finding view id.");
            return false;
        }

        telNum = sharedPref.getString(
                getString(R.string.key_number_store_prefix_phone) + speedDialNum,
                null);
        name = sharedPref.getString(
                getString(R.string.key_number_store_prefix_name) + speedDialNum,
                "Unknown");
        type = sharedPref.getString(
                getString(R.string.key_number_store_prefix_type) + speedDialNum,
                "Phone");

        if (telNum == null) {
            Log.d(TAG, "Error in obtaining phone number for longClick");
            return false;
        }

        enableCallButtonsInView(telNum, name, type);
        initializePhoneStateReceiver();
        intent.setData(Uri.parse("tel:" + (telNum.trim())));
        startActivity(intent);

        return true;
    }*/

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mHandler == null) { // means there is no pending handler
                    mHandler = new Handler();
                    mRunnable = new DialerRunnable(getSpeedDialButtonPressed(v.getId(), -1));
                    mHandler.postDelayed(mRunnable, mLongPressThreshold);
                    mLongPressFlag = false;  // flag to let
                }

                break;
            case MotionEvent.ACTION_UP:
                mHandler.removeCallbacks(mRunnable);
                mHandler = null;
                mRunnable = null;
                /*if (mLongPressFlag) { // Long press was consumed by the runnable!
                    mLongPressFlag = false; // reset it
                    return true; // Hopefully this consumes the event so onClick is not triggered
                }*/

                //Now it is just a normal button press

                break;
        }

        return false;
    }


    /**
     * Simple function that makes the call views visible and hides unnecessary views
     * @param telNum - a string with the telephone number
     * @param name - a string to display the contact's name
     * @param type - a string to display the phone number type
     */
    private void enableCallButtonsInView(String telNum, String name, String type){
        //Display the end call button
        Button endCallBtn = (Button) mWrapperView.findViewById(R.id.lock_screen_end_call_button);
        endCallBtn.setVisibility(View.VISIBLE);
        endCallBtn.setOnClickListener(this);

        //Display the information regarding the number we are dialing
        TextView dialInfoView = (TextView) mWrapperView.findViewById(R.id.lock_screen_call_display);
        dialInfoView.setText(getDialInfoViewText(telNum, name, type));
        dialInfoView.setVisibility(View.VISIBLE);

        // Hide the clock
        mWrapperView.findViewById(R.id.lock_screen_clock).setVisibility(View.GONE);
        // Hide the date
        mWrapperView.findViewById(R.id.lock_screen_date).setVisibility(View.GONE);
        // Hide the other information
        mWrapperView.findViewById(R.id.lock_screen_info).setVisibility(View.GONE);

    }

    private void disableCallButtonsInView() {
        mWrapperView.findViewById(R.id.lock_screen_end_call_button).setVisibility(View.GONE);

        mWrapperView.findViewById(R.id.lock_screen_call_display).setVisibility(View.GONE);

        mWrapperView.findViewById(R.id.lock_screen_clock).setVisibility(View.VISIBLE);

        mWrapperView.findViewById(R.id.lock_screen_date).setVisibility(View.VISIBLE);

        mWrapperView.findViewById(R.id.lock_screen_info).setVisibility(View.VISIBLE);
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


    }

    /**
     * Simple method that handles logic when the correct passcode is entered
     */
    private void onCorrectPasscode() {
        Log.d(TAG, "Finishing activity, clearing service.");
        //unregisterReceiver(mReceiver);
        stopService(new Intent(this, PhoneStateService.class));
        finish();
    }

    private String getStoredPin(){
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        return sharedPref.getString( getString(R.string.lock_screen_passcode_value_key), null);

    }
    private void wrongPinEntered() {
        resetPinEntry();
        mNumTries++;
        Toast toast = Toast.makeText(
                getApplicationContext(),
                getString(R.string.toast_wrong_pin_entered),
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP,0,0);
        toast.show();
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    /**
     * Method that resets the Pin Entry
     */
    private void resetPinEntry() {
        mPinDisplayView.setText(getString(R.string.lock_screen_pin_default_display));
        mPinEntered = "";
        mDeleteButton.setVisibility(View.INVISIBLE);
    }

    private int getSpeedDialButtonPressed(int id, int defaultReturn){
        switch (id) {
            // TODO: set zero key as a lock screen dialer?
            case R.id.lock_screen_pin_button_1:
                return 1;
            case R.id.lock_screen_pin_button_2:
                return 2;
            case R.id.lock_screen_pin_button_3:
                return 3;
            case R.id.lock_screen_pin_button_4:
                return 4;
            case R.id.lock_screen_pin_button_5:
                return 5;
            case R.id.lock_screen_pin_button_6:
                return 6;
            case R.id.lock_screen_pin_button_7:
                return 7;
            case R.id.lock_screen_pin_button_8:
                return 8;
            case R.id.lock_screen_pin_button_9:
                return 9;
            default:
                return defaultReturn;
        }
    }

    private void enableLongPressFlag () { mLongPressFlag = true; }


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

        //unregister the receiver
        super.onDestroy();
    }



/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lock_screen, menu);
        return true;
    }
*/

/*    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/

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

            Intent intent = new Intent (Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + (telNum.trim())));
            startActivity(intent);

            enableCallButtonsInView(telNum, name, type);
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(350);
            resetPinEntry();
            enableLongPressFlag();

        }
    }
}


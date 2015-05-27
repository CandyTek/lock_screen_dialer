package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Vibrator;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class LockScreenKeypadPinActivity extends Activity
        implements View.OnClickListener, View.OnLongClickListener {

    private final static String TAG = "LSKeypadPinActivity";

    private TextView mPinDisplayView;
    private Button mDeleteButton;
    private Button mOkButton;
    private Button[] mkeypadButtons;
    private String mPinStored;
    private String mPinEntered;
    private int mNumTries;

    // Variables to implement TYPE_SYSTEM_ERROR stuff
    public WindowManager winManager;
    private RelativeLayout mWrapperView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mNumTries = savedInstanceState.getInt("numTries");
        }
        else {
            mNumTries = 0;
        }

        mkeypadButtons = new Button[10];

        // Implement some of the WindowManager TYPE_SYSTEM_ERROR hocus pocus
        WindowManager.LayoutParams localLayoutParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // To avoid notification bar
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // Same
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, //Same
                        PixelFormat.TRANSLUCENT);
        this.winManager = ((WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE));
        this.mWrapperView = new RelativeLayout(getBaseContext());
        getWindow().setAttributes(localLayoutParams);
        View.inflate(this, R.layout.activity_lock_screen_keypad_pin, this.mWrapperView);
        this.winManager.addView(this.mWrapperView, localLayoutParams);

        // Instantiate class variables for the interactive views
        mPinDisplayView = (TextView) mWrapperView.findViewById(R.id.lock_screen_pin_display);
        mPinDisplayView.setText(getString(R.string.lock_screen_pin_default_display)); // In case returning to this display from elsewhere, want to reset

        mDeleteButton = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_delete);
        mOkButton = (Button) mWrapperView.findViewById(R.id.lock_screen_pin_button_OK);

        mPinEntered = ""; // Same -- we want to reset

        mPinStored = getStoredPin();

        if (mPinStored == null) {
            Log.d(TAG, "Stored PIN is null.");
            onCorrectPasscode(); //For now, just exit.  TODO: find good way to handle these errors.
        }

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
            Log.d(TAG, "Attempting to access " + filename);
            if (sharedPref.getString(filename, null) != null) { //only set the long click where necessary
                Log.d(TAG, "Setting long click on key " + i);
                mkeypadButtons[i].setOnLongClickListener(this);
            }
        }
        mDeleteButton.setOnClickListener(this);
        mOkButton.setOnClickListener(this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("numTries", mNumTries);
    }

    public void onClick (View view) {
        switch (view.getId()) {
            case R.id.lock_screen_pin_button_0:
                mPinEntered += '0';
                break;
            case R.id.lock_screen_pin_button_1:
                mPinEntered += '1';
                break;
            case R.id.lock_screen_pin_button_2:
                mPinEntered += '2';
                break;
            case R.id.lock_screen_pin_button_3:
                mPinEntered += '3';
                break;
            case R.id.lock_screen_pin_button_4:
                mPinEntered += '4';
                break;
            case R.id.lock_screen_pin_button_5:
                mPinEntered += '5';
                break;
            case R.id.lock_screen_pin_button_6:
                mPinEntered += '6';
                break;
            case R.id.lock_screen_pin_button_7:
                mPinEntered += '7';
                break;
            case R.id.lock_screen_pin_button_8:
                mPinEntered += '8';
                break;
            case R.id.lock_screen_pin_button_9:
                mPinEntered += '9';
                break;
            case R.id.lock_screen_pin_button_OK:
                wrongPinEntered();  // Because our functionality will automatically accept a PIN when it is entered, we can assume error
                return;
            default:  // This should be the delete button
                Log.d(TAG, "delete button pressed.");
                resetPinEntry();
                return;
        }

        // Display a new "digit" on the text view
        if (mPinDisplayView.getText().toString().equals(getString(R.string.lock_screen_pin_default_display))){
            mPinDisplayView.setText("*");
            mDeleteButton.setVisibility(View.VISIBLE);
        } else {
            mPinDisplayView.setText(mPinDisplayView.getText().toString() + '*');
        }

        //  Now check whether the PIN entered so far matches the stored PIN
        if (mPinEntered.equals(mPinStored)) {
            onCorrectPasscode();
        }
    }

    public boolean onLongClick (View view){  // TODO: will need to set a longer longClick
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE
        );
        String telNum;
        String preferenceKeyPrefix = getString(R.string.key_number_store_prefix_phone);

        Intent intent = new Intent (Intent.ACTION_CALL);


        switch (view.getId()) {
            // TODO: set zero key as a lock screen dialer?
            case R.id.lock_screen_pin_button_1:
                telNum = sharedPref.getString(preferenceKeyPrefix+"1", null);
                break;
            case R.id.lock_screen_pin_button_2:
                telNum = sharedPref.getString(preferenceKeyPrefix+"2", null);
                break;
            case R.id.lock_screen_pin_button_3:
                telNum = sharedPref.getString(preferenceKeyPrefix+"3", null);
                break;
            case R.id.lock_screen_pin_button_4:
                telNum = sharedPref.getString(preferenceKeyPrefix+"4", null);
                break;
            case R.id.lock_screen_pin_button_5:
                telNum = sharedPref.getString(preferenceKeyPrefix+"5", null);
                break;
            case R.id.lock_screen_pin_button_6:
                telNum = sharedPref.getString(preferenceKeyPrefix+"6", null);
                break;
            case R.id.lock_screen_pin_button_7:
                telNum = sharedPref.getString(preferenceKeyPrefix+"7", null);
                break;
            case R.id.lock_screen_pin_button_8:
                telNum = sharedPref.getString(preferenceKeyPrefix+"8", null);
                break;
            case R.id.lock_screen_pin_button_9:
                telNum = sharedPref.getString(preferenceKeyPrefix+"9", null);
                break;
            default:  // This should be the delete button
                telNum = null;
                break;
        }

        if (telNum == null) {
            return false;
        }

        intent.setData(Uri.parse("tel:" + (telNum.trim())));
        startActivity(intent);

        return true;
    }

    public void onCorrectPasscode() {
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
}

package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class LockScreenKeypadPinFragment extends Fragment
    implements View.OnClickListener, View.OnLongClickListener {

    private static final String TAG = "PinFragment";

    private TextView mPinDisplayView;
    private Button mDeleteButton;
    private Button mOkButton;
    private Button[] mkeypadButtons;
    private String mPinStored;
    private String mPinEntered;
    private int mNumTries;

    private OnCorrectPasscode mListener;



    public LockScreenKeypadPinFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mNumTries = savedInstanceState.getInt("numTries");
        }
        else {
            mNumTries = 0;
        }

        mkeypadButtons = new Button[10];

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_lock_screen_keypad_pin, container, false);

        // grab the display that we will need to change on button press
        mPinDisplayView = (TextView) view.findViewById(R.id.lock_screen_pin_display);
        mPinDisplayView.setText(getString(R.string.lock_screen_pin_default_display)); // In case returning to this display from elsewhere, want to reset

        mDeleteButton = (Button) view.findViewById(R.id.lock_screen_pin_button_delete);
        mOkButton = (Button) view.findViewById(R.id.lock_screen_pin_button_OK);

        mPinEntered = ""; // Same -- we want to reset

        mPinStored = getStoredPin();

        if (mPinStored == null) {
            Log.d(TAG, "Stored PIN is null.");
            mListener.onCorrectPasscode(); //For now, just exit.  TODO: find good way to handle these errors.
        }

        mkeypadButtons[0] = (Button) view.findViewById(R.id.lock_screen_pin_button_0);
        mkeypadButtons[1] = (Button) view.findViewById(R.id.lock_screen_pin_button_1);
        mkeypadButtons[2] = (Button) view.findViewById(R.id.lock_screen_pin_button_2);
        mkeypadButtons[3] = (Button) view.findViewById(R.id.lock_screen_pin_button_3);
        mkeypadButtons[4] = (Button) view.findViewById(R.id.lock_screen_pin_button_4);
        mkeypadButtons[5] = (Button) view.findViewById(R.id.lock_screen_pin_button_5);
        mkeypadButtons[6] = (Button) view.findViewById(R.id.lock_screen_pin_button_6);
        mkeypadButtons[7] = (Button) view.findViewById(R.id.lock_screen_pin_button_7);
        mkeypadButtons[8] = (Button) view.findViewById(R.id.lock_screen_pin_button_8);
        mkeypadButtons[9] = (Button) view.findViewById(R.id.lock_screen_pin_button_9);

        SharedPreferences sharedPref = getActivity().getSharedPreferences(
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

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("numTries", mNumTries);
    }


    public void onClick (View view) {
        if (mListener == null) {
            Log.d(TAG, "mListener is null");
            return;
        }
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
            mListener.onCorrectPasscode();
        }
    }

    public boolean onLongClick (View view){  // TODO: will need to set a longer longClick
        SharedPreferences sharedPref = getActivity().getSharedPreferences(
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
        getActivity().startActivity(intent);

        return true;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCorrectPasscode) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnCorrectPasscode {
        // TODO: Update argument type and name
        public void onCorrectPasscode();
    }

    private String getStoredPin(){
        SharedPreferences sharedPref = getActivity().getSharedPreferences(
                getString(R.string.lock_screen_type_file_key),
                Context.MODE_PRIVATE);

        return sharedPref.getString( getString(R.string.lock_screen_passcode_value_key), null);

    }

    /**
     * Method for handling when an erroneous PIN has been entered
     */
    private void wrongPinEntered() {
        resetPinEntry();
        mNumTries++;

        Vibrator v = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
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
}

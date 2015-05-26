package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
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
    implements View.OnClickListener {

    private static final String TAG = "PinFragment";

    private TextView mPinDisplayView;
    private Button mDeleteButton;
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

        mPinEntered = ""; // Same -- we want to reset

        mPinStored = getStoredPin();

        if (mPinStored == null) {
            Log.d(TAG, "Stored PIN is null.");
            mListener.onCorrectPasscode(); //For now, just exit.  TODO: find good way to handle these errors.
        }


        // set the onClickListener -- must be programatically done because this is a fragment
        view.findViewById(R.id.lock_screen_pin_button_0).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_1).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_2).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_3).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_4).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_5).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_6).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_7).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_8).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_9).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_OK).setOnClickListener(this);
        view.findViewById(R.id.lock_screen_pin_button_delete).setOnClickListener(this);


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
                resetPinEntry();
                return;
        }

        // Display a new "digit" on the text view
        if (mPinDisplayView.getText().toString().equals(
                getString(R.string.lock_screen_pin_default_display))){
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
        Toast toast = Toast.makeText(
                getActivity().getApplicationContext(),
                getString(R.string.toast_wrong_pin_entered),
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP,0,0);
        toast.show();
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

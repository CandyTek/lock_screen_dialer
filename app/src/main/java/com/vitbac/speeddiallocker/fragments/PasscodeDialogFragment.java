package com.vitbac.speeddiallocker.fragments;

import android.app.DialogFragment;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;

import com.vitbac.speeddiallocker.R;
import com.vitbac.speeddiallocker.views.PasscodeEntryDisplay;
import com.vitbac.speeddiallocker.views.PasscodeEntryWidget;

/**
 * Created by nick on 8/10/15.
 */
public class PasscodeDialogFragment extends DialogFragment
        implements PasscodeEntryDisplay.OnDeletePressed, PasscodeEntryWidget.OnPasscodeEntryListener,
        PasscodeEntryWidget.OnInputReceivedListener, View.OnClickListener {
    private static final String TAG = "PasscodeDialogFrag";
    public static final int PASSCODE_PIN = 0, PASSCODE_PATTERN = 1;

    private PasscodeEntryWidget mPasscodeWidget;
    private PasscodeEntryDisplay mPasscodeDisplay;
    private String mPasscode;
    private String mKey;
    private int mPasscodeType;
    private OnAccessGranted mListener;


    public PasscodeDialogFragment() {}

    public static PasscodeDialogFragment newInstance(int type, String passcode, String key) {
        PasscodeDialogFragment frag = new PasscodeDialogFragment();
        Bundle bundle = new Bundle(3);
        if (type != PASSCODE_PIN && type != PASSCODE_PATTERN) {
            throw new IllegalArgumentException("Int parameter must be either 0 or 1; received " + type);
        }
        bundle.putInt("type", type);
        bundle.putString("passcode", passcode);
        bundle.putString("key", key);
        frag.setArguments(bundle);
        return frag;
    }

    public interface OnAccessGranted {
        void onAccessGranted(String key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        mPasscode = bundle.getString("passcode");
        mPasscodeType = bundle.getInt("type");
        mKey = bundle.getString("key");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_passcode_dialog, container, false);
        getDialog().setCanceledOnTouchOutside(false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        FrameLayout frame = (FrameLayout)rootView.findViewById(R.id.passcode_widget_container);
        View lockMechFrag;
        if (mPasscodeType == PASSCODE_PIN) {
            lockMechFrag = getActivity().getLayoutInflater()
                    .inflate(R.layout.fragment_passcode_dialog_pin, null);
        } else if (mPasscodeType == PASSCODE_PATTERN){
            lockMechFrag = getActivity().getLayoutInflater()
                    .inflate(R.layout.fragment_passcode_dialog_pattern, null);
        } else {
            throw new IllegalArgumentException("Passcode type is invalid.");
        }
        frame.addView(lockMechFrag);

        mPasscodeDisplay = (PasscodeEntryDisplay) rootView.findViewById(R.id.passcode_display);
        mPasscodeWidget = (PasscodeEntryWidget) rootView.findViewById(R.id.passcode_widget);
        View cancelButton = rootView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onResume(){
        super.onResume();
        Window window = getDialog().getWindow();
        //window.setLayout(pixelToDIP(250), pixelToDIP(400));
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mPasscodeWidget.setOnPassCodeEntryListener(this);
        mPasscodeWidget.setPasscode(mPasscode);


        if (mPasscodeType == PASSCODE_PIN) {
            //mPasscodeDisplay.displayDeleteButton(true);
            mPasscodeDisplay.setOnDeletePressedListener(this);
            mPasscodeWidget.setOnInputReceivedListener(this);

            /*mPasscodeDisplay.setInstructionText(
                    getActivity().getString(R.string.config_access_pin_default_display));*/
        } /*else if (mPasscodeType == PASSCODE_PATTERN) {
            mPasscodeDisplay.setInstructionText(
                    getActivity().getString(R.string.config_access_pattern_default_display));
        }*/
    }


    public void onDeletePressed(){
        mPasscodeWidget.backspace();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.cancel_button) {
            dismiss();
        }
    }

    public void onInputReceived(String input) {
        mPasscodeDisplay.setPasscodeText(input);
    }


    public void onPasscodeEntered(boolean isCorrect) {
        if (isCorrect) {
            dismiss();
            if (mListener != null){
                mListener.onAccessGranted(mKey);
            }
        } else if (mPasscodeDisplay.wrongEntry()) {
            // if true, a delay has been imposed, which here we will take to dismiss this dialog
            dismiss();
        } else {
            mPasscodeWidget.resetView();
        }
    }

    public void setOnAccessGrantedListener(OnAccessGranted listener) {
        mListener = listener;
    }

}

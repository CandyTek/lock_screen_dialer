package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;



public class ContactAssignedDialogFragment extends DialogFragment
    implements View.OnClickListener {

    private static final String TAG = "ContactAssignedDialFrag";
    private ReassignSpeedDialInterface mListener;

    private String mdisplayName;
    private String mthumbUri;
    private String mphoneNum;
    private String mphoneType;
    private String mkeyNumSelected;

    private TextView mdisplayNameView;
    private ImageView mthumbnailView;
    private TextView mphoneNumView;
    private TextView mphoneTypeView;
    private TextView mInstructionView;
    private Button mReassignButton;
    private Button mRemoveButton;
    /*private Button mCancelButton;*/


    public ContactAssignedDialogFragment() {
        // Required empty public constructor
    }

    public static ContactAssignedDialogFragment newInstance(String displayName, String thumbUri,
                                                            String phoneNum, String phoneType,
                                                            String keyNumSelected) {

        ContactAssignedDialogFragment fragment = new ContactAssignedDialogFragment();
        Bundle args = new Bundle();
        args.putString("displayName", displayName);
        args.putString("thumbUri", thumbUri);
        args.putString("phoneNum", phoneNum);
        args.putString("phoneType", phoneType);
        args.putString("keyNumSelected", keyNumSelected);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mdisplayName = bundle.getString("displayName");
        mthumbUri = bundle.getString("thumbUri");
        mphoneNum = bundle.getString("phoneNum");
        mphoneType = bundle.getString("phoneType");
        mkeyNumSelected = bundle.getString("keyNumSelected");
    }

    @Override
    public void onResume(){
        super.onResume();
        Window window = getDialog().getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contact_assigned_dialog, container, false);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        mdisplayNameView = (TextView) rootView.findViewById(R.id.contact_assigned_display_name);
        mdisplayNameView.setText(mdisplayName);
        mdisplayNameView.setSingleLine(false); // To make wrapping; can't seem to do this via XML

        mthumbnailView = (ImageView) rootView.findViewById(R.id.contact_assigned_thumbnail);
        if(mthumbUri != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mthumbnailView.setImageURI(Uri.parse(mthumbUri));
            }
            else {
                final Uri contactUri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI, mthumbUri);
                mthumbnailView.setImageURI(Uri.withAppendedPath(
                        contactUri,
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                ));
            }
        }
        else { // Set the default thumbnail image
            mthumbnailView.setImageResource(R.drawable.default_contact_image);
        }

        mphoneNumView = (TextView) rootView.findViewById(R.id.contact_assigned_phone_num);
        mphoneNumView.setText(mphoneNum);

        mphoneTypeView = (TextView) rootView.findViewById(R.id.contact_assigned_phone_type);
        mphoneTypeView.setText(mphoneType);

        /*mInstructionView = (TextView) rootView.findViewById(R.id.contact_assigned_instruction);
        mInstructionView.setText(getString(R.string.contact_assigned_instruction));*/
        TextView numView = (TextView) rootView.findViewById(R.id.contact_assigned_number);
        numView.setText(mkeyNumSelected);
        mReassignButton = (Button) rootView.findViewById(R.id.contact_assigned_reassign);
        mReassignButton.setOnClickListener(this);
        mRemoveButton = (Button) rootView.findViewById(R.id.contact_assigned_remove);
        mRemoveButton.setOnClickListener(this);
        /*mCancelButton = (Button) rootView.findViewById(R.id.contact_assigned_cancel);*/
        /*mCancelButton.setOnClickListener(this);*/


        // Inflate the layout for this fragment
        return rootView;
    }

    /**
     * Interface implementation of the onClick button for the buttons with active onClickListeners
     * as set in the onCreate method
     * @param v
     */
    public void onClick(View v) {
        mListener.reassignSpeedDial(v.getId(), mkeyNumSelected);
    }

    /*
    public void reassignButtonClicked(View view) {
        //mListener.ReassignSpeedDial(true); //User clicks "Reassign" speed dial
    }

    public void cancelButtonClicked(View view) {
        //mListener.ReassignSpeedDial(false); //User clicks "Cancel"
    }*/

   @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (ReassignSpeedDialInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ReassignSpeedDialInterface");
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
    public interface ReassignSpeedDialInterface {
        public void reassignSpeedDial(int viewId, String keyNumberSelected);
    }

}

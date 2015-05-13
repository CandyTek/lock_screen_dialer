package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


public class ContactDialogFragment extends DialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    private static final String TAG = "ContactDialogFragment"; //debugging tag
    private OnPhoneNumSelectionListener mListener;

    private String mContactLookupKey;
    private String mContactDisplayName;
    private String mContactThumbnailUriString;

    private ListView mPhoneNumsView;
    private TextView mContactNameView;
    private ImageView mThumbnailView;
    private View mRootView;

    private SimpleCursorAdapter mCursorAdapter;

    /**
     * Private variables to set up the database retrievals
     */

    private static final String[] PROJECTION =
            {
                    ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE,
                    ContactsContract.CommonDataKinds.Phone.LABEL
            };

    private static final String SELECTION =
            ContactsContract.Data.LOOKUP_KEY + " = ?" +
                    " AND " +
                    ContactsContract.Data.MIMETYPE + " = " +
                    "'" + ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

    private String[] mSelectionArgs = { "" };

    /**
     * Private variables to enable the CursorAdapter to set up the ListView
     */
    private static final String[] FROM_COLUMNS =
            {
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.TYPE
            };

    private static final int[] TO_IDS =
            {
                    R.id.phone_selector_list_item_number,
                    R.id.phone_selector_list_item_phone_type
            };


    public ContactDialogFragment() {
        // Required empty public constructor
    }

    public static ContactDialogFragment newInstance(String lookupKey,
                                                    String displayName,
                                                    String thumbnailUriString) {
        ContactDialogFragment returnFragment = new ContactDialogFragment();
        Bundle bundle = new Bundle(3);
        bundle.putString("lookupKey", lookupKey);
        bundle.putString("displayName", displayName);
        bundle.putString("thumbnailUriString", thumbnailUriString);
        returnFragment.setArguments(bundle);
        return returnFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        mContactLookupKey = bundle.getString("lookupKey");
        mContactDisplayName = bundle.getString("displayName");
        mContactThumbnailUriString = bundle.getString("thumbnailUriString");
    }

    @Override
    public void onResume(){
        super.onResume();
        Window window = getDialog().getWindow();
        //window.setLayout(pixelToDIP(250), pixelToDIP(400));
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, pixelToDIP(300));
        window.setGravity(Gravity.CENTER);

        //setStyle(STYLE_NO_TITLE, android.R.style.Theme_Holo_Light);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = inflater.inflate(R.layout.fragment_contact_dialog, container, false);
        getDialog().setCanceledOnTouchOutside(true);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        mContactNameView = (TextView) mRootView.findViewById(R.id.contact_selected_display_name);
        mThumbnailView = (ImageView) mRootView.findViewById(R.id.contact_selected_thumbnail);
        //mContactNameText.setText("Test 1234");
        return mRootView;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Gets the ListView from the parent activity (even though it was returned from here)
        mPhoneNumsView = (ListView) mRootView.findViewById(R.id.contact_selected_phone_num_list);

        //if (mPhoneNumsList == null) Log.d("ContactDialogFragment", "mPhone NumsList is null.");
        // Establish the CursorAdapter to get the phone numbers
        //Log.d(TAG, ""+Integer.valueOf(String.valueOf(R.id.contact_selected_phone_num_list), 16));
        //Log.d(TAG, ""+getActivity().findViewById(R.id.contact_selected_phone_num_list));
        //Log.d(TAG, ""+mRootView.findViewById(R.id.contact_selected_phone_num_list));

        // Establish the CursorAdapter to get the phone numbers
        mCursorAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.fragment_contact_dialog_item,
                null, // cursor not available yet
                FROM_COLUMNS,
                TO_IDS,
                0);

        // Set the adapter for the ListView
        mPhoneNumsView.setAdapter(mCursorAdapter);

        // Set the item click listener to be the current fragment
        mPhoneNumsView.setOnItemClickListener(this);

        // Initialize the loader
        getLoaderManager().initLoader(1, null, this);

        // Set the display name
        if (mContactDisplayName != null){
            mContactNameView.setText(mContactDisplayName);
            mContactNameView.setSingleLine(false);
        }
        else {
            mContactNameView.setText("Unknown");
        }

        // Set the contact image -- either based on the URI or to the default pic
        if (mContactThumbnailUriString != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mThumbnailView.setImageURI(Uri.parse(mContactThumbnailUriString));
            }
            else {
                final Uri contactUri = Uri.withAppendedPath(
                        ContactsContract.Contacts.CONTENT_URI, mContactThumbnailUriString);
                mThumbnailView.setImageURI(Uri.withAppendedPath(
                        contactUri,
                        ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                ));
            }
        }
        else {
            mThumbnailView.setImageResource(R.drawable.default_contact_image);
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onPhoneNumSelected(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPhoneNumSelectionListener) activity;
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
     * Implementation of the onCreateLoader() method.  Use "%" to represent a sequence of zero
     * or more characters; "_" to represent a single character.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {

        /*
        Uri baseUri;

        if (mSearchString != null && mSearchString.length() != 0) {
            Log.d("DB QUERY", "mSearchString length is not 0");
            //mSelectionArgs[0] = "'%" + mSearchString + "%'";
            baseUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_FILTER_URI, Uri.encode(mSearchString));

        }
        else {
            Log.d("DB QUERY", "mSearchString length is 0");
            //mSelectionArgs[0] = "'%a%'";
            baseUri = ContactsContract.Contacts.CONTENT_URI;
        }
        */
        mSelectionArgs[0]=mContactLookupKey;


        // Start the query
        return new CursorLoader(
                getActivity(),
                ContactsContract.Data.CONTENT_URI,
                //ContactsContract.Contacts.CONTENT_URI,
                PROJECTION,
                SELECTION,
                mSelectionArgs,
                ContactsContract.CommonDataKinds.Phone.TYPE
        );
    }

    /**
     * Implement onLoadFinished, which is loaded when the Contacts Provider returns the results
     * of a query.  In this method, put the result of Cursor in teh SimpleCursorAdapter to
     * automatically update the ListView with the search results
     */
    public void onLoadFinished (Loader<Cursor> loader, Cursor cursor) {
        // Put the result Cursor in the adapter for the ListView
        mCursorAdapter.swapCursor(cursor);
    }

    /**
     * This method is invoked when the loader framework detects that the result Cursor contains
     * stale data.  If you don't delete the SimpleCursorAdapter reference to the existing Cursor
     * the loader framework will not recycle the Cursor, which causes a memory leak.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Delete the reference to the existing Cursor
        mCursorAdapter.swapCursor(null);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

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
    public interface OnPhoneNumSelectionListener {
        // TODO: Update argument type and name
        public void onPhoneNumSelected(Uri uri);
    }

    private int pixelToDIP(int pixels){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, pixels,
                getResources().getDisplayMetrics());
    }
}

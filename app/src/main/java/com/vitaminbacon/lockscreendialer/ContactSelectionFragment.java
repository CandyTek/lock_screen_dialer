package com.vitaminbacon.lockscreendialer;

import android.annotation.SuppressLint;
import android.app.Activity;
//import android.app.LoaderManager;
//import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.provider.ContactsContract;


public class ContactSelectionFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    /**
     * Defines an array that contains column names to move from the Cursor to the ListView
     */
    @SuppressLint("InlinedApi")
    private final static String[] FROM_COLUMNS = {
            ContactsContract.Contacts.PHOTO_ID,  // Thumbnail-sized photo
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                    ContactsContract.Contacts.DISPLAY_NAME
    };

    /**
     * Defines an array that contains resource ids for the layout views
     * that get the Cursor column contents.  The id is pre-defined in the
     * Android framework, so it is prefaced with "android.R.id"
     */
    private final static int[] TO_IDS = { R.id.contact_selector_list_item_pic,
            R.id.contact_selector_list_item_name };  // REMOVED: android.R.id.text1 };
    /**
     * Defines a constant that contains the columns to be returned from a query
     */
    @SuppressLint("InlinedApi")
    private static final String[] PROJECTION = {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY :
                            ContactsContract.Contacts.DISPLAY_NAME
    };


    // The column index for the columns you substantiated in the PROJECTION
    private static final int CONTACT_ID_INDEX = 0;
    private static final int CONTACT_LOOKUP_KEY_INDEX = 1;
    private static final int CONTACT_DISPLAY_NAME_INDEX = 2;

    /**
     * To get the data we want.  Combination of text expressions and variables that tell the
     * provider the data columns to search and the values to find.
     */
    @SuppressLint("InlinedApi")
    private static final String SELECTION =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                    // Use DISPLAY_NAME_PRIMARY for Honeycomb + versions
                    "(("
                            + ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " NOTNULL) AND ("
                            + ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1) AND ("
                            + ContactsContract.Contacts.DISPLAY_NAME_PRIMARY + " != '' ))" :
                    // Use DISPLAY_NAME otherwise
                    "((" + ContactsContract.Contacts.DISPLAY_NAME + " NOTNULL) AND ("
                            + ContactsContract.Contacts.HAS_PHONE_NUMBER + "=1) AND ("
                            + ContactsContract.Contacts.DISPLAY_NAME + " != '' ))"; //" LIKE ?";

    // The search filter to weed through the contacts
    private String mSearchString;

    // Defines an array to hold values that replace the "?"
    private String[] mSelectionArgs = { mSearchString };



    // Define global mutable variables
    // Define a ListView object
    ListView mContactsList;

    // Define variables for the contact the user selects
    // The contact's _ID value
    long mContactId;

    // The contact's LOOKUP_KEY
    String mContactKey;

    // A content URI for the selected contact
    Uri mContactUri;

    // An adapter that binds the result Cursor to the ListView
    private SimpleCursorAdapter mCursorAdapter;

    private OnContactSelectedListener mListener;  // Requires implementation in parent activity

    // The view encapsulating the search text
    private EditText mEditText;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ContactSelectionFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContactSelectionFragment newInstance(String param1, String param2) {
        ContactSelectionFragment fragment = new ContactSelectionFragment();
        /*
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        */
        return fragment;
    }

    public ContactSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Fragment", "onCreate() entered.");
        /*
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /*
        TextView textView = new TextView(getActivity());
        textView.setText(R.string.hello_blank_fragment);
        return textView;
        */

        // Inflate the fragment layout
        Log.d("Fragment", "onCreateView executed");
        return inflater.inflate(R.layout.contact_selector_list_view, container, false);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(int phonenum) {
        if (mListener != null) {
            mListener.onContactSelected(phonenum);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnContactSelectedListener) activity;
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
    public interface OnContactSelectedListener {
        public void onContactSelected(int phonenum);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Gets the ListView from the parent activity (even though it was returned from here)
        mContactsList = (ListView) getActivity().findViewById(android.R.id.list);

        // Gets the EditText view
        mEditText = (EditText) getActivity().findViewById(R.id.contact_search_bar);
        // Set the text changing listener
        mEditText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after){

            }

            public void onTextChanged(CharSequence s, int start, int count, int after){
                mSearchString = s.toString();
                Log.d("Text Listener", mSearchString);
                // Now reload the query
                restartLoaderManager();  // Can't directly restart here, because "this" variable incorrect in this context
            }

        });

        // Get CursorAdapter
        mCursorAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.contact_selector_list_item,
                null, // cursor not available yet
                FROM_COLUMNS,
                TO_IDS,
                0);
        // Set the adapter for the ListView
        mContactsList.setAdapter(mCursorAdapter);
        // Set the item click listener to be the current fragment
        mContactsList.setOnItemClickListener(this);
        // Initialize the loader
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public void onItemClick (AdapterView<?> parent, View item, int position, long rowID) {
        Log.d("listener", "onItemClick() reached.");
        // Get the Cursor
        Cursor cursor = ((SimpleCursorAdapter) parent.getAdapter()).getCursor();
        // Move to teh selected contact
        cursor.moveToPosition(position);
        // Get the _ID value
        mContactId = cursor.getLong(CONTACT_ID_INDEX);
        // Get the selected LOOKUP_KEY
        mContactKey = cursor.getString(CONTACT_LOOKUP_KEY_INDEX);
        // Create teh contact's content Uri
        mContactUri = ContactsContract.Contacts.getLookupUri(mContactId, mContactKey);

    }

    /**
     * Implementation of the onCreateLoader() method.  Use "%" to represent a sequence of zero
     * or more characters; "_" to represent a single character.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle args) {

        //TODO: fix DB search so that it only searches field names, instead of any field in the contact
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



        // Start the query
        return new CursorLoader(
                getActivity(),
                baseUri,
                //ContactsContract.Contacts.CONTENT_URI,
                PROJECTION,
                SELECTION,
                null,
                //mSelectionArgs,
                null
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

    /**
     * Private simple class to handle text change listener
     */
    private void restartLoaderManager() {
        getLoaderManager().restartLoader(0, null, this);
    }


}

package com.vitaminbacon.lockscreendialer;

import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;


public class ContactSelectionActivity extends ActionBarActivity
        implements ContactSelectionFragment.OnContactSelectedListener,
        ContactDialogFragment.OnPhoneNumSelectionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_selection);

        /*
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contact_list_container, new PlaceholderFragment())
                    .commit();
        }
        */

        // Check that the activity is using the layout version with
        // the proper fragment container
        if (findViewById(R.id.contact_list_container) != null) {

            // Check if being restored from a previous inactive state
            if (savedInstanceState != null) {
                return;
            }

            // Create a new Fragment to be placed in the activity layout
            ContactSelectionFragment fragment = new ContactSelectionFragment();
            fragment.setArguments(getIntent().getExtras()); // In case activity started with special instructions

            // Add the fragment to the fragment container
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.contact_list_container, fragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_contact_selection, menu);
        return true;
    }

    @Override
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
    }

    /**
     * Implementing the fragment interface
     */
    public void onContactSelected(String lookupKey){

        ContactDialogFragment dialog = new ContactDialogFragment();
        dialog.show( getSupportFragmentManager(), "fragment_contact_dialog" );
    }

    public void onContactSelected(String lookupKey, String displayName, String thumbnailUriString) {

        // Instantiate a ContactDialogFragment with variables that can be restored onResume()!
        ContactDialogFragment dialog = ContactDialogFragment.
                newInstance(lookupKey, displayName, thumbnailUriString);
        dialog.show(getSupportFragmentManager(), "fragment_contact_dialog");


    }
    /**
     * Implementing the contact dialog fragment interface
     */
    public void onPhoneNumSelected(Uri uri){

    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_contact_selection, container, false);
            return rootView;
        }
    }
}

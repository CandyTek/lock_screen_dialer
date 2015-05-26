package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.Toast;


public class ContactSelectionActivity extends ActionBarActivity
        implements ContactSelectionFragment.OnContactSelectedListener,
        ContactDialogFragment.OnPhoneNumSelectionListener,
        ContactAssignedDialogFragment.ReassignSpeedDialInterface {

    private String mkeyNumberSelected;
    //private Boolean mkeyNumberAlreadyAssigned; -- phased out, check for this cleaner by checking if data exists for assignment
    private static final String TAG = "ContactsSelectionAct";

    private ContactAssignedDialogFragment mAssignedDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mkeyNumberSelected = getIntent().
                getStringExtra(getResources().getString(R.string.key_number_to_assign));

        /*mkeyNumberAlreadyAssigned = getIntent().getBooleanExtra(
                getResources().getString(R.string.is_key_number_already_assigned),
                false);*/

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

            //TODO: Would it be better UI experience to have the below dialog appear in SpeedDialSelectionActivity?

            // Check to see whether we need to open up the assigned contact dialog to reassign or clear the contact
            SharedPreferences sharedPref = this.getSharedPreferences(
                    getString(R.string.speed_dial_preference_file_key),
                    Context.MODE_PRIVATE);

            if (sharedPref.getString( // Checks that value exists for the key number selected
                    getString(R.string.key_number_store_prefix_phone) + mkeyNumberSelected,
                    null) != null) {
                // get the stored values

                String phoneNum = sharedPref.getString(
                        getString(R.string.key_number_store_prefix_phone) + mkeyNumberSelected,
                        null);
                String displayName = sharedPref.getString(
                        getString(R.string.key_number_store_prefix_name) + mkeyNumberSelected,
                        null);
                if (displayName ==null){
                    displayName = "Unknown";
                }
                String thumbUri = sharedPref.getString(
                        getString(R.string.key_number_store_prefix_thumb) + mkeyNumberSelected,
                        null);
                String phoneType = sharedPref.getString(
                        getString(R.string.key_number_store_prefix_type) + mkeyNumberSelected,
                        null);

                mAssignedDialogFragment =
                        ContactAssignedDialogFragment.newInstance(displayName,
                                thumbUri,
                                phoneNum,
                                phoneType,
                                mkeyNumberSelected);

                mAssignedDialogFragment.show(getSupportFragmentManager(), "fragment_contact_dialog");
            }
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
    public void onPhoneNumSelected(String displayName, String thumbUri,
                                   String phoneNum, String phoneType){
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE
        );

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(
                getString(R.string.key_number_store_prefix_name) + mkeyNumberSelected,
                displayName);
        editor.putString(
                getString(R.string.key_number_store_prefix_thumb) + mkeyNumberSelected,
                thumbUri);
        editor.putString(
                getString(R.string.key_number_store_prefix_phone) + mkeyNumberSelected,
                phoneNum);
        editor.putString(
                getString(R.string.key_number_store_prefix_type) + mkeyNumberSelected,
                phoneType);
        editor.commit();

        // Now return to the main SettingsActivity, and dump this activity using flags.
/*        Intent intent = new Intent(this, SettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);*/
        finish();
        Toast.makeText(getApplicationContext(),
                displayName + " "
                        + getString(R.string.toast_contact_assigned)
                        + mkeyNumberSelected,
                Toast.LENGTH_SHORT
        ).show();

    }


/*    public void reassignButtonClicked(View view) {
        reassignSpeedDial(true); //User clicks "Reassign" speed dial
    }

    public void cancelButtonClicked(View view) {
        reassignSpeedDial(false); //User clicks "Cancel"
    }*/

    public void reassignSpeedDial (int viewId) {

        // Regardless, dismiss the dialog fragment
        if (mAssignedDialogFragment != null) {
            mAssignedDialogFragment.dismiss();
        }

        switch (viewId) {

            case R.id.contact_assigned_reassign: // Reassign button pressed
                deleteContactFromSpeedDial();
                Toast.makeText(getApplicationContext(),
                        getString(R.string.toast_contact_reassign_cleared) + mkeyNumberSelected,
                        Toast.LENGTH_SHORT
                ).show();
                break;

            case R.id.contact_assigned_cancel: // Cancel "x" pressed
                Toast.makeText(getApplicationContext(),
                        getString(R.string.toast_contact_reassign_cancelled),
                        Toast.LENGTH_SHORT
                ).show();
                finish();
                break;

            case R.id.contact_assigned_remove: // Remove button pressed
                deleteContactFromSpeedDial();
                Toast.makeText(getApplicationContext(),
                        getString(R.string.toast_contact_reassign_cleared) + mkeyNumberSelected,
                        Toast.LENGTH_SHORT
                ).show();
                finish();
                break;
            default:
                break;
        }
    }

    /**
     * Method that deletes the stored information for the key specified by mkeyNumberSelected
     */
    public void deleteContactFromSpeedDial(){
        //  Delete the stored contact information
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.remove(getString(R.string.key_number_store_prefix_name) + mkeyNumberSelected);
        editor.remove(getString(R.string.key_number_store_prefix_phone) + mkeyNumberSelected);
        editor.remove(getString(R.string.key_number_store_prefix_thumb) + mkeyNumberSelected);
        editor.remove(getString(R.string.key_number_store_prefix_type) + mkeyNumberSelected);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){ //.apply() is faster, but only supported on API 9
            editor.apply();
        }
        else {
            editor.commit();
        }
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

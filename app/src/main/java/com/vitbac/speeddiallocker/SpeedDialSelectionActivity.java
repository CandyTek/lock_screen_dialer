package com.vitbac.speeddiallocker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.vitbac.speeddiallocker.fragments.ContactAssignedDialogFragment;

import java.util.Arrays;


public class SpeedDialSelectionActivity extends FragmentActivity
        implements ContactAssignedDialogFragment.ReassignSpeedDialInterface {

    private static final String TAG = "SpeedDialSelAct";
    private static final int SELECTED_COLOR_ID = R.color.platinum;
    private Button[] mKeypadButtons;
    private Boolean[] mKeypadAssignedTracker;
    private ContactAssignedDialogFragment mAssignedDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // inflate the view with the XML file
        setContentView(R.layout.activity_speed_dial_selection);
    }

    /**
     * This needs to be in the onResume method, since the shaded keys will be updated
     * when/if this activity comes back to the top of the stack after a contact is seleceted.
     * onCreate is only called on initial startup and if the app process was killed for want
     * of more memory
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Load the preference file
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE
        );

        // Grab the buttons
        mKeypadButtons = new Button[9];
        mKeypadAssignedTracker = new Boolean[9];  //TODO: can't I just get rid of this tracker now?
        Arrays.fill(mKeypadAssignedTracker, false); // initialize the tracker array as false
        mKeypadButtons[0] = (Button) this.findViewById(R.id.speed_dial_selection_1);
        mKeypadButtons[1] = (Button) this.findViewById(R.id.speed_dial_selection_2);
        mKeypadButtons[2] = (Button) this.findViewById(R.id.speed_dial_selection_3);
        mKeypadButtons[3] = (Button) this.findViewById(R.id.speed_dial_selection_4);
        mKeypadButtons[4] = (Button) this.findViewById(R.id.speed_dial_selection_5);
        mKeypadButtons[5] = (Button) this.findViewById(R.id.speed_dial_selection_6);
        mKeypadButtons[6] = (Button) this.findViewById(R.id.speed_dial_selection_7);
        mKeypadButtons[7] = (Button) this.findViewById(R.id.speed_dial_selection_8);
        mKeypadButtons[8] = (Button) this.findViewById(R.id.speed_dial_selection_9);

        // Change the color of the buttons where they have been assigned
        for (int i=0; i < 9; i++) {
            int keyNum = i+1;
            String filename = getString(R.string.key_number_store_prefix_phone) + keyNum;
            Log.d(TAG, "Attempting to access " + filename);
            if (sharedPref.getString(filename, null) != null) {
                Log.d(TAG, "Setting shaded background on key " + keyNum);
                //mKeypadButtons[i].setBackgroundColor(getResources().getColor(SELECTED_COLOR_ID));
                mKeypadButtons[i].setBackgroundResource(R.drawable.selector_speed_dial_highlighted);
                /*mKeypadButtons[i].setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.selector_speed_dial_highlighted));
                mKeypadButtons[i].invalidate();*/

                /*View buttonBackground;
                switch (keyNum) {
                    case 1:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_1);
                        break;
                    case 2:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_2);
                        break;
                    case 3:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_3);
                        break;
                    case 4:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_4);
                        break;
                    case 5:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_5);
                        break;
                    case 6:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_6);
                        break;
                    case 7:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_7);
                        break;
                    case 8:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_8);
                        break;
                    case 9:
                        buttonBackground = findViewById(R.id.speed_dial_selection_bkgrd_9);
                        break;
                    default:
                        buttonBackground = null;
                }
                if (buttonBackground != null) {
                    buttonBackground.setBackgroundColor(getResources().getColor(SELECTED_COLOR_ID));
                    buttonBackground.invalidate();
                } else {
                    Log.e(TAG, "Could not obtain button background view");
                }*/
                mKeypadAssignedTracker[i] = true;
            } else {
                Log.d(TAG, "Setting standard selector background on key = " + keyNum);
                mKeypadButtons[i].setBackgroundResource(R.drawable.selector_speed_dial);
            }
            /*else {
                mKeypadButtons[i].setBackgroundColor(TRANSPARENT_COLOR);
            }*/
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_speed_dial_selection, menu);
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
     * Method to be called when user clicks a button to assign a contact to a particular
     * keypad number.
     */
    public void assignSpeedDialContact (View view) {

        String keyNumPressed;
        switch (view.getId()) {
            case R.id.speed_dial_selection_1:
                keyNumPressed = "1";
                break;
            case R.id.speed_dial_selection_2:
                keyNumPressed = "2";
                break;
            case R.id.speed_dial_selection_3:
                keyNumPressed = "3";
                break;
            case R.id.speed_dial_selection_4:
                keyNumPressed = "4";
                break;
            case R.id.speed_dial_selection_5:
                keyNumPressed = "5";
                break;
            case R.id.speed_dial_selection_6:
                keyNumPressed = "6";
                break;
            case R.id.speed_dial_selection_7:
                keyNumPressed = "7";
                break;
            case R.id.speed_dial_selection_8:
                keyNumPressed = "8";
                break;
            case R.id.speed_dial_selection_9:
                keyNumPressed = "9";
                break;
            default:
                Log.e(TAG, "Invalid view sent to assignSpeedDialContact");
                return;
        }

        // Prepare an intent for the new activity that will allow the user to select
        /*Intent intent = new Intent(this, ContactSelectionActivity.class);
        intent.putExtra(getString(R.string.key_number_to_assign), keyNumPressed);
        startActivity(intent);*/

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);

        if (sharedPref.getString( // Checks that value exists for the key number selected
                getString(R.string.key_number_store_prefix_phone) + keyNumPressed,
                null) != null) {
            // get the stored values

            String phoneNum = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_phone) + keyNumPressed,
                    null);
            String displayName = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_name) + keyNumPressed,
                    null);
            if (displayName == null) {
                displayName = "Unknown";
            }
            String thumbUri = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_thumb) + keyNumPressed,
                    null);
            String phoneType = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_type) + keyNumPressed,
                    null);

            mAssignedDialogFragment =
                    ContactAssignedDialogFragment.newInstance(displayName,
                            thumbUri,
                            phoneNum,
                            phoneType,
                            keyNumPressed);
            mAssignedDialogFragment.show(getSupportFragmentManager(), "fragment_contact_dialog");
        } else {
            // Prepare an intent for the new activity that will allow the user to select a contact
            Intent intent = new Intent(this, ContactSelectionActivity.class);
            intent.putExtra(getString(R.string.key_number_to_assign), keyNumPressed);
            startActivity(intent);
        }
    }

    /**
     * Interface method for ContactAssignedDialogFragment
     *
     * @param viewId
     */
    public void reassignSpeedDial(int viewId, String keyNumberSelected) {

        switch (viewId) {

            case R.id.contact_assigned_reassign: // Reassign button pressed
                deleteContactFromSpeedDial(keyNumberSelected);
                showCustomToast(
                        getString(R.string.toast_contact_reassign_cleared) + keyNumberSelected);
                Intent intent = new Intent(this, ContactSelectionActivity.class);
                intent.putExtra(getString(R.string.key_number_to_assign), keyNumberSelected);
                startActivity(intent);
                break;

            /*case R.id.contact_assigned_cancel: // Cancel "x" pressed
                showCustomToast(getString(R.string.toast_contact_reassign_cancelled));
                break;*/

            case R.id.contact_assigned_remove: // Remove button pressed
                deleteContactFromSpeedDial(keyNumberSelected);
                showCustomToast(
                        getString(R.string.toast_contact_reassign_cleared) + keyNumberSelected);
                break;
            default:
                return;
        }

        // Regardless of what was pressed, dismiss the dialog fragment
        try {
            mAssignedDialogFragment.dismiss();
        } catch (NullPointerException e) {
            Log.e(TAG, "Unable to dismiss dialog because dialog fragment null", e);
        }
    }

    /**
     * Method that deletes the stored information for the key specified
     */
    public void deleteContactFromSpeedDial(String keyNumberSelected) {
        //  Delete the stored contact information
        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        editor.remove(getString(R.string.key_number_store_prefix_name) + keyNumberSelected);
        editor.remove(getString(R.string.key_number_store_prefix_phone) + keyNumberSelected);
        editor.remove(getString(R.string.key_number_store_prefix_thumb) + keyNumberSelected);
        editor.remove(getString(R.string.key_number_store_prefix_type) + keyNumberSelected);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) { //.apply() is faster, but only supported on API 9
            editor.apply();
        } else {
            editor.commit();
        }

        // Now correct the selector
        int num = Integer.parseInt(keyNumberSelected);

        Button b;
        switch (num) {
            case 1:
                b = (Button) findViewById(R.id.speed_dial_selection_1);
                break;
            case 2:
                b = (Button) findViewById(R.id.speed_dial_selection_2);
                break;
            case 3:
                b = (Button) findViewById(R.id.speed_dial_selection_3);
                break;
            case 4:
                b = (Button) findViewById(R.id.speed_dial_selection_4);
                break;
            case 5:
                b = (Button) findViewById(R.id.speed_dial_selection_5);
                break;
            case 6:
                b = (Button) findViewById(R.id.speed_dial_selection_6);
                break;
            case 7:
                b = (Button) findViewById(R.id.speed_dial_selection_7);
                break;
            case 8:
                b = (Button) findViewById(R.id.speed_dial_selection_8);
                break;
            case 9:
                b = (Button) findViewById(R.id.speed_dial_selection_9);
                break;
            default:
                Log.e(TAG, "Unable to unhighlight button because key number provided was " + num);
                return;
        }
        b.setBackgroundResource(R.drawable.selector_speed_dial);
    }

    private void showCustomToast(String text) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(
                R.layout.toast_custom,
                (ViewGroup) findViewById(R.id.toast_custom));
        TextView textView = (TextView) layout.findViewById(R.id.toast_text);
        textView.setText(text);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}

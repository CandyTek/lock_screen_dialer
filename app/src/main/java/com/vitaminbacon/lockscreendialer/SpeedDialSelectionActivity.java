package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;


public class SpeedDialSelectionActivity extends ActionBarActivity {

    private static final String TAG = "SpeedDialSelAct";
    private static final int SELECTED_COLOR_ID = R.color.platinum;
    private Button[] mkeypadButtons;
    private Boolean[] mkeypadAssignedTracker;

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
        mkeypadButtons = new Button[9];
        mkeypadAssignedTracker = new Boolean[9];  //TODO: can't I just get rid of this tracker now?
        Arrays.fill(mkeypadAssignedTracker, false); // initialize the tracker array as false
        mkeypadButtons[0] = (Button)this.findViewById(R.id.speed_dial_selection_1);
        mkeypadButtons[1] = (Button)this.findViewById(R.id.speed_dial_selection_2);
        mkeypadButtons[2] = (Button)this.findViewById(R.id.speed_dial_selection_3);
        mkeypadButtons[3] = (Button)this.findViewById(R.id.speed_dial_selection_4);
        mkeypadButtons[4] = (Button)this.findViewById(R.id.speed_dial_selection_5);
        mkeypadButtons[5] = (Button)this.findViewById(R.id.speed_dial_selection_6);
        mkeypadButtons[6] = (Button)this.findViewById(R.id.speed_dial_selection_7);
        mkeypadButtons[7] = (Button)this.findViewById(R.id.speed_dial_selection_8);
        mkeypadButtons[8] = (Button)this.findViewById(R.id.speed_dial_selection_9);

        // Change the color of the buttons where they have been assigned
        for (int i=0; i < 9; i++) {
            int keyNum = i+1;
            String filename = getString(R.string.key_number_store_prefix_phone) + keyNum;
            Log.d(TAG, "Attempting to access " + filename);
            if (sharedPref.getString(filename, null) != null) {
                Log.d(TAG, "Setting shaded background on key " + keyNum);
                //mkeypadButtons[i].setBackgroundColor(getResources().getColor(SELECTED_COLOR_ID));
                mkeypadButtons[i].setBackgroundResource(R.drawable.selector_speed_dial_highlighted);
                /*mkeypadButtons[i].setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.selector_speed_dial_highlighted));
                mkeypadButtons[i].invalidate();*/

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
                mkeypadAssignedTracker[i] = true;
            } else {
                Log.d(TAG, "Setting standard selector background on key = " + keyNum);
                mkeypadButtons[i].setBackgroundResource(R.drawable.selector_speed_dial);
            }
            /*else {
                mkeypadButtons[i].setBackgroundColor(TRANSPARENT_COLOR);
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
        Intent intent = new Intent(this, ContactSelectionActivity.class);
        intent.putExtra(getString(R.string.key_number_to_assign), keyNumPressed);
        startActivity(intent);
    }

}

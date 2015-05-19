package com.vitaminbacon.lockscreendialer;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;


public class SpeedDialSelectionActivity extends ActionBarActivity {

    private static final String TAG = "SpeedDialSelAct";
    private Button[] mkeypadButtons;
    private Boolean[] mkeypadAssignedTracker;
    private static final int SELECTED_COLOR_ID = R.color.platinum;


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
        mkeypadAssignedTracker = new Boolean[9];
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
                mkeypadButtons[i].setBackgroundColor(getResources().getColor(SELECTED_COLOR_ID));
                mkeypadAssignedTracker[i] = true;
            }
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

        Button button = (Button)view;

        // Prepare an intent for the new activity that will allow the user to select
        Intent intent = new Intent(this, ContactSelectionActivity.class);
        String keyNumPressed = button.getText().toString();

        // The next activity will need to know which number was pressed!
        intent.putExtra(getString(R.string.key_number_to_assign), keyNumPressed);

        // The next activity will want to know whether this key number has already been assigned
/*        intent.putExtra(
                getString(R.string.is_key_number_already_assigned),
                mkeypadAssignedTracker[Integer.parseInt(keyNumPressed)]
        );*/

        startActivity(intent);
    }

}

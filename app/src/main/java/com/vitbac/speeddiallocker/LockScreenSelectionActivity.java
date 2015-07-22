package com.vitbac.speeddiallocker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class LockScreenSelectionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen_selection);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_lock_screen_selection, menu);
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

    public void onLockScreenSelected(View view) {

        Intent intent;
        switch (view.getId()) {
            case R.id.lock_screen_selection_keypad_pattern:
                intent = new Intent(this, KeypadPatternConfigActivity.class);
                break;
            case R.id.lock_screen_selection_keypad_pin:
                intent = new Intent(this, KeypadPinConfigActivity.class);
                break;
            default:
                return;
        }

        startActivity(intent);
    }
}

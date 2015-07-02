package com.vitaminbacon.lockscreendialer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


public class KeypadPinConfigActivity extends Activity {

    private static final String TAG = "KeypadPinConfig";
    private TextView mKeyPadEntryInstructions;
    private EditText mEditText;
    private Button mSubmitButton;
    private String mPinEntered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keypad_pin_config);

    }

    @Override
    protected void onResume() {
        super.onResume();

        mKeyPadEntryInstructions = (TextView) this.findViewById(R.id.keypad_pin_config_instruction);
        mEditText = (EditText) this.findViewById(R.id.keypad_pin_config_edittext);
        mEditText.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                if(mPinEntered == null || !mPinEntered.equals("")){
                    return;
                }
                if (mEditText.getText().toString().length() == 0){
                    mKeyPadEntryInstructions.setText(getString(
                            R.string.keypad_pin_config_instructions_1));
                }
                else if (mEditText.getText().toString().length() < 4 ){
                    mKeyPadEntryInstructions.setText(getString(
                            R.string.keypad_pin_config_instructions_2));
                }
                else{
                    mKeyPadEntryInstructions.setText(getString(
                            R.string.keypad_pin_config_instructions_3));
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
        });
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                /*if (actionId == EditorInfo.IME_ACTION_DONE) {
                    return true;
                }
                return false;*/
                return (actionId == EditorInfo.IME_ACTION_DONE);
            }
        });
        mSubmitButton = (Button) this.findViewById(R.id.btn_keypad_pin_config_submit);

        setActivityToFirstState();  // calls method to ensure that entry onResume is always in first state
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_keypad_pin_config, menu);
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
     * Methods that set the state of the activity
     */
    private void setActivityToFirstState(){
        mKeyPadEntryInstructions.setText(getString(R.string.keypad_pin_config_instructions_1));
        mEditText.setText("");
        mSubmitButton.setText(getString(R.string.btn_keypad_pin_config_submit_1));
        mPinEntered = "";  //Can test for this to see if Activity is in re-enter PIN state.
    }

    private void setActivityToSecondState(String pin){
        mKeyPadEntryInstructions.setText(getString(R.string.keypad_pin_config_instructions_4));
        mPinEntered = pin; // Must go before setText so that logic works correctly!
        mEditText.setText("");
        mSubmitButton.setText(getString(R.string.btn_keypad_pin_config_submit_2));
    }

    public void submitButtonClicked(View view) {
        String pin = mEditText.getText().toString();
        if (mPinEntered.equals("")) { //Activity is in first state
            if (pin.length() < 4) {
                makeToast(getString(R.string.err_keypad_pin_config_too_short));
                setActivityToFirstState();
                return;
            }
            if (pin.length() > 6) {  // Just a sys check to see if XML limit works
                Log.d(TAG, "PIN entered too long.");
                setActivityToFirstState();
                return;
            }
            setActivityToSecondState(pin);
        }
        else { // Activity is in second state
            if (!mPinEntered.equals(pin)) {
                makeToast(getString(R.string.err_keypad_pin_config_not_matching));
                setActivityToFirstState();
            }
            else {
                SharedPreferences sharedPref = this.getSharedPreferences(
                        getString(R.string.lock_screen_type_file_key),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor editor = sharedPref.edit();
                // Set the preferences to indicate Keypad PIN is the type of entry
                editor.putString(
                        getString(R.string.lock_screen_type_value_key),
                        getString(R.string.lock_screen_type_value_keypad_pin));
                // Store the PIN
                editor.putString(
                        getString(R.string.lock_screen_passcode_value_key),
                        mPinEntered);

                editor.commit();

                makeToast(getString(R.string.keypad_pin_config_success));

                // Finish up with result.
                Intent returnIntent = new Intent();
                setResult(RESULT_OK, returnIntent);
                finish();
                /*Intent intent = new Intent(this, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);*/
            }
        }

    }

    @Override
    public void onBackPressed() {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
        super.onBackPressed();
    }

    public void cancelButtonClicked(View view) {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    private void makeToast(String text) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(
                R.layout.toast_custom,
                (ViewGroup) findViewById(R.id.toast_custom));
        TextView textView = (TextView) layout.findViewById(R.id.toast_text);
        textView.setText(text);
        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

}

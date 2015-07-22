package com.vitbac.speeddiallocker;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;


public class ErrorPageActivity extends Activity {

    private WindowManager mWindowManager;
    private ViewGroup mWrapperView;
    private ViewGroup mLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_error_page);

        WindowManager.LayoutParams localLayoutParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // To avoid notification bar
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // Same
                                //WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                                WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER, //Same
                        PixelFormat.TRANSLUCENT);
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        getWindow().setAttributes(localLayoutParams);
        //View.inflate(this, R.layout.activity_lock_screen_keypad_pin, mWrapperView);
        mWrapperView = new FrameLayout(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayout = (RelativeLayout) inflater.inflate(R.layout.activity_error_page, null);

        /*mLayout = LayoutInflater
                .from(this)
                .inflate(R.layout.activity_error_page,
                        new RelativeLayout(getBaseContext()),
                        false);*/
        //mWrapperView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        mWindowManager.addView(mWrapperView, localLayoutParams);
        mWrapperView.addView(mLayout);
    }


    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_error_page, menu);
        return true;
    }*/

    /*@Override
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
    }*/
}

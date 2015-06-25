package com.vitaminbacon.lockscreendialer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.view.GestureDetectorCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.internal.telephony.ITelephony;
import com.vitaminbacon.lockscreendialer.exceptions.CallHandlerException;
import com.vitaminbacon.lockscreendialer.helpers.BitmapToViewHelper;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public abstract class LockScreenActivity extends Activity implements View.OnClickListener,
        View.OnTouchListener, CompoundButton.OnCheckedChangeListener,
        BitmapToViewHelper.GetBitmapFromTaskInterface {

    private final static String TAG = "LSActivity";
    // Timer to handle on long clicks using the ontouchlistener -- this will presumably be used by all instances
    protected Handler mHandler;
    protected DialerRunnable mRunnable;
    protected boolean mLongPressFlag;
    // Variables to implement TYPE_SYSTEM_ERROR stuff
    private WindowManager mWindowManager;
    private RelativeLayout mWrapperView;
    //private Bitmap mBackgroundBitmap;
    private ImageView mBackgroundView;
    private ProgressBar mBackgroundProgress;
    private View mScreenText;  // So far, entirely for animation sequence
    // Variables to utilize phone state service and handle phone calls
    private boolean mPhoneCallActiveFlag;
    private String mPhoneNumOnCall;
    private String mPhoneTypeOnCall;
    private String mContactNameOnCall;
    private boolean mBackgroundSetFlag;

    private GestureDetectorCompat mDetector;
    private float mLastMoveCoord;
    private boolean mFlinged;
    private boolean mSheathScreenOn;
    //protected int mLayoutId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);

        // Check phone call status first, and handle appropriately
        int phoneState = getIntent().getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0);
        if (phoneState == PhoneStateReceiver.PHONE_STATE_IDLE) {
            //mPhoneCallActiveFlag = false;
            Log.d(TAG, "onCreate received intent with phone state idle; starting screen service and exiting");
            startService(new Intent(this, LockScreenService.class)); // Means lock screen was unlocked, but phone call ended, so resume screen service
        } else if (phoneState == PhoneStateReceiver.PHONE_STATE_RINGING) {
            //mPhoneCallActiveFlag = true;
            Log.d(TAG, "onCreate received intent with phone state ringing; stopping screen service and exiting");
            stopService(new Intent(this, LockScreenService.class));  // Means lock screen was unlocked, but phone call was received
            finish();
            return;
        } else if (phoneState == 0) { // Activity initialized not by the phone state receiver
            // begin the phone state service to listen to phone call information; supposedly only one service of a kind can exist
            startService(new Intent(this, PhoneStateService.class));
        }
        mPhoneCallActiveFlag = false;

        // Implement some of the WindowManager TYPE_SYSTEM_ERROR hocus pocus
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        WindowManager.LayoutParams localLayoutParams =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // To avoid notification bar
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // Same
                                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, //Same
                        PixelFormat.TRANSLUCENT);
        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        getWindow().setAttributes(localLayoutParams);
        //View.inflate(this, R.layout.activity_lock_screen_keypad_pin, mWrapperView);
        mWrapperView = (RelativeLayout) LayoutInflater
                .from(this)
                .inflate(R.layout.activity_lock_screen,  // obtained from subclass onCreate()
                        new RelativeLayout(getBaseContext()),
                        false);
        mWindowManager.addView(mWrapperView, localLayoutParams);

        // Check that the layout has the requisite phone-related elements for this activity to function
        if (mWrapperView.findViewById(R.id.lock_screen_end_call_button) == null ||
                mWrapperView.findViewById(R.id.lock_screen_speaker_call_button) == null ||
                mWrapperView.findViewById(R.id.lock_screen_phone_buttons) == null ||
                // mWrapperView.findViewById(R.id.lock_screen_call_display) == null ||
                mWrapperView.findViewById(R.id.drawer_lock_screen_call_display) == null ||
                mWrapperView.findViewById(R.id.lock_screen_background_progress) == null ||
                mWrapperView.findViewById(R.id.lock_screen_background_view) == null ||
                mWrapperView.findViewById(R.id.lock_screen_fragment_container) == null) {
            Log.e(TAG, "Layout incompatible with this activity for failing to have proper Views.");
            onFatalError();
            return;
        }
        //  Check that the layout elements are of the right type
        try {
            ImageButton b = (ImageButton)mWrapperView.findViewById(R.id.lock_screen_end_call_button);
            RelativeLayout rl = (RelativeLayout) b.getParent(); // ensures the correct encapsulating layout is there
            // TextView v = (TextView)mWrapperView.findViewById(R.id.lock_screen_call_display);
            mBackgroundView = (ImageView)mWrapperView.findViewById(R.id.lock_screen_background_view);
            mBackgroundProgress = (ProgressBar)mWrapperView.findViewById(R.id.lock_screen_background_progress);
            mScreenText = mWrapperView.findViewById(R.id.lock_screen_interaction_container);
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout incompatible with this activity for failing to have proper Views.");
            onFatalError();
            return;
            /*throw new ClassCastException(this.toString()
                    + " must use appropriate XML layout with correct IDs and correct types.");*/
        }

        try {
            instantiateOptionalViewsInView();
        } catch (CallHandlerException e) {
            Log.e(TAG, "Layout renders activity unable to handle calls", e);
            onFatalError();
            return;
        }

        //Inflate the pin activity view fragment
        try {
            View pinActivityFragment = getLayoutInflater()
                    .inflate(getFragmentLayout(), null);
            FrameLayout container = (FrameLayout) mWrapperView
                    .findViewById(R.id.lock_screen_fragment_container);
            if (pinActivityFragment == null) {
                Log.e(TAG, "Null fragment provided by subclass.");
                onFatalError();
                return;
            }
            container.addView(pinActivityFragment);
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout does not have container into which to enter this lock screen's layout", e);
            onFatalError();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mSheathScreenOn = prefs.getBoolean(getString(R.string.key_toggle_sheath_screen), false);

        // Set up the animation for when the background is set and there is no SheathScreen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 && !mSheathScreenOn) {
            mScreenText.setTranslationX(getDisplayWidth());
        }

        mBackgroundView.setVisibility(View.GONE);
        setActivityBackground(mBackgroundView);
        mBackgroundSetFlag = false;
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
    }



    @Override
    protected void onResume(){
        // if there is still a phone call active, we need to set the display to handle that
        //Log.d(TAG, "onResume() called.");
        super.onResume();

        // Check the "sheath" screen status and enable/disable

        try {
            View sheathScreen = mWrapperView.findViewById(R.id.lock_screen_sheath_container);
            View interactionScreen = mWrapperView.findViewById(R.id.lock_screen_interaction_container);
            if (mSheathScreenOn) {
                Log.d(TAG, "Sheath screen active.");
                sheathScreen.setVisibility(View.VISIBLE);
                sheathScreen.setTranslationY(0);
                //interactionScreen.setVisibility(View.INVISIBLE);
                sheathScreen.setOnTouchListener(this);

                // Need to set up the proper translation position for interactionScreen
                interactionScreen.setVisibility(View.VISIBLE);
                interactionScreen.setTranslationY(interactionScreen.getHeight());
            } else {
                Log.d(TAG, "Sheath screen inactive");
                sheathScreen.setVisibility(View.INVISIBLE);
                interactionScreen.setVisibility(View.VISIBLE);
            }

        } catch (NullPointerException e) {
            Log.e(TAG, "Layout has improper views; unable to find sheath screen or interaction screen", e);
            onFatalError();
        }

        // For backwards compatibility, we need to manually set the "clock" text view
        // to the current time
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                TextView clock = (TextView) mWrapperView.findViewById(R.id.lock_screen_clock);
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm a", Locale.getDefault());
                clock.setText(sdf.format(cal.getTime()));
            } catch (ClassCastException e) {
                Log.e(TAG, "Layout has improper clock view type for older versions.", e);
                onFatalError();
                return;
            } catch (NullPointerException e) {
                Log.e(TAG, "Layout does not have clock view.", e);
                onFatalError();
                return;
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int receivedPhoneState = getIntent().getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0);

        // Only want to reset the old saved flag if this activity wasn't recalled by virtue of the phone being hung up.
        if (receivedPhoneState != PhoneStateReceiver.PHONE_STATE_IDLE) {
            mPhoneCallActiveFlag = savedInstanceState.getBoolean("phoneCallActiveFlag");
        }
        mContactNameOnCall = savedInstanceState.getString("contactNameOnCall");
        mPhoneNumOnCall = savedInstanceState.getString("phoneNumOnCall");
        mPhoneTypeOnCall = savedInstanceState.getString("phoneTypeOnCall");

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("phoneCallActiveFlag", mPhoneCallActiveFlag);
        if (mContactNameOnCall != null) {
            outState.putString("contactNameOnCall", mContactNameOnCall);
        }
        if (mPhoneNumOnCall != null) {
            outState.putString("phoneNumOnCall", mPhoneNumOnCall);
        }
        if (mPhoneTypeOnCall != null) {
            outState.putString("phoneTypeOnCall", mPhoneTypeOnCall);
        }
        //outState.putBoolean("backgroundSetFlag", mBackgroundSetFlag);
    }

    /**
     * Critical function that gathers data from the phone state receiver as to the status of
     * phone calls
     * @param intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        int phoneState = intent.getIntExtra(PhoneStateReceiver.EXTRA_PHONE_STATE, 0); // returns 0 if doesn't exist

        Log.d(TAG, "onNewIntent called, phoneState = " + phoneState);

        if (phoneState == PhoneStateReceiver.PHONE_STATE_IDLE) {
            // Phone was just hung up and activity is instantiated
            Log.d(TAG, "onNewIntent received intent with phone state idle; starting screen service");
            mPhoneCallActiveFlag = false;
            mContactNameOnCall = mPhoneNumOnCall = mPhoneTypeOnCall = null;
            disableCallViewsInView(true);
            try {
                enableOptionalViewsInView();
            } catch (CallHandlerException e) {
                Log.e(TAG, "Layout renders activity unable to handle calls", e);
                onFatalError();
            }

            startService(new Intent(this, LockScreenService.class)); // reenable the off-screen receiver
        }
        else if (phoneState == PhoneStateReceiver.PHONE_STATE_RINGING) { // a call has been received, we should handle lock screen in case user returns there
            Log.d(TAG, "onNewIntent received intent with phone state ringing; stopping screen service");
            // This implementation ends the lock screen, but it should be recalled by the receiver once the call is over
            stopService(new Intent(this, LockScreenService.class));  // don't want the lock screen to keep popping up during a phone call in this implementation
            finish();
        }
        else if (phoneState == PhoneStateReceiver.PHONE_STATE_OFFHOOK) {
            // Currently requires no implementation
            return;
        }
    }

    @Override
    public void onBackPressed() {  // Overrides the back button to prevent exit
        return;
    }

    // Implemented to use TYPE_SYSTEM_ERROR hack
    @Override
    public void onDestroy() {
        //Log.d(TAG, "onDestroy called");
        super.onDestroy();

        // TODO: is there a way to animate this?
        mWindowManager.removeView(mWrapperView);
        ((ImageView) mWrapperView.findViewById(R.id.lock_screen_background_view)).setImageBitmap(null);  // Probably not necessary
        mWrapperView.removeAllViews();

        /*if (mBackgroundBitmap != null) {
            mBackgroundBitmap.recycle();
        }*/

        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
        }
        mHandler = null;
        mRunnable = null;

    }


    /**
     * Simple functions that makes the call views visible/invisible and hides unnecessary views.
     * This is the implementation of this parent class that requires certain view IDs --
     * @param telNum - a string with the telephone number
     * @param name - a string to display the contact's name
     * @param type - a string to display the phone number type
     */
    private void enableCallViewsInView(String telNum, String name, String type, String thumbUri){
        Log.d(TAG, "enableCallViewsInView() called");
        //TextView dialInfoView;

        try {
            final ImageButton endCallBtn, spkrBtn;
            final ViewGroup phoneButtons, widgets;
            final TextView phoneCallName, phoneCallDescr, phoneCallNum;
            final ImageView phoneCallThumb;
            final View drawer;

            // Get the info views
            drawer = mWrapperView.findViewById(R.id.drawer_lock_screen_call_display);
            phoneCallThumb = (ImageView) mWrapperView.findViewById(R.id.drawer_phone_call_thumb);
            phoneCallName = (TextView) mWrapperView.findViewById(R.id.drawer_phone_call_name);
            phoneCallDescr = (TextView) mWrapperView.findViewById(R.id.drawer_phone_call_description);
            phoneCallNum = (TextView) mWrapperView.findViewById(R.id.drawer_phone_call_number);
            phoneCallName.setText(name);
            phoneCallDescr.setText("Calling at " + type + "...");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                phoneCallNum.setText(PhoneNumberUtils.formatNumber(telNum, tm.getSimCountryIso()));
            } else {
                phoneCallNum.setText(PhoneNumberUtils.formatNumber(telNum));
            }
            phoneCallNum.setText(telNum);

            if (thumbUri != null) { // block that sets the thumbnail, if it exists

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    phoneCallThumb.setImageURI(Uri.parse(thumbUri));
                }
                else {
                    final Uri contactUri = Uri.withAppendedPath(
                            ContactsContract.Contacts.CONTENT_URI, thumbUri);
                    phoneCallThumb.setImageURI(Uri.withAppendedPath(
                            contactUri,
                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                    ));
                }
                if (phoneCallThumb.getDrawable() == null) {
                    Log.d(TAG, "Thumb drawable null");
                }
            } else {
                phoneCallThumb.setImageResource(R.drawable.default_contact_image);
            }
            // Get the phone button views
            phoneButtons = (ViewGroup) mWrapperView.findViewById(R.id.lock_screen_phone_buttons);
            widgets = (ViewGroup) mWrapperView.findViewById(R.id.lock_screen_additional_widgets);
            endCallBtn = (ImageButton) mWrapperView.findViewById(R.id.lock_screen_end_call_button);
            spkrBtn = (ImageButton) mWrapperView.findViewById(R.id.lock_screen_speaker_call_button);

            /*dialInfoView = (TextView) mWrapperView.findViewById(R.id.lock_screen_call_display);
            dialInfoView.setText(getDialInfoViewText(telNum, name, type));
            dialInfoView.setVisibility(View.VISIBLE);*/

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                // First, animate the display drawer at the top
                final int dDistance = drawer.getHeight();
                drawer.setTranslationY(dDistance * -1);
                drawer.setVisibility(View.VISIBLE);
                final int bDistance = phoneButtons.getWidth();
                phoneButtons.setTranslationX(bDistance);
                phoneButtons.setVisibility(View.VISIBLE);
                endCallBtn.setVisibility(View.VISIBLE);
                spkrBtn.setVisibility(View.VISIBLE);
                ((ViewGroup) endCallBtn.getParent()).setVisibility(View.VISIBLE); // Must make parent explicitly visible now then set click listener

                // Now start the drawer animation
                drawer.animate()
                        .translationY(0)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                try {
                                    View infoBlock = mWrapperView
                                            .findViewById(R.id.lock_screen_info_block);
                                    infoBlock.setVisibility(View.INVISIBLE);
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                                    onFatalError();
                                }

                                // Now chain the animation
                                phoneButtons.animate().translationX(0).setListener(null);
                                widgets.animate().translationX(bDistance * -1);
                            }
                        });

                endCallBtn.setOnClickListener(this);
                spkrBtn.setOnClickListener(this);

            } else {
                drawer.setVisibility(View.VISIBLE);
                try {
                    View infoBlock = mWrapperView.findViewById(R.id.lock_screen_info_block);
                    infoBlock.setVisibility(View.INVISIBLE);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                    onFatalError();
                }

                phoneButtons.setVisibility(View.VISIBLE);
                endCallBtn.setVisibility(View.VISIBLE);
                spkrBtn.setVisibility(View.VISIBLE);
                ((ViewGroup) endCallBtn.getParent()).setVisibility(View.VISIBLE);
                widgets.setVisibility(View.INVISIBLE);
                endCallBtn.setOnClickListener(this);
                spkrBtn.setOnClickListener(this);
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;
        }
    }

    private void disableCallViewsInView(boolean animationFlag) {
        Log.d(TAG, "disableCallViewsInView() called");
        ImageButton endCallBtn;
        //TextView dialInfoView;
        final ViewGroup phoneButtons, widgets;  // Declared final for anonymous function purpose
        final View drawer;

        try {
            drawer = mWrapperView.findViewById(R.id.drawer_lock_screen_call_display);
            phoneButtons = (ViewGroup) mWrapperView.findViewById(R.id.lock_screen_phone_buttons);
            widgets = (ViewGroup) mWrapperView.findViewById(R.id.lock_screen_additional_widgets);
            //endCallBtn = (ImageButton) mWrapperView.findViewById(R.id.lock_screen_end_call_button);
            //dialInfoView = (TextView) mWrapperView.findViewById(R.id.lock_screen_call_display);
            //dialInfoView.setVisibility(View.GONE);

            if (animationFlag && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                // Position the widgets
                int bDistance = widgets.getWidth();
                widgets.setTranslationX(bDistance * -1);
                widgets.setVisibility(View.VISIBLE);

                // Start the animation
                widgets.animate().translationX(0);
                phoneButtons.animate()
                        .translationX(bDistance)
                        .setListener(new AnimatorListenerAdapter() {
                            @SuppressLint("NewApi")
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                int dDistance = drawer.getHeight();
                                drawer.animate()
                                        .translationY(dDistance * -1)
                                        .setListener(new AnimatorListenerAdapter() {
                                            @TargetApi(Build.VERSION_CODES.HONEYCOMB)
                                            @Override
                                            public void onAnimationEnd(Animator animation) {
                                                super.onAnimationEnd(animation);
                                                drawer.setVisibility(View.INVISIBLE);
                                                drawer.setTranslationY(0);
                                            }
                                        });
                                try {
                                    View infoBlock = mWrapperView.findViewById(R.id.lock_screen_info_block);
                                    infoBlock.setVisibility(View.VISIBLE);
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                                    onFatalError();
                                }
                            }
                        });
            } else {
                drawer.setVisibility(View.INVISIBLE);
                try {
                    View infoBlock = mWrapperView.findViewById(R.id.lock_screen_info_block);
                    infoBlock.setVisibility(View.VISIBLE);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                    onFatalError();
                }
                phoneButtons.setVisibility(View.INVISIBLE);
                widgets.setVisibility(View.VISIBLE);
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;
        }

    }

    private void enableErrorViewsinView() {
        final View drawer;

        try {
            drawer = mWrapperView.findViewById(R.id.drawer_lock_screen_call_fail_display);
            final int dDistance = drawer.getHeight();
            drawer.setTranslationY(dDistance * -1);
            drawer.setVisibility(View.VISIBLE);
            // Now start the drawer animation
            drawer.animate()
                    .translationY(0)
                    .setListener(new AnimatorListenerAdapter() {
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            try {
                                View infoBlock = mWrapperView
                                        .findViewById(R.id.lock_screen_info_block);
                                infoBlock.setVisibility(View.INVISIBLE);
                            } catch (NullPointerException e) {
                                Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                                onFatalError();
                            }
                        }
                    });
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;
        }
    }

    private void disableErrorViewsInView() {
        final View drawer;
        try {
            drawer = mWrapperView.findViewById(R.id.drawer_lock_screen_call_fail_display);
            final int dDistance = drawer.getHeight();
            try {
                View infoBlock = mWrapperView
                        .findViewById(R.id.lock_screen_info_block);
                infoBlock.setVisibility(View.INVISIBLE);
            } catch (NullPointerException e) {
                Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                onFatalError();
            }

            // Now start the drawer animation
            drawer.animate()
                    .translationY(dDistance * -1)
                    .setListener(new AnimatorListenerAdapter() {
                        @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            drawer.setVisibility(View.INVISIBLE);
                            drawer.setTranslationY(0);

                        }
                    });
        } catch (ClassCastException e) {
            Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;
        }
    }

    private void enableOptionalViewsInView() throws CallHandlerException {
        //Log.d(TAG, "Enabling Optional Views");
        setOptionalViewsInView(View.VISIBLE);
    }

    private void disableOptionalViewsInView() throws CallHandlerException {
        //Log.d(TAG, "Disabling Optional Views");
        setOptionalViewsInView(View.GONE);
    }

    /**
     * Sets the views defined by the IDs in the XML array to the value parameter, typically either
     * View.GONE or View.VISIBILE
     * @param value
     */
    private void setOptionalViewsInView (int value) throws CallHandlerException {

        if (value != View.GONE && value != View.VISIBLE && value != View.INVISIBLE) {
            Log.e(TAG, "Invalid argument passed to setOptionalViewsInView");
            return;
        }

        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);

        if (keys.length() != ids.length()) {  // TODO: excpetion?
            //Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            throw new CallHandlerException(this.toString()
                    + "XML arrays for keys and ids to optional views mismatched.");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i=0; i < keys.length(); i++) {
            //Log.d(TAG, "Iteration " + i + " - value " + value + " - " + keys.getString(i));

            View view = mWrapperView.findViewById(ids.getResourceId(i, -1));
            if (view == null) { // There should always be at least an existing id somewhere for the view in the XML
                Log.w(TAG, "Iteration " + i + " of key " + keys.getString(i)
                        + " in setOptionalViewsInView() has invalid id.");
                continue;
            }

            // access to SharedPrefs throws a class cast exception if stored type is not as sought - FYI
            switch (view.getId()) {
                case R.id.lock_screen_clock:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(value);
                    }
                    break;
                case R.id.lock_screen_date:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(value);
                    }
                    break;
                case R.id.lock_screen_info:
                    if (!prefs.getString(keys.getString(i), "").equals("")) {
                        view.setVisibility(value);
                    }
                    break;
            }
        }
    }

    private void instantiateOptionalViewsInView() throws CallHandlerException {
        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int viewId;
        View view;

        if (keys.length() != ids.length()) {
            //Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            throw new CallHandlerException(this.toString()
                    + "XML arrays for keys and ids to optional views mismatched.");
        }

        for (int i=0; i < keys.length(); i++){
            viewId = ids.getResourceId(i, -1); // gets the corresponding id for that view (hopefully)
            view = mWrapperView.findViewById(viewId);

            if (view == null) { // Error check
                Log.w(TAG, "Iteration " + i + " of key " + keys.getString(i)
                        + " in setOptionalViewsInView() has invalid id.");  // TODO: do we need to make this an error?
                continue;
            }

            try {

                switch (viewId) {
                    case R.id.lock_screen_date:
                        if (prefs.getBoolean(keys.getString(i), false)) {
                            SimpleDateFormat df = new SimpleDateFormat(getString(R.string.date_format));
                            ((TextView) view).setText(df.format(new Date()));
                            view.setVisibility(View.VISIBLE);
                        } else {
                            view.setVisibility(View.INVISIBLE);
                        }

                        break;

                    case R.id.lock_screen_clock:
                        if (prefs.getBoolean(keys.getString(i), false)) {
                            view.setVisibility(View.VISIBLE);
                        } else {
                            view.setVisibility(View.INVISIBLE);
                        }
                        break;

                    case R.id.lock_screen_info:
                        String s = prefs.getString(keys.getString(i), "");
                        if (s.equals("")) {  //equivalent of no value
                            continue;
                        } else {
                            ((TextView) view).setText(s);
                        }
                        break;

                    case R.id.lock_screen_speed_dial_toggle:
                        //Log.d(TAG, "instantiating toggle button");

                        /*ToggleButton toggle = (ToggleButton) mWrapperView.findViewById(
                                R.id.lock_screen_speed_dial_toggle);*/

                        TextView tv = (TextView) mWrapperView.findViewById(
                                R.id.lock_screen_speed_dial_toggle_text);
                        if (prefs.getBoolean(keys.getString(i), false)) {
                            view.setVisibility(View.VISIBLE);
                            ((ToggleButton) view).setOnCheckedChangeListener(this);
                            if (tv != null) {
                                tv.setVisibility(View.VISIBLE);
                                if (prefs.getBoolean(
                                        getString(R.string.key_toggle_speed_dial_enabled),
                                        false)) {
                                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_on));
                                    ((ToggleButton) view).setChecked(true);
                                } else {
                                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_off));
                                    ((ToggleButton) view).setChecked(false);
                                }
                            } else {
                                Log.w(TAG, "Layout does not have toggle button text");
                            }
                        } else {
                            view.setVisibility(View.INVISIBLE);
                            if (tv != null) {
                                tv.setVisibility(View.INVISIBLE);
                            } else {
                                Log.w(TAG, "Layout does not have toggle button text");
                            }
                        }
                        break;
                    case R.id.lock_screen_speed_dial_toggle_text:
                        if (prefs.getBoolean(keys.getString(i), false)) {  // We will use the same key as lock_screen_speed_dial_toggle in the XML array


                        } else {
                            Log.w(TAG, "No toggle-button text view in this layout.");
                        }

                }
            } catch (ClassCastException e) {
                Log.e(TAG, "Layout incompatible with activity", e);
                onFatalError();
            }
        }

    }

    /**
     * Method that provides acceptable display information depending on the information available.
     * @param telNum
     * @param name
     * @param type
     * @return
     */
    private String getDialInfoViewText(String telNum, String name, String type){
        if (name != null && type != null ) {
            return "Call with " + name + " on " + type.toLowerCase() + "....";
        }
        else if (name != null & telNum != null) {
            return "Call with " + name + "on phone no. " + telNum + " ....";
        }
        else if (name != null) { // this shouldn't happen, but....
            return "Call with " + name + " ....";
        }
        else if (telNum != null) { // this should be case where call was received
            return "Call with phone no. " + telNum + " ....";
        }

        return "Call ongoing ....";
    }


    private void endPhoneCall() {
        TelephonyManager telephonyManager;
        Class c;
        Method m;
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        try {

            c = Class.forName(telephonyManager.getClass().getName());
            m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(telephonyManager);
            telephonyService.endCall();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        mPhoneCallActiveFlag = false;


    }

    private boolean isCallActive() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager.getMode() == AudioManager.MODE_IN_CALL) {
            return true;
        } else {
            return false;
        }
    }

    public void onClick (View view) {
        switch (view.getId()) {
            case R.id.lock_screen_end_call_button:
                endPhoneCall();  // We have the generation of a phone intent handle the logic in onNewIntent()
                break;
            case R.id.lock_screen_speaker_call_button:
                try {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    ImageButton btn = (ImageButton) mWrapperView
                            .findViewById(R.id.lock_screen_speaker_call_button);
                    if (am.isSpeakerphoneOn()) {
                        am.setSpeakerphoneOn(false);
                        btn.setImageResource(R.drawable.ic_volume_up_white_48dp);
                    } else {
                        am.setSpeakerphoneOn(true);
                        btn.setImageResource(R.drawable.ic_volume_mute_white_48dp);
                    }
                    break;
                } catch (NullPointerException e) {
                    Log.e(TAG, "Layout not compatible with phone calls, missing speaker button.", e);
                    onFatalError();
                }
        }
    }

    public boolean onTouch(final View view, MotionEvent event) {
        if (view.getId() != R.id.lock_screen_sheath_container) {
            return false;
        }
        final View interactionScreen = mWrapperView.findViewById(R.id.lock_screen_interaction_container);

        try {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mDetector.onTouchEvent(event);
                    mLastMoveCoord = event.getRawY();
                    mFlinged = false;
                    Log.d(TAG, "Action Down " + event.toString());
                    break;
                case MotionEvent.ACTION_MOVE:
                    mDetector.onTouchEvent(event);
                    float sheathPos = view.getTranslationY();
                    float interactionPos = interactionScreen.getTranslationY();
                    view.setTranslationY(sheathPos + (event.getRawY() - mLastMoveCoord));
                    interactionScreen.setTranslationY(interactionPos + (event.getRawY() - mLastMoveCoord));
                    mLastMoveCoord = event.getRawY();

                    break;
                case MotionEvent.ACTION_UP:
                    mDetector.onTouchEvent(event);
                    Log.d(TAG, "Action Up " + event.toString());
                    int moveTolerance = getResources()
                            .getInteger(R.integer.swipe_percent_move_tolerance);
                    if (view.getTranslationY() / view.getHeight() < (-0.01 * moveTolerance)
                            && !mFlinged) {
                        // Animate sheath
                        view.animate()
                                .translationY(view.getHeight() * -1)
                                .setListener(new AnimatorListenerAdapter() {
                                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        view.setVisibility(View.INVISIBLE);
                                    }
                                });
                        interactionScreen.animate()
                                .translationY(0);

                    } else if (!mFlinged) {
                        // Return
                        view.animate()
                                .translationY(0)
                                .setListener(null);

                        interactionScreen.animate()
                                .translationY(interactionScreen.getHeight());
                    }

                    break;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Layout not compatible with animation; interaction screen id unavailable", e);
            onFatalError();
        }
        return true;
    }

    /*@Override
    public boolean onTouchEvent (MotionEvent event) {
        Log.d(TAG, "onTouchEvent called with " + event.toString());
        mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }*/

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //Log.d(TAG, "lock screen speed dial toggle button pressed");
        try {
            TextView tv = (TextView) mWrapperView.findViewById(
                    R.id.lock_screen_speed_dial_toggle_text);

            if (tv != null) { // not necessary to have a text view as we implemented, so can come back null!
                if (isChecked) {
                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_on));
                } else {
                    tv.setText(getString(R.string.lock_screen_speed_dial_toggle_off));
                }
            }
        } catch (ClassCastException e) {
            // This isn't a fatal error.
            Log.w(
                    TAG,
                    "Activity does not have valid lock screen toggle text view for custom functionality",
                    e);
        }
    }

    /**
     * Simple method that handles logic when the correct passcode is entered
     */
    protected void onCorrectPasscode() {
        //Log.d(TAG, "Finishing activity, clearing service.");
        //unregisterReceiver(mReceiver);
        stopService(new Intent(this, PhoneStateService.class));
        finish();
    }

    protected void onFatalError() {
        Log.d(TAG, "onFatalError() called.");
        stopService(new Intent(this, PhoneStateService.class));
        finish();

        //TODO: some kind of dialog or toast or something?
    }

    protected void enableLongPressFlag () { mLongPressFlag = true; }

    protected void enablePhoneCallActiveFlag() { mPhoneCallActiveFlag = true; }

    protected boolean getPhoneCallActiveFlag() { return mPhoneCallActiveFlag; }

    protected RelativeLayout getWrapperView(){ return mWrapperView; }

    /**
     * Helper method to set the appropriate background, or load it from memory to be displayed
     * once loaded.
     * @param view
     */
    private void setActivityBackground(ImageView view) {
        /*if (mBackgroundBitmap != null) { // if we already have an instantiated background, return
            Log.d(TAG, "setActivityBackground() called when background already set");
            return;
        }*/

        SharedPreferences prefs = getSharedPreferences(
                getString(R.string.background_file_key),
                MODE_PRIVATE);
        int color = prefs.getInt(getString(R.string.key_background_color), -1);
        String filePath = prefs.getString(getString(R.string.key_background_pic), null);
        int orientation = prefs.getInt(getString(R.string.key_background_orientation), -1);
        File file= null;
        if (filePath != null) {
            file = new File(filePath);
        }

        if (color != -1) { // since color is set, we just set the background to that and return null
            Log.d(TAG, "setting activity to a color");
            //view.setImageBitmap(null);
            view.setBackgroundColor(color);
            view.setVisibility(View.VISIBLE);
            mBackgroundProgress.setVisibility(View.GONE);
            mBackgroundSetFlag = true;
            return;

        } else if (filePath == null) { // then we have the default image situation
            Log.d(TAG, "setting activity to default image");


            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(), R.drawable.background_default);
            mBackgroundView.setImageBitmap(bitmap);
            view.setVisibility(View.VISIBLE);
            mBackgroundProgress.setVisibility(View.GONE);
            //mBackgroundBitmap = bitmap;
            mBackgroundSetFlag = true;

        } else if (file != null && file.exists()){ //now we must retrieve and set up the stored picture
            if (!mBackgroundSetFlag) {
                Log.d(TAG, "setting background image from stored data");
                Display display = getWindowManager().getDefaultDisplay();
                Bitmap bitmap = null;
                int w = getDisplayWidth();
                int h = getDisplayHeight();

                BitmapToViewHelper.assignBitmapWithData(this, filePath, orientation, w, h);  // Calls getBitmapFromTask interface method below to set mBackgroundBitmap
                //view.setImageBitmap(mBackgroundBitmap);
                //BitmapToViewHelper.assignViewWithBitmap(view, filePath, orientation, w, h);
                //mBackgroundBitmap = ((BitmapDrawable) view.getDrawable()).getBitmap();
            }
        } else { // this should be an error -- no data for background and no color and no default?
            Log.e(TAG, "Fatal error in assigning background");
            onFatalError();
        }
    }

    /**
     * Returns the display width
     */
    private int getDisplayWidth() {
        Display display = getWindowManager().getDefaultDisplay();

        // Get the right screen size in manner depending on version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.x;

        } else {
            return display.getWidth();
        }
    }

    /**
     * Returns the display height
     * @return
     */
    private int getDisplayHeight(){
        Display display = getWindowManager().getDefaultDisplay();

        // Get the right screen size in manner depending on version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.y;

        } else {
            return display.getHeight();
        }
    }
    /**
     * Interface from BitmapToViewHelper
     * There is a strange error.  If we try to set the imageview background in this method, the
     * findViewByID method will return null.  We suspect that the call here, which is made from a
     * task, will have different resources available to it that when these methods are typically
     * called from Android.  So, for that reason, we do not instantiate the view from here.
     */
    public void getBitmapFromTask(Bitmap bitmap) {
        mBackgroundView.setImageBitmap(bitmap);
        mBackgroundSetFlag = true;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            int animationTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mBackgroundView.setAlpha(0f);
            mBackgroundView.setVisibility(View.VISIBLE);

            mBackgroundView.animate()
                    .alpha(1f)
                    .setDuration(animationTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // Here we can slide in the text view information stuff
                            //Log.d(TAG, "screenText translationX is " + mScreenText.getTranslationX());
                            if (!mSheathScreenOn) {
                                mScreenText.animate().translationX(0);
                            }
                        }
                    });

            mBackgroundProgress.animate()
                    .alpha(0f)
                    .setDuration(animationTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBackgroundProgress.setVisibility(View.GONE);
                        }
                    });
        } else {
            mBackgroundView.setVisibility(View.VISIBLE);
            mBackgroundProgress.setVisibility(View.GONE);
        }

    }



    protected boolean isSpeedDialEnabled() {
        ToggleButton toggle = (ToggleButton)mWrapperView.findViewById(
                R.id.lock_screen_speed_dial_toggle);

        // TODO: OK for now, but need to check whether speed dial enabled at all in the settings
        return toggle.isChecked();
    }

    /**
     * Sets a view and all subviews to a particular visibility parameter
     *
     */
    final protected void setVisibilityOnView (View v, int visibility) {
        if (visibility != View.GONE || visibility != View.INVISIBLE || visibility != View.VISIBLE) {
            Log.e (TAG, "Attempt to set view and view's children with improper visibility value");
            return;
        }

        List<View> unvisited = new ArrayList<View>();
        unvisited.add(v);

        while (!unvisited.isEmpty()) {
            View child = unvisited.remove(0);
            child.setVisibility(visibility);
            if (!(child instanceof ViewGroup)) {
                continue;
            }
            ViewGroup group = (ViewGroup) child;
            final int childCount = group.getChildCount();
            for (int i=0; i<childCount; i++) {
                unvisited.add(group.getChildAt(i));
            }
        }
    }

    private Uri getPhotoThumbnailUri (String data) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return Uri.parse(data);
        }

        return Uri.withAppendedPath(
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, data),
                ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
        );
    }

    /**
     * Abstract function that just returns the resource Id of the subclass's fragment layout view
     */
    abstract int getFragmentLayout();

    /**
     * ---------------------------------------------------------------------------------------------
     * ABSTRACT METHODS
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * Runnable class that initiates the phone call on a long press
     */
    protected class DialerRunnable implements Runnable {
        private int num; // the digit of the button pressed
        private Context context;

        public DialerRunnable(Context context, int num) {
            this.num = num;
            this.context = context;
        }

        public void run() {

            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.speed_dial_preference_file_key),
                    Context.MODE_PRIVATE);

            if (num == -1) { //error handling
                Log.d(TAG, "Error in finding view id.");
                return;
            }

            String telNum = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_phone) + num, null);
            String name = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_name) + num, "Unknown");
            String type = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_type) + num, "Phone");
            String thumbUri = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_thumb) + num, null);

            if (telNum == null) {
                Log.e(TAG, "Unable to make call because phone number invalid");
                return;
            }

            // Check for roaming
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephony.isNetworkRoaming()) {
                enableErrorViewsinView();
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disableErrorViewsInView();
                    }
                }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            } else if (!getPhoneCallActiveFlag() && isSpeedDialEnabled()) {  //Only want to initiate the call if line is idle and speed dial enabled
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + (telNum.trim())));
                startActivity(intent);
                //stopService(new Intent(context, LockScreenService.class));   //This caused errors when unlocking the screen during the call
                enableCallViewsInView(telNum, name, type, thumbUri);
                try {
                    disableOptionalViewsInView();
                } catch (CallHandlerException e) {
                    Log.e(TAG, "Activity unable to handle calls", e);
                    onFatalError();
                }
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(350);
                enableLongPressFlag();
                enablePhoneCallActiveFlag();
            }

        }
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String TAG = "MyGestureListener";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(TAG, "onDown is " + event.getY());
            return true;
        }


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            int minDistance = getResources().getInteger(R.integer.swipe_min_distance);
            int thresholdVel = getResources().getInteger(R.integer.swipe_threshold_velocity);

            Log.d(TAG, "VelX = " + velocityX + " VelY = " + velocityY + " e1 = " + e1.getRawY() + " e2 = " + e2.getRawY());
            if (e1.getRawY() - e2.getRawY() > minDistance && Math.abs(velocityY) > thresholdVel) {
                Log.d(TAG, "Threshold reached, animating.");
                final View sheathScreen = mWrapperView.findViewById(R.id.lock_screen_sheath_container);
                View interactionScreen = mWrapperView.findViewById(R.id.lock_screen_interaction_container);
                sheathScreen.animate()
                        .translationY(sheathScreen.getHeight() * -1)
                        .setListener(new AnimatorListenerAdapter() {
                            @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                sheathScreen.setVisibility(View.INVISIBLE);
                            }
                        });
                interactionScreen.animate().translationY(0);
                mFlinged = true;

            }
            return false;
        }
    }
}


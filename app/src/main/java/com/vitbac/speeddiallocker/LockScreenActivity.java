package com.vitbac.speeddiallocker;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.view.GestureDetectorCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.internal.telephony.ITelephony;
import com.vitbac.speeddiallocker.exceptions.IllegalLayoutException;
import com.vitbac.speeddiallocker.helpers.BitmapToViewHelper;
import com.vitbac.speeddiallocker.services.LockScreenService;
import com.vitbac.speeddiallocker.services.PhoneCallReceiver;
import com.vitbac.speeddiallocker.services.PhoneStateReceiver;
import com.vitbac.speeddiallocker.services.PhoneStateService;
import com.vitbac.speeddiallocker.views.PasscodeEntryDisplay;
import com.vitbac.speeddiallocker.views.PasscodeEntryView;
import com.vitbac.speeddiallocker.views.PullBackView;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;


public class LockScreenActivity extends Activity implements View.OnClickListener,
        View.OnTouchListener, CompoundButton.OnCheckedChangeListener,
        BitmapToViewHelper.GetBitmapFromTaskInterface, PasscodeEntryView.OnPasscodeEntryListener,
        PasscodeEntryView.OnLongPressListener, PasscodeEntryDisplay.OnLockoutListener,
        PasscodeEntryView.OnInputReceivedListener, PasscodeEntryDisplay.OnDeletePressed {

    private final static String TAG = "LSActivity2";

    // Timer to handle on long clicks using the ontouchlistener -- this will presumably be used by all instances
    protected Handler mHandler;
    protected DialerRunnable mRunnable;
    protected boolean mLongPressFlag;
    // A runnable/handler issued to provide notification of an error
    private Handler mErrorHandler;
    private Runnable mErrorRunnable;
    private Runnable mErrorRemoveRunnable;
    // A runnable/handler issued to reset the passcode view
    private Handler mPasscodeResetHandler;
    private Runnable mPasscodeResetRunnable;

    // Variables to implement TYPE_SYSTEM_ERROR stuff
    private WindowManager mWindowManager;
    private RelativeLayout mWindowView;
    private View mContainerView;

    // Variables for the backgrounds and animation layouts
    // Unfortunately, for whatever reason retrieving these variables dynamically while the phone is
    // tilted results in an error, so it is important to get them and hold them from the beginning
    private ImageView mBackgroundView;
    //private ProgressBar mBackgroundProgress;
    private View mBackgroundProgress;
    private TextView mSheathTextView;

    // Variables to utilize phone state service and handle phone calls
    private boolean mPhoneCallActiveFlag;
    private String mPhoneNumOnCall;
    private String mPhoneTypeOnCall;
    private String mContactNameOnCall;
    private int mSpeedDialNumPressed;
    private boolean mBackgroundSetFlag;
    private boolean mPhoneCallAnimOn;

    // Sheath screen related variables
    private GestureDetectorCompat mDetector;
    private float mLastMoveCoord;
    private boolean mFlinged;
    private boolean mSheathScreenOn;
    private boolean mSheathInstructionFlag;

    private Date mDate; // To check whether date needs updating

    // Views that concern the passcode
    private PasscodeEntryDisplay mDisplayView;
    private PasscodeEntryView mPasscodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called.");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lock_screen_launcher); // To clear rotation error on Samsung Galaxy devices pre Lollipop
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Check phone call status first -- any phone call status existing in onCreate()
        // terminates this activity
        int phoneState = getIntent().getIntExtra(PhoneCallReceiver.EXTRA_PHONE_STATE, -1);
        switch (phoneState) {
            case PhoneStateReceiver.STATE_ENDED_INCOMING_CALL:
                // Start the screen service and the lock screen --
                // An incoming call was received, previously ending the lock screen, and the lock screen
                // is now restarting.
                startService(new Intent(this, LockScreenService.class));
                /*startActivity(new Intent(this, LockScreenLauncherActivity.class));
                finish();
                return;*/
                break;
            case PhoneStateReceiver.STATE_ENDED_OUTGOING_CALL:
                // End the activity but begin the screen service --
                // Because we are getting this state in onCreate, it means the user initiated an outgoing
                // call, but then unlocked the lock screen. Since phone call ended, just resume the
                // screen service and end.
                startService(new Intent(this, LockScreenService.class));
                //startActivity(new Intent(this, LockScreenLauncherActivity.class));
                /*finish();
                return;*/
                break;
            case PhoneStateReceiver.STATE_MISSED_CALL:
                // Start the screen service and the lock screen --
                // An incoming call was received while lock screen initiated, previously ending the lock
                // screen, and now the lock screen is just restarting
                startService(new Intent(this, LockScreenService.class));
                /*startActivity(new Intent(this, LockScreenLauncherActivity.class));
                finish();
                return;*/
                break;
            case PhoneStateReceiver.STATE_STARTED_INCOMING_CALL:
                // This situation should not result in initiating the lock screen in onCreate()
                // TODO: Only possibility is for call waiting to have been received, so handle in the receiver!
                throw new IllegalArgumentException("Received improper state STATE_STARTED_INCOMING_CALL in onCreate().");
            case PhoneStateReceiver.STATE_STARTED_OUTGOING_CALL:
                // This situation should not result in initiating the lock screen in onCreate(), but in
                // onNewIntent()
                throw new IllegalArgumentException("Received improper state STATE_STARTED_OUTGOING_CALL in onCreate(). Should be received in onNewIntent()");
        }

        //  Lock screen was initiated naturally by screen event or by rerouting to the launcher
        startService(new Intent(this, PhoneStateService.class));

        // Set up the error runnables
        mErrorRunnable = new Runnable() {
            @Override
            public void run() {
                enableErrorViewsinView(
                        getString(R.string.error_title_offhook),
                        getString(R.string.error_description_offhook),
                        R.drawable.ic_dnd_on_white_48dp
                );
                endPhoneCall();
            }
        };
        mErrorRemoveRunnable = new Runnable() {
            @Override
            public void run() {
                disableErrorViewsInView();
            }
        };
        mErrorHandler = new Handler();
        mPhoneCallActiveFlag = false;

        // Set up the window
        WindowManager.LayoutParams localLayoutParams;
        if (prefs.getBoolean(getString(R.string.key_toggle_status_bar_access), false)) {
            localLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // To avoid notification bar
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, //Same
                    PixelFormat.TRANSLUCENT);

        } else {
            localLayoutParams = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | // To avoid notification bar
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | // Same
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, //Same
                    PixelFormat.TRANSLUCENT);
        }
        // Set hack to control orientation for underlying apps that get activated!
        localLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        mWindowManager = (WindowManager) getApplicationContext().getSystemService(WINDOW_SERVICE);
        getWindow().setAttributes(localLayoutParams);
        //View.inflate(this, R.layout.activity_lock_screen_keypad_pin, mWindowView);
        mWindowView = (RelativeLayout) LayoutInflater
                .from(this)
                .inflate(R.layout.activity_lock_screen2,
                        new RelativeLayout(getBaseContext()),
                        false);
        //mWindowView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        mWindowManager.addView(mWindowView, localLayoutParams);

        mContainerView = mWindowView.findViewById(R.id.activity_container);

        /*try {
            validateLayout();
        } catch (IllegalLayoutException e) {
            Log.e(TAG, e.getMessage(), e);
            onFatalError();
            return;
        }*/
        validateLayout();

        /*try {
            instantiateOptionalViewsInView();
            mDate = new Date();
        } catch (IllegalLayoutException e) {
            Log.e(TAG, e.getMessage(), e);
            onFatalError();
            return;
        }*/
        instantiateOptionalViewsInView();
        mDate = new Date();

        //Inflate the locking mechanism fragment XML and prepare those views
        String lockScreenType = getIntent().getStringExtra(getString(R.string.key_lock_screen_type));
        Log.d(TAG, "Lock screen type is " + lockScreenType);
        View lockMechFragment;
        if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pattern))) {
            lockMechFragment = getLayoutInflater()
                    .inflate(R.layout.fragment_lock_screen_pattern2, null);
        } else if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pin))){
            lockMechFragment = getLayoutInflater()
                    .inflate(R.layout.fragment_lock_screen_keypad_pin2, null);
        } else {
            throw new IllegalArgumentException("Received invalid value for lock screen type: " + lockScreenType);
        }
        FrameLayout container = (FrameLayout) getView(R.id.lock_screen_fragment_container);
        if (lockMechFragment == null) {
            /*Log.e(TAG, "Null fragment provided by subclass.");
            onFatalError();
            return;*/
            throw new IllegalLayoutException("lock mechanism fragment was null");
        }
        mPasscodeView =
                (PasscodeEntryView)lockMechFragment.findViewById(R.id.lock_screen_passcode_entry_view);
        mPasscodeView.setPasscode(getStoredPasscode());
        mPasscodeView.setOnPassCodeEntryListener(this);
        mPasscodeView.setOnLongPressListener(this);
        mDisplayView =
                (PasscodeEntryDisplay)lockMechFragment.findViewById(R.id.lock_screen_passcode_display);
        mDisplayView.setOnLockoutListener(this);
        // Set an on input received listener only if PIN
        if (lockScreenType.equals(getString(R.string.value_lock_screen_type_keypad_pin))) {
            mPasscodeView.setOnInputReceivedListener(this);
            mDisplayView.setOnDeletePressedListener(this);
        }

        // Set the font for these views
        String font = prefs.getString(
                getString(R.string.key_select_lock_screen_fonts),
                getString(R.string.font_default));
        mPasscodeView.setTypeface(Typeface.create(font, Typeface.NORMAL));
        mDisplayView.setTypeface(Typeface.create(font, Typeface.NORMAL));

        // Now add the lock mech XML to the view tree
        container.addView(lockMechFragment);

        // Determine if the sheath screen is enabled and prepare the display
        mSheathScreenOn = prefs.getBoolean(getString(R.string.key_toggle_sheath_screen), false);
        mPhoneCallAnimOn = prefs
                .getBoolean(getString(R.string.key_toggle_phone_call_animations), true);
        // Since we never do side animation with the lock screen if we have a sheath, and we want to
        // set up the lock screen animation before the background loading begins and never in onResume
        mBackgroundSetFlag = false;
        mBackgroundView.setVisibility(View.GONE);
        setActivityBackground(mBackgroundView);
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
    }



    @Override
    protected void onResume() {
        super.onResume();

        int phoneState = getIntent().getIntExtra(PhoneCallReceiver.EXTRA_PHONE_STATE, -1);
        //Log.d(TAG, "onResume() called with phone state = " + phoneState);
        switch (phoneState) {
            /*case PhoneStateReceiver.STATE_ENDED_INCOMING_CALL:
                // Need to do nothing, everything should have been handled in onCreate()
                //Log.e(TAG, "Received improper state STATE_ENDED_INCOMING_CALL in onNewIntent().");
                //throw new IllegalArgumentException("getIntent() provided improper state STATE_ENDED_INCOMING_CALL in onResume().");
                break;*/
            case PhoneStateReceiver.STATE_ENDED_OUTGOING_CALL:
                // Close the call views and reenable the screen service--
                // A call was initiated in the lock screen, a passcode was not entered, and the call
                // ended.
                mContactNameOnCall = mPhoneNumOnCall = mPhoneTypeOnCall = null;
                disableCallViewsInView(true);
                enableOptionalViewsInView();
                /*try {
                    enableOptionalViewsInView();
                } catch (IllegalLayoutException e) {
                    //Log.e(TAG, "Layout renders activity unable to handle calls", e);
                    onFatalError();
                }*/
                // Service may or may not have been started already in onCreate(), but should be OK
                startService(new Intent(this, LockScreenService.class));
                mPhoneCallActiveFlag = false;
                break;
            case PhoneStateReceiver.STATE_STARTED_OUTGOING_CALL:
                // Initiate the call drawer and call buttons --
                // User has initiated a speed dial.  Doing the animations here allows them to be
                // much more fluid
                //Log.d(TAG, "Outgoing call initiated");
                if (!mPhoneCallActiveFlag) {
                    // We don't want to reinstantiate the call views on a call to onResume() if the
                    // phone call active flag is already set
                    SharedPreferences sharedPref = getSharedPreferences(
                            getString(R.string.speed_dial_preference_file_key),
                            Context.MODE_PRIVATE);

                    String telNum = sharedPref.getString(
                            getString(R.string.key_number_store_prefix_phone)
                                    + mSpeedDialNumPressed, null);
                    String name = sharedPref.getString(
                            getString(R.string.key_number_store_prefix_name)
                                    + mSpeedDialNumPressed, "Unknown");
                    String type = sharedPref.getString(
                            getString(R.string.key_number_store_prefix_type)
                                    + mSpeedDialNumPressed, "Phone");
                    String thumbUri = sharedPref.getString(
                            getString(R.string.key_number_store_prefix_thumb)
                                    + mSpeedDialNumPressed, null);

                    enableCallViewsInView(telNum, name, type, thumbUri);

                    disableOptionalViewsInView();
                    /*try {
                        disableOptionalViewsInView();
                    } catch (IllegalLayoutException e) {
                        Log.e(TAG, "Activity unable to handle calls", e);
                        onFatalError();
                    }*/

                    mPhoneCallActiveFlag = true;

                    // Remove any runnables for error messages
                    if (mErrorHandler != null) {
                        mErrorHandler.removeCallbacks(mErrorRunnable);
                        mErrorHandler.removeCallbacks(mErrorRemoveRunnable);
                    }
                }
                break;
            case PhoneStateReceiver.STATE_STARTED_INCOMING_CALL:
                // Shut down the screen service and end the activity --
                // Incoming phone call ends the service under current implementation
                //Log.d(TAG, "Received incoming call in onResume()");
                stopService(new Intent(this, LockScreenService.class));
                finish();
                break;
            /*case PhoneStateReceiver.STATE_MISSED_CALL:
                // Do nothing because this should have been handled in onCreate()
                // The lock screen should have been ended when a call was received in the first place
                //Log.e(TAG, "Received improper state STATE_MISSED_CALL in onResume()");
                //throw new IllegalArgumentException("getIntent() provided improper state STATE_MISSED_CALL in onResume().");*/
            default:
                if (!mSheathScreenOn && !mBackgroundSetFlag) {
                    //if (!mSheathScreenOn && !mPhoneCallActiveFlag) {
                    mContainerView.post(new Runnable() {
                        public void run() {
                            prepareLockScreenAnimation();
                        }
                    });
                } else if (mSheathScreenOn && !mPhoneCallActiveFlag) {
                    mFlinged = false;
                    if (mBackgroundSetFlag) {
                        // Note no need to do a .post call, because if this flag is set, mContainView's post is already complete
                        prepareSheathScreenAnimation(true);
                        // Covers situation where power button pressed after swiping sheath away
                        doSheathTextAnimation(-1);
                    } else {
                        mContainerView.post(new Runnable() {
                            public void run() {
                                prepareSheathScreenAnimation(false);
                            }
                        });
                    }
                }
                break;
        }

        //See if we need to update the date
        updateDateViews();

        // For backwards compatibility, we need to manually set the "clock" text view
        // to the current time
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            try {
                TextView lockClock = (TextView) getView(R.id.lock_screen_clock);
                TextView sheathClock = (TextView) getView(R.id.sheath_screen_clock);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                Calendar cal = Calendar.getInstance(TimeZone.getDefault());
                SimpleDateFormat sdf;
                if (DateFormat.is24HourFormat(this)) {
                    sdf = new SimpleDateFormat("H:mm", Locale.getDefault());
                } else {
                    sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                }
                lockClock.setText(sdf.format(cal.getTime()));
                sheathClock.setText(sdf.format(cal.getTime()));
            } catch (ClassCastException e) {
                /*Log.e(TAG, "Layout has improper clock view type for older versions.", e);
                onFatalError();
                return;*/
                throw new IllegalLayoutException("Layout has improper clock view type for older SDK versions.");
            } catch (NullPointerException e) {
                /*Log.e(TAG, "Layout does not have clock view.", e);
                onFatalError();
                return;*/
                throw new IllegalLayoutException("Layout does not have proper clock view.");
            }
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int receivedPhoneState = getIntent().getIntExtra(PhoneCallReceiver.EXTRA_PHONE_STATE, -1);

        // Only want to reset the old saved flag if this activity wasn't recalled by virtue of the phone being hung up.
        // TODO: does this make sense?  Not really -- any ended calls are rerouted to the Launcher, so this information doesn't exist
        if (receivedPhoneState != PhoneStateReceiver.STATE_ENDED_INCOMING_CALL
                && receivedPhoneState != PhoneStateReceiver.STATE_ENDED_OUTGOING_CALL
                && receivedPhoneState != PhoneStateReceiver.STATE_MISSED_CALL) {
            mPhoneCallActiveFlag = savedInstanceState.getBoolean("phoneCallActiveFlag");
            Log.d(TAG, "PHONE CALL FLAG IS NOW " + mPhoneCallActiveFlag);
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
        /*int phoneState = intent.getIntExtra(PhoneCallReceiver.EXTRA_PHONE_STATE, -1);
        Log.d(TAG, "onNewIntent called with phoneState = " + phoneState);*/
        setIntent(intent); // Sets up to handle all logic in onResume()
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged() called, newConfig = " + newConfig.toString());
        super.onConfigurationChanged(newConfig);
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
        if (mWindowManager != null && mWindowView != null) {
            mWindowManager.removeView(mWindowView);
            ((ImageView) mWindowView.findViewById(R.id.lock_screen_background_view)).setImageBitmap(null);  // Probably not necessary
            mWindowView.removeAllViews();
        }

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
     * ---------------------------------------------------------------------------------------------
     * DISPLAY METHODS
     * ---------------------------------------------------------------------------------------------
     */

    private void prepareSheathScreenAnimation(boolean animate) {
        //Log.d(TAG, "prepareSheathScreenAnimation() called");
        View sheathScreen = getView(R.id.lock_screen_sheath_container);
        View lockScreen = getView(R.id.lock_screen_interaction_container);


        // Set the visibility of the views
        sheathScreen.setVisibility(View.VISIBLE);
        lockScreen.setVisibility(View.VISIBLE);
        if (!mBackgroundSetFlag) {
            getView(R.id.lock_screen_sheath_instruction).setVisibility(View.INVISIBLE);
        }

        // Set the translation, animated or not
        if (animate && sheathScreen.getTranslationY() != 0
                && lockScreen.getTranslationY() != getDisplayHeight()) {
            sheathScreen.animate().translationY(0).setListener(null);
            lockScreen.animate().translationY(getDisplayHeight()).setListener(null);
        } else {
            sheathScreen.setTranslationY(0);
            lockScreen.setTranslationY(getDisplayHeight());
        }

        sheathScreen.setOnTouchListener(this);
    }

    private void doSheathScreenAnimation(final boolean swiped) {
        //Log.d(TAG, "doSheathScreenAnimation() called, swiped = " + swiped);
        final View sheathScreen = getView(R.id.lock_screen_sheath_container);
        final View lockScreen = getView(R.id.lock_screen_interaction_container);
        int sheathPos;
        int lockPos;

        if (sheathScreen.getHeight() != lockScreen.getHeight()) {
            Log.w(TAG, "Sheath and lock not the same height, sheath = "
                    + sheathScreen.getHeight() + " and lock = " + lockScreen.getHeight());
        }

        if (swiped) {
            //sheathPos = sheathScreen.getHeight() * -1;
            sheathPos = getDisplayHeight() * -1;
            lockPos = 0;
        } else {
            sheathPos = 0;
            //lockPos = lockScreen.getHeight();
            lockPos = getDisplayHeight();
        }
        sheathScreen.animate()
                .translationY(sheathPos)
                .setListener(new AnimatorListenerAdapter() {
                    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        if (swiped) {
                            sheathScreen.setVisibility(View.INVISIBLE);
                        }
                        mFlinged = false;
                    }
                });
        lockScreen.animate()
                .translationY(lockPos)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mFlinged = false;
                    }
                });

    }

    /**
     * Animates the sheath text to the alpha parameter.  If the alpha is less than 0,
     * method does the initial animation sequence
     *
     * @param alpha
     */
    private void doSheathTextAnimation(float alpha) {
        if (!mSheathInstructionFlag) {
            return;
        }

        final int longAnimTime = getResources().getInteger(R.integer.sheath_text_long_animation);
        final int shortAnimTime = getResources().getInteger(R.integer.sheath_text_short_animation);

        if (alpha < 0) {
            /*mSheathTextView
                    .setAlpha(getResources().getFraction(R.fraction.sheath_text_alpha_min, 1, 1));*/
            mSheathTextView.setVisibility(View.VISIBLE);
            mSheathTextView.animate()
                    .alpha(getResources().getFraction(R.fraction.sheath_text_alpha_max, 1, 1))
                    .setDuration(longAnimTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mSheathTextView.animate()
                                    .alpha(getResources()
                                            .getFraction(R.fraction.sheath_text_alpha_static, 1, 1))
                                    .setDuration(longAnimTime)
                                    .setListener(null);
                        }
                    });
        } else {
            mSheathTextView.animate()
                    .alpha(alpha)
                    .setDuration(shortAnimTime)
                    .setListener(null);
        }
    }

    private void prepareLockScreenAnimation() {
        //Log.d(TAG, "prepareLockScreenAnimation() called");
        View sheathScreen = getView(R.id.lock_screen_sheath_container);
        View lockScreen = getView(R.id.lock_screen_interaction_container);
        lockScreen.setTranslationX(getDisplayWidth());
        lockScreen.setVisibility(View.VISIBLE);
        sheathScreen.setVisibility(View.INVISIBLE);  // Since lock screen animation only performed without sheath, we can just hide this
    }

    private void doLockScreenAnimation() {
        //Log.d(TAG, "doLockScreenAnimation() called");
        View lockScreen = getView(R.id.lock_screen_interaction_container);
        lockScreen.animate().translationX(0);
    }

    /**
     * View a is crossfaded in while View b is crossfaded out, and sts the sheath animation as appropriate
     *
     * @param a
     * @param b
     */
    private void crossFadeViewsOnStart(final View a, final View b) {
        //Log.d(TAG, "Crossfading views");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            a.setAlpha(0f);
            a.setVisibility(View.VISIBLE);
            b.setAlpha(1f);
            b.setVisibility(View.VISIBLE);
            int animTime = getResources().getInteger(android.R.integer.config_mediumAnimTime);
            a.animate()
                    .alpha(1f)
                    .setDuration(animTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @SuppressLint("NewApi")
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            // Here we can slide in the text view information stuff
                            //Log.d(TAG, "screenText translationX is " + mLockScreen.getTranslationX());
                            if (!mSheathScreenOn) {
                                //mLockScreen.animate().translationX(0);
                                doLockScreenAnimation();
                            } else {
                                // Instantiate the sheath text animation
                                doSheathTextAnimation(-1);
                            }
                            mBackgroundSetFlag = true;
                        }
                    });

            b.animate()
                    .alpha(0f)
                    .setDuration(animTime)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            b.setVisibility(View.GONE);
                        }
                    });
        } else {
            a.setVisibility(View.VISIBLE);
            b.setVisibility(View.GONE);
            mBackgroundSetFlag = true;
        }
    }

    /**
     * Animates the call drawer and call button animations when a phone call is initiated by the user.
     * This is the implementation of this parent class that requires certain view IDs --
     * @param telNum - a string with the telephone number
     * @param name - a string to display the contact's name
     * @param type - a string to display the phone number type
     */
    private void enableCallViewsInView(String telNum, String name, String type, String thumbUri){
        try {
            final Button endCallBtn, spkrBtn;
            final ViewGroup phoneButtons, widgets;
            final TextView phoneCallName, phoneCallDescr, phoneCallNum;
            final ImageView phoneCallThumb;
            final View drawer, infoBlock;

            // Get the info views
            drawer = getView(R.id.drawer_lock_screen_call_display);
            infoBlock = getView(R.id.lock_screen_info_block);
            phoneCallThumb = (ImageView) getView(R.id.drawer_phone_call_thumb);
            phoneCallName = (TextView) getView(R.id.drawer_phone_call_name);
            phoneCallDescr = (TextView) getView(R.id.drawer_phone_call_description);
            phoneCallNum = (TextView) getView(R.id.drawer_phone_call_number);
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
                    //phoneCallThumb.setImageURI(Uri.parse(thumbUri));
                    //Log.d(TAG, "thumbUri = " + thumbUri);
                    Uri photoUri = Uri.parse(thumbUri);
                    Cursor cursor = getContentResolver().query(photoUri,
                            new String[]{ContactsContract.Contacts.Photo.PHOTO}, null, null, null);
                    if (cursor == null) {
                        phoneCallThumb.setImageResource(android.R.color.transparent);
                        phoneCallThumb.setImageResource(R.drawable.default_contact_image);
                    } else {
                        try {
                            if (cursor.moveToFirst()) {
                                byte[] data = cursor.getBlob(0);
                                if (data != null) {
                                    //Log.d(TAG, "Applying bitmap to thumb view");
                                    phoneCallThumb.setImageBitmap(
                                            BitmapFactory
                                                    .decodeStream(new ByteArrayInputStream(data)));
                                } else {
                                    phoneCallThumb.setImageResource(android.R.color.transparent);
                                    phoneCallThumb.setImageResource(R.drawable.default_contact_image);
                                }
                            } else {
                                phoneCallThumb.setImageResource(android.R.color.transparent);
                                phoneCallThumb.setImageResource(R.drawable.default_contact_image);
                            }
                        } finally {
                            cursor.close();
                        }
                    }

                }
                if (phoneCallThumb.getDrawable() == null) {
                    //Log.d(TAG, "Thumb drawable null");
                }
            } else {
                phoneCallThumb.setImageResource(android.R.color.transparent);
                phoneCallThumb.setImageResource(R.drawable.default_contact_image);
            }
            // Get the phone button views
            phoneButtons = (ViewGroup) getView(R.id.lock_screen_phone_buttons);
            widgets = (ViewGroup) getView(R.id.lock_screen_additional_widgets);
            endCallBtn = (Button) getView(R.id.lock_screen_end_call_button);
            spkrBtn = (Button) getView(R.id.lock_screen_speaker_call_button);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 && mPhoneCallAnimOn) {
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
                                    infoBlock.setVisibility(View.INVISIBLE);
                                } catch (NullPointerException e) {
                                    /*Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                                    onFatalError();*/
                                    throw new IllegalLayoutException("Layout lacks necessary view infoBlock to complete drawer animation");
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
                infoBlock.setVisibility(View.INVISIBLE);
                /*try {
                    infoBlock.setVisibility(View.INVISIBLE);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                    onFatalError();
                }*/

                phoneButtons.setVisibility(View.VISIBLE);
                endCallBtn.setVisibility(View.VISIBLE);
                spkrBtn.setVisibility(View.VISIBLE);
                ((ViewGroup) endCallBtn.getParent()).setVisibility(View.VISIBLE);
                widgets.setVisibility(View.INVISIBLE);
                endCallBtn.setOnClickListener(this);
                spkrBtn.setOnClickListener(this);
            }
        } catch (ClassCastException e) {
            /*Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout types incompatible with this activity - enableCallViewsInView().");
        } catch (NullPointerException e) {
            /*Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout missing views and incompatible with this activity - enableCallViewsInView()");
        }
    }

    /**
     * Disables the call drawer when a phone call has been completed
     *
     * @param animationFlag - value true animates the drawer and buttons
     */
    private void disableCallViewsInView(boolean animationFlag) {
        //Log.d(TAG, "disableCallViewsInView() called");
        ImageButton endCallBtn;

        final ViewGroup phoneButtons, widgets;  // Declared final for anonymous function purpose
        final View drawer, infoBlock;

        try {
            drawer = getView(R.id.drawer_lock_screen_call_display);
            phoneButtons = (ViewGroup) getView(R.id.lock_screen_phone_buttons);
            widgets = (ViewGroup) getView(R.id.lock_screen_additional_widgets);
            infoBlock = getView(R.id.lock_screen_info_block);


            if (animationFlag && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1
                    && mPhoneCallAnimOn) {
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
                                    infoBlock.setVisibility(View.VISIBLE);
                                } catch (NullPointerException e) {
                                    /*Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                                    onFatalError();*/
                                    throw new IllegalLayoutException("Layout lacks necessary view to complete animation.");
                                }
                            }
                        });
            } else {
                drawer.setVisibility(View.INVISIBLE);
                try {
                    infoBlock.setVisibility(View.VISIBLE);
                } catch (NullPointerException e) {
                    /*Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                    onFatalError();*/
                    throw new IllegalLayoutException("Layout lacks necessary view to complete animation.");
                }
                phoneButtons.setVisibility(View.INVISIBLE);
                widgets.setVisibility(View.VISIBLE);
            }
        } catch (ClassCastException e) {
            /*Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout types incompatible with this activity - enableCallViewsInView().");
        } catch (NullPointerException e) {
            /*Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout missing views and incompatible with this activity - enableCallViewsInView()");
        }

    }

    /**
     * Enables the error drawer when the user is in roaming
     */
    private void enableErrorViewsinView(String title, String description, int imageResource) {
        final View drawer, infoBlock;

        try {
            drawer = getView(R.id.drawer_lock_screen_call_fail_display);
            TextView titleView = (TextView) drawer.findViewById(R.id.drawer_phone_call_fail_title);
            titleView.setText(title);
            TextView descriptionView = (TextView) drawer
                    .findViewById(R.id.drawer_phone_call_fail_description);
            descriptionView.setText(description);
            ImageView imgView = (ImageView) drawer.findViewById(R.id.drawer_phone_call_fail_thumb);
            imgView.setImageResource(imageResource);

            infoBlock = getView(R.id.lock_screen_info_block);
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
                                infoBlock.setVisibility(View.INVISIBLE);
                            } catch (NullPointerException e) {
                                /*Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                                onFatalError();*/
                                throw new IllegalLayoutException("Layout lacks necessary view infoBlock to complete animation.");
                            }
                        }
                    });
        } catch (ClassCastException e) {
            /*Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout types incompatible with this activity - enableCallViewsInView().");
        } catch (NullPointerException e) {
            /*Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout missing views and incompatible with this activity - enableCallViewsInView()");
        }
    }

    /**
     * Removes the error drawer
     */
    private void disableErrorViewsInView() {
        final View drawer, infoBlock;
        try {
            drawer = getView(R.id.drawer_lock_screen_call_fail_display);
            infoBlock = getView(R.id.lock_screen_info_block);
            final int dDistance = drawer.getHeight();
            try {
                infoBlock.setVisibility(View.INVISIBLE);
            } catch (NullPointerException e) {
                /*Log.e(TAG, "Layout lacks necessary view to complete animation", e);
                onFatalError();*/
                throw new IllegalLayoutException("Layout lacks necessary view to complete animation");
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
            /*Log.e(TAG, "Layout types incompatible with this activity - enableCallViewsInView().", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout types incompatible with this activity - enableCallViewsInView().");
        } catch (NullPointerException e) {
            /*Log.e(TAG, "Layout missing views and incompatible with this activity - enableCallViewsInView()", e);
            onFatalError();
            return;*/
            throw new IllegalLayoutException("Layout missing views and incompatible with this activity - enableCallViewsInView()");
        }
    }

    private void enableOptionalViewsInView() throws IllegalLayoutException {
        //Log.d(TAG, "Enabling Optional Views");
        setOptionalViewsInView(View.VISIBLE);
    }

    private void disableOptionalViewsInView() throws IllegalLayoutException {
        //Log.d(TAG, "Disabling Optional Views");
        setOptionalViewsInView(View.GONE);
    }

    /**
     * Sets the views defined by the IDs in the XML array to the value parameter,
     * typically either View.GONE or View.VISIBILE
     * @param value
     */
    private void setOptionalViewsInView(int value) throws IllegalLayoutException {

        if (value != View.GONE && value != View.VISIBLE && value != View.INVISIBLE) {
            Log.e(TAG, "Invalid argument passed to setOptionalViewsInView");
            return;
        }

        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);

        if (keys.length() != ids.length()) {  // TODO: excpetion?
            //Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            throw new IllegalLayoutException(this.toString()
                    + "XML arrays for keys and ids to optional views mismatched.");
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        for (int i=0; i < keys.length(); i++) {
            //Log.d(TAG, "Iteration " + i + " - value " + value + " - " + keys.getString(i));

            View view = getView(ids.getResourceId(i, -1));
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

    /**
     * Initiates the values of the optional views
     * @throws IllegalLayoutException
     */
    private void instantiateOptionalViewsInView() throws IllegalLayoutException {
        TypedArray keys = getResources().obtainTypedArray(R.array.optional_display_view_file_keys);
        TypedArray ids = getResources().obtainTypedArray(R.array.optional_display_view_layout_ids);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int viewId;
        View view;

        if (keys.length() != ids.length()) {
            //Log.e(TAG, "XML arrays for keys and ids to optional views mismatched.");
            throw new IllegalLayoutException(this.toString()
                    + "XML arrays for keys and ids to optional views mismatched.");
        }

        for (int i=0; i < keys.length(); i++){
            viewId = ids.getResourceId(i, -1); // gets the corresponding id for that view (hopefully)
            view = getView(viewId);

            if (view == null) { // Error check
                Log.w(TAG, "Iteration " + i + " of key " + keys.getString(i)
                        + " in setOptionalViewsInView() has invalid id.");  // TODO: do we need to make this an error?
                continue;
            }

            switch (viewId) {
                case R.id.lock_screen_date:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        /*String dateFormat = prefs.getString(
                                getString(R.string.key_date_format),
                                getString(R.string.pref_default_value_date_format));
                        java.util.Date dateTime = Calendar.getInstance().getTime();
                        String dateString = DateFormat.format(dateFormat, dateTime).toString();*/
                        //SimpleDateFormat df = new SimpleDateFormat(getString(R.string.date_format));
                        ((TextView) view).setText(getFormattedDate());
                        view.setVisibility(View.VISIBLE);

                    } else {
                        view.setVisibility(View.GONE);
                    }

                    break;

                case R.id.sheath_screen_date:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        //SimpleDateFormat df = new SimpleDateFormat(getString(R.string.date_format));
                        ((TextView) view).setText(getFormattedDate());
                        view.setVisibility(View.VISIBLE);
                    } else {
                        view.setVisibility(View.INVISIBLE);
                    }
                    break;

                case R.id.lock_screen_clock:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(View.VISIBLE);
                        /*if (Build.VERSION.SDK_INT >= 17 && view instanceof TextClock
                                && prefs.getBoolean(getString(R.string.key_toggle_24_hr_clock), false)) {
                            TextClock tc = (TextClock) view;
                            tc.setFormat24Hour("HH:mm");
                        }*/
                    } else {
                        view.setVisibility(View.GONE);
                        // Now let's set the date to the dominate view
                        TextView lockDate = (TextView) getView(R.id.lock_screen_date);
                        if (lockDate != null) {
                            float size = getResources().getDimensionPixelSize(R.dimen.lock_screen_main_info_size);
                            //Log.d(TAG, "Size value is " + size);
                            lockDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                        }
                    }
                    break;

                case R.id.sheath_screen_clock:
                    if (prefs.getBoolean(keys.getString(i), false)) {
                        view.setVisibility(View.VISIBLE);
                        /*if (Build.VERSION.SDK_INT >= 17 && view instanceof TextClock
                                && prefs.getBoolean(getString(R.string.key_toggle_24_hr_clock), false)) {
                            Log.d(TAG, "Setting to 24 hr mode");
                            TextClock tc = (TextClock) view;
                            tc.setFormat24Hour("H:mm");
                        }*/
                    } else {
                        view.setVisibility(View.GONE);
                        // Now let's set the date to the dominate view
                        TextView sheathDate = (TextView) getView(R.id.sheath_screen_date);
                        if (sheathDate != null) {
                            float size = getResources().getDimensionPixelSize(R.dimen.sheath_screen_main_info_size);
                            sheathDate.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
                        }
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

                    TextView tv = (TextView) getView(R.id.lock_screen_speed_dial_toggle_text);
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
                            String font = prefs.getString(
                                    getString(R.string.key_select_accessory_fonts),
                                    getString(R.string.font_default));
                            //Only place where this logic needs to be specifically inserted
                            if (!font.equals(getString(R.string.font_default))) {
                                tv.setTypeface(Typeface.create(font, Typeface.NORMAL));
                            }
                            // Now set the color of the background button
                            try {
                                // Get drawing color
                                int color = PreferenceManager
                                        .getDefaultSharedPreferences(this)
                                        .getInt(
                                                getString(R.string.key_select_speed_dial_button_color),
                                                getResources().getColor(R.color.blue_diamond));

                                LayerDrawable layerList = (LayerDrawable) getResources()
                                        .getDrawable(R.drawable.toggle_button_on);
                                GradientDrawable shape = (GradientDrawable) layerList
                                        .findDrawableByLayerId(R.id.toggle_button_color);
                                shape.setColor(color);

                                StateListDrawable sld = new StateListDrawable();
                                // The order here is critically important -- android selects
                                // FIRST valid state, traversed based on the order they are added
                                sld.addState(new int[]{android.R.attr.state_pressed},
                                        getResources().getDrawable(R.color.pressed));
                                sld.addState(new int[]{android.R.attr.state_checked}, layerList);
                                sld.addState(new int[]{},
                                        getResources().getDrawable(android.R.color.transparent));

                                ((ToggleButton) view).setBackgroundDrawable(sld);

                            } catch (NullPointerException e) {
                                Log.w(TAG, "No toggle button layout or color");
                            } catch (ClassCastException e) {
                                Log.w(TAG, "Toggle button layers of wrong type");
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
                        // Even though invisible, this allows us just to check the toggle button
                        if (prefs.getBoolean(
                                getString(R.string.key_toggle_speed_dial_enabled),
                                false)) {
                            ((ToggleButton) view).setChecked(true);
                        } else {
                            ((ToggleButton) view).setChecked(false);
                        }
                    }
                    break;
                case R.id.lock_screen_speed_dial_toggle_text:
                    if (prefs.getBoolean(keys.getString(i), false)) {  // We will use the same key as lock_screen_speed_dial_toggle in the XML array


                    } else {
                        Log.w(TAG, "No toggle-button text view in this layout.");
                    }
                    break;

                case R.id.lock_screen_sheath_instruction:
                    if (prefs.getBoolean(getString(R.string.key_toggle_sheath_instruction), true)) {
                        view.setVisibility(View.VISIBLE);
                        mSheathInstructionFlag = true;
                    } else {
                        view.setVisibility(View.INVISIBLE);
                        mSheathInstructionFlag = false;
                    }

            }
            // Now change the font of the optional views.
            String font = prefs.getString(
                    getString(R.string.key_select_accessory_fonts),
                    getString(R.string.font_default));
            if (!font.equals(getString(R.string.font_default)) && view instanceof TextView) {
                ((TextView) view).setTypeface(Typeface.create(font, Typeface.NORMAL));
            }
        }

    }

    /**
     * Display method to set the appropriate background, or load it from memory to be displayed
     * once loaded.
     * @param view
     */
    private void setActivityBackground(ImageView view) {
        /*SharedPreferences prefs = getSharedPreferences(
                getString(R.string.background_file_key),
                MODE_PRIVATE);*/

        SharedPreferences defPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences bgPrefs = getSharedPreferences(
                getString(R.string.file_background_type),
                Context.MODE_PRIVATE);
        String bgTypeValue = defPrefs.getString(
                getString(R.string.key_select_background_type),
                getString(R.string.value_background_type_app_content));

        // Set the background according to the background type
        if (bgTypeValue.equals(getString(R.string.value_background_type_app_content))) {
            // Set app content to background
            int picResourceId = bgPrefs.getInt(
                    getString(R.string.key_select_background_app_content),
                    AppBackgroundActivity.RANDOM_PIC);
            // Assign a resource id if it is supposed to be random
            if (picResourceId == AppBackgroundActivity.RANDOM_PIC) {
                TypedArray appPics = getResources().obtainTypedArray(R.array.app_pics);
                Random rand = new Random();
                int randomNum = rand.nextInt(appPics.length());
                //Log.d(TAG, "Random number generated is " + randomNum);
                picResourceId = appPics.getResourceId(randomNum, 0);
            }
            Bitmap bitmap = BitmapFactory.decodeResource(
                    getResources(), picResourceId);
            mBackgroundView.setImageBitmap(bitmap);
            crossFadeViewsOnStart(mBackgroundView, mBackgroundProgress);
            return;
        } else if (bgTypeValue.equals(getString(R.string.value_background_type_user_device))) {
            try {
                FileInputStream streamIn =
                        openFileInput(getString(R.string.stored_background_file_name));
                Bitmap bitmap = BitmapFactory.decodeStream(streamIn);
                mBackgroundView.setImageBitmap(bitmap);
                //mBackgroundSetFlag = true;
                //Log.d(TAG, "About to crossfade bitmap background");
                crossFadeViewsOnStart(mBackgroundView, mBackgroundProgress);
                return;
            } catch (IOException e) {
                // TODO: make this a toast
                Log.e(TAG, "Unable to obtain bitmap, defaulting to color", e);
            }
        }
        // Set color to background
        int color = bgPrefs.getInt(
                getString(R.string.key_select_background_color),
                getResources().getColor(R.color.default_background_color));
        view.setBackgroundColor(color);
        crossFadeViewsOnStart(view, mBackgroundProgress);
    }


    /**
     * ---------------------------------------------------------------------------------------------
     * INTERFACE IMPLEMENTATION METHODS
     * ---------------------------------------------------------------------------------------------
     */

    public void onClick (View view) {
        switch (view.getId()) {
            case R.id.lock_screen_end_call_button:
                endPhoneCall();  // We have the generation of a phone intent handle the logic in onNewIntent()
                break;
            case R.id.lock_screen_speaker_call_button:
                try {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    Button btn = (Button) getView(R.id.lock_screen_speaker_call_button);
                    if (am.isSpeakerphoneOn()) {
                        am.setSpeakerphoneOn(false);
                        //btn.setImageResource(R.drawable.ic_volume_up_white_48dp);
                        btn.setCompoundDrawablesWithIntrinsicBounds(
                                0, R.drawable.ic_volume_up_white_48dp, 0, 0);
                    } else {
                        am.setSpeakerphoneOn(true);
                        //btn.setImageResource(R.drawable.ic_volume_mute_white_48dp);
                        btn.setCompoundDrawablesWithIntrinsicBounds(
                                0, R.drawable.ic_volume_mute_white_48dp, 0, 0);
                    }
                    break;
                } catch (NullPointerException e) {
                    /*Log.e(TAG, "Layout not compatible with phone calls, missing speaker button.", e);
                    onFatalError();*/
                    throw new IllegalLayoutException("Layout not compatible with phone calls, missing speaker button.");
                }
        }
    }

    public boolean onTouch(final View view, MotionEvent event) {
        //Log.d(TAG, "onTouch() called");
        if (view.getId() != R.id.lock_screen_sheath_container) {
            return false;
        } else if (mFlinged) {
            return true;
        }
        final View interactionScreen = getView(R.id.lock_screen_interaction_container);
        int moveTolerance = getResources()
                .getInteger(R.integer.swipe_percent_move_tolerance);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDetector.onTouchEvent(event);
                mLastMoveCoord = event.getRawY();
                doSheathTextAnimation(
                        getResources().getFraction(R.fraction.sheath_text_alpha_max, 1, 1));
                break;
            case MotionEvent.ACTION_MOVE:
                mDetector.onTouchEvent(event);
                float sheathPos = view.getTranslationY();
                float interactionPos = interactionScreen.getTranslationY();
                // If statement to create pushback when user tries to pull sheath down
                if (sheathPos < view.getHeight() * (moveTolerance / 2) * 0.01) {
                    // Move the sheath screen in natural fashion
                    PullBackView pbv = (PullBackView) getView(R.id.pull_back_view);
                    if (pbv.isPullBackActivated()) {
                        if (sheathPos <= 0) {
                            pbv.deactivate();
                        } else {
                            drawPullbackView(event.getRawX(), event.getRawY());
                        }
                    }
                    view.setTranslationY(sheathPos + (event.getRawY() - mLastMoveCoord));
                    interactionScreen
                            .setTranslationY(interactionPos + (event.getRawY() - mLastMoveCoord));

                } else {
                    // Show the pull back regardless
                    try {
                        PullBackView pbv = (PullBackView) getView(R.id.pull_back_view);
                        if (!pbv.isPullBackActivated()) {
                            // Starts a reference point for the pull back view
                            pbv.setTouchStartPos(event.getRawY());
                        }
                        drawPullbackView(event.getRawX(), event.getRawY());
                    } catch (Exception e) {
                        Log.e(TAG, "Unable to animate pull back because view is not in layout or otherwise invalid", e);
                    }
                    if (sheathPos >= view.getHeight() * (moveTolerance / 2) * 0.01) {

                        if (event.getRawY() < mLastMoveCoord) { // Only move sheath if touch is going up
                            view.setTranslationY(sheathPos + (event.getRawY() - mLastMoveCoord));
                            interactionScreen
                                    .setTranslationY(interactionPos + (event.getRawY() - mLastMoveCoord));
                        }
                    }
                }
                mLastMoveCoord = event.getRawY();

                break;
            case MotionEvent.ACTION_UP:
                mDetector.onTouchEvent(event);

                // Kill the pull back view
                /*try {
                    PullBackView pbv = (PullBackView) getView(R.id.pull_back_view);
                    pbv.deactivate();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to animate pull back because view is not in layout or otherwise invalid", e);
                }*/
                PullBackView pbv = (PullBackView) getView(R.id.pull_back_view);
                pbv.deactivate();
                if (view.getTranslationY() / view.getHeight() < (-0.01 * moveTolerance)
                        && !mFlinged) {
                    // Animate sheath
                    doSheathScreenAnimation(true);
                } else if (!mFlinged) {
                    // Return
                    doSheathScreenAnimation(false);
                    doSheathTextAnimation(getResources().getFraction(
                            R.fraction.sheath_text_alpha_static, 1, 1));
                }
                break;
        }

        return true;
    }

    private void drawPullbackView(float x, float y) {
        PullBackView pbv = (PullBackView) getView(R.id.pull_back_view);

        Paint p = new Paint();
        p.setColor(getResources().getColor(R.color.default_pullback_color));
        p.setAntiAlias(true);
        p.setAlpha(getResources().getInteger(R.integer.default_pullback_alpha));

        pbv.paintPullBackAtTop(p, x, y);
    }

    /**
     * Interface with toggle button
     * @param buttonView
     * @param isChecked
     */
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //Log.d(TAG, "lock screen speed dial toggle button pressed");
        try {
            TextView tv = (TextView) getView(R.id.lock_screen_speed_dial_toggle_text);

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
     * Interface from BitmapToViewHelper
     * There is a strange error.  If we try to set the imageview background in this method, the
     * findViewByID method will return null.  We suspect that the call here, which is made from a
     * task, will have different resources available to it that when these methods are typically
     * called from Android.  So, for that reason, we do not instantiate the view from here.
     */
    public void getBitmapFromTask(Bitmap bitmap) {
        mBackgroundView.setImageBitmap(bitmap);
        //mBackgroundSetFlag = true;
        //Log.d(TAG, "About to crossfade bitmap background");
        crossFadeViewsOnStart(mBackgroundView, mBackgroundProgress);
    }

    public void onPasscodeEntered(boolean isCorrect) {
        if (isCorrect) {
            onCorrectPasscode();
            return;
        }
        onIncorrectPasscode();
    }

    public void onInputReceived(String input) {
        // The listener was only set for lock mech type PIN, so we can do as we please here
        mDisplayView.setPasscodeText(input);
    }

    public void onLockout(int delay) {
        //  PasscodeView should already have it's input blocked
        if (mPasscodeResetHandler == null) {
            mPasscodeResetHandler = new Handler();
        }
        mPasscodeResetRunnable = new Runnable() {
            @Override
            public void run() {
                mPasscodeView.resetView();
            }
        };
        mPasscodeResetHandler.postDelayed(mPasscodeResetRunnable, delay);
    }

    public void onDeletePressed() {
        mPasscodeView.backspace();
    }

    public void onLongPress (int key) {

        // First check if speed dial is enabled
        if (!isSpeedDialEnabled()) {
            enableErrorViewsinView(
                    getString(R.string.error_title_speed_dial_inactive),
                    getString(R.string.error_description_speed_dial_inactive),
                    R.drawable.ic_error_white_48dp);
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableErrorViewsInView();
                }
            }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            return;
        }

        // Get the phone data for this key
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.speed_dial_preference_file_key),
                Context.MODE_PRIVATE);
        String telNum = sharedPref.getString(
                getString(R.string.key_number_store_prefix_phone) + key, null);

        // Check if there is a valid assigned to the key
        if (telNum == null) {
            enableErrorViewsinView(
                    getString(R.string.error_title_invalid_number),
                    getString(R.string.error_description_invalid_number),
                    R.drawable.ic_error_white_48dp);
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableErrorViewsInView();
                }
            }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            return;
        }

        // Check for roaming
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (telephony.isNetworkRoaming()) {
            enableErrorViewsinView(
                    getString(R.string.error_title_roaming),
                    getString(R.string.error_description_roaming),
                    R.drawable.ic_signal_cellular_connected_no_internet_2_bar_white_48dp);
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableErrorViewsInView();
                }
            }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            return;
        }

        // Check if airplane mode is on
        if (isAirplaneModeOn()) {
            enableErrorViewsinView(
                    getString(R.string.error_title_airplane),
                    getString(R.string.error_description_airplane),
                    R.drawable.ic_airplanemode_on_white_48dp);
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    disableErrorViewsInView();
                }
            }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            return;
        }

        // Finally check if there is an ongoing phone call (no need to display error message if so)
        if (!getPhoneCallActiveFlag()) {
            mDisplayView.displayMessage(getString(R.string.lock_screen_initiate_call));
            mSpeedDialNumPressed = key;
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + (telNum.trim())));
            startActivity(intent);
            //stopService(new Intent(context, LockScreenService.class));   //This caused errors when unlocking the screen during the call

            // Vibrate the phone to indicate call being attempted
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(350);
            enableLongPressFlag();

            // Now set a runnable for the case where a OFF_HOOK intent is not timely received
            if (mErrorHandler != null && mErrorRunnable != null
                    && mErrorRemoveRunnable != null) {
                int errorDisplayDelay = getResources()
                        .getInteger(R.integer.lock_screen_phone_error_call_failed_delay);
                int errorDisplayLength = getResources()
                        .getInteger(R.integer.lock_screen_phone_error_display_length);
                mErrorHandler.postDelayed(mErrorRunnable, errorDisplayDelay);
                mErrorHandler.postDelayed(
                        mErrorRemoveRunnable,
                        errorDisplayDelay + errorDisplayLength
                );
            }
        }

    }

    /**
     * ---------------------------------------------------------------------------------------------
     * RESULT METHODS
     * ---------------------------------------------------------------------------------------------
     */
    /**
     * Simple method that handles logic when the correct passcode is entered
     */
    protected void onCorrectPasscode() {
        stopService(new Intent(this, PhoneStateService.class));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String sound = prefs.getString(
                getString(R.string.key_select_passcode_correct_sound),
                getString(R.string.passcode_correct_default));
        final Uri uri = Uri.parse(sound);
        final Context ctx = this;

        View v = mContainerView;
        /*playSound(ctx, uri);
        finish();*/
        Log.d(TAG, "Finish() called");
        // Fade out the lock screen
        if (v != null) {
            v.animate().alpha(0f).setDuration(200).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    playSound(ctx, uri);
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    finish();
                }
            });
        } else {
            playSound(ctx, uri);
            finish();
        }
    }

    private void onIncorrectPasscode() {
        if (!mDisplayView.wrongEntry()) {
            // means no delay has been imposed so we can reset the view
            Log.d(TAG, "Resetting view immediately");
            mPasscodeView.resetView();
        }
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(200);
    }

    private void playSound(Context context, Uri alert) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(context, alert);
            final AudioManager audioManager = (AudioManager) context
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) != 0) {
                float log1 = (float) (Math.log(25) / Math.log(50)); //half volume
                mediaPlayer.setVolume(1 - log1, 1 - log1);
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_NOTIFICATION);
                mediaPlayer.prepare();
                mediaPlayer.start();
            }
        } catch (IOException e) {
            Log.e(TAG, "Caught IOException in trying to play sound.", e);
        }
    }

   /* protected void onFatalError() {
        Log.d(TAG, "onFatalError() called.");
        stopService(new Intent(this, PhoneStateService.class));
        finish();

        //TODO: some kind of dialog or toast or something?
    }*/


    /**
     * ---------------------------------------------------------------------------------------------
     * HELPER METHODS
     * ---------------------------------------------------------------------------------------------
     */

    private void validateLayout() {
        // Check that the layout has the requisite phone-related elements for this activity to function
        if (getView(R.id.lock_screen_end_call_button) == null ||
                getView(R.id.lock_screen_speaker_call_button) == null ||
                getView(R.id.lock_screen_phone_buttons) == null ||
                getView(R.id.drawer_lock_screen_call_display) == null ||
                getView(R.id.lock_screen_background_progress) == null ||
                getView(R.id.lock_screen_background_view) == null ||
                getView(R.id.lock_screen_interaction_container) == null ||
                getView(R.id.lock_screen_sheath_container) == null ||
                getView(R.id.lock_screen_fragment_container) == null) {
            throw new IllegalLayoutException("Layout does not have one or more required view ids");
        }
        //  Check that the layout elements are of the right type
        try {
            Button b = (Button) getView(R.id.lock_screen_end_call_button);
            ViewGroup vg = (ViewGroup) b.getParent(); // ensures the correct encapsulating layout is there
            // TextView v = (TextView)mWindowView.findViewById(R.id.lock_screen_call_display);
            mBackgroundView = (ImageView) getView(R.id.lock_screen_background_view);
            //mBackgroundProgress = (ProgressBar) getView(R.id.lock_screen_background_progress);
            mBackgroundProgress = getView(R.id.lock_screen_background_progress);
            mSheathTextView = (TextView) getView(R.id.lock_screen_sheath_instruction);
        } catch (ClassCastException e) {
            throw new IllegalLayoutException("Layout does not have the requisite view classes");
        }

    }

    private String getDialInfoViewText(String telNum, String name, String type) {
        if (name != null && type != null) {
            return "Call with " + name + " on " + type.toLowerCase() + "....";
        } else if (name != null & telNum != null) {
            return "Call with " + name + "on phone no. " + telNum + " ....";
        } else if (name != null) { // this shouldn't happen, but....
            return "Call with " + name + " ....";
        } else if (telNum != null) { // this should be case where call was received
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

        // Better placed to a location when all the phone buttons are retracted
        //mPhoneCallActiveFlag = false;
    }

    private boolean isCallActive() {
        AudioManager manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (manager.getMode() == AudioManager.MODE_IN_CALL) {
            return true;
        } else {
            return false;
        }
    }

    protected void enableLongPressFlag() {
        mLongPressFlag = true;
    }

    protected void enablePhoneCallActiveFlag() {
        //Log.d(TAG, "PHONE CALL FLAG IS NOW TRUE");
        mPhoneCallActiveFlag = true;
    }

    // TODO: should we just check with the telephony manager here to see if a call is active?  I think so.
    protected boolean getPhoneCallActiveFlag() {
        return mPhoneCallActiveFlag;
    }

    /**
     * Use is probably deprecated now
     *
     * @return
     */
    protected View getContainerView() {
        return mContainerView;
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
        /*Display display = getWindowManager().getDefaultDisplay();

        // Get the right screen size in manner depending on version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            Point size = new Point();
            display.getSize(size);
            return size.y;

        } else {
            return display.getHeight();
        }*/
        return mContainerView.getHeight();
    }

    protected boolean isSpeedDialEnabled() {
        try {
            ToggleButton toggle = (ToggleButton) getView(R.id.lock_screen_speed_dial_toggle);
            return toggle.isChecked();
        } catch (Exception e) {
            Log.e(TAG, "Layout has invalid toggle button view; enabling speed dial");
            return true;
        }
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

    protected View getView(int id) {
        return mContainerView.findViewById(id);
    }

    private String getFormattedDate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String dateFormat = prefs.getString(
                getString(R.string.key_date_format),
                getString(R.string.pref_default_value_date_format));
        if (!dateFormat.equals(getString(R.string.pref_default_value_date_format))) {
            Date dateTime = Calendar.getInstance().getTime();
            return DateFormat.format(dateFormat, dateTime).toString();
        } else {
            // Return a default locale formatted date
            Date date = new Date();
            return DateFormat.getDateFormat(this).format(date);
        }
    }

    private String getStoredPasscode() {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.file_lock_screen_type),
                Context.MODE_PRIVATE);
        return sharedPref.getString(getString(R.string.value_lock_screen_passcode), null);
    }

    private void updateDateViews() {
        try {
            Date currentDate = new Date();
            if (currentDate.getDate() != mDate.getDate()) {
                //Log.d(TAG, "Comparing mDate with currentDate; currentDate is " + currentDate.toString() + " and mDate is " + mDate.toString());
                TextView sheathDate, lockDate;
                String dateString;
                sheathDate = (TextView) getView(R.id.sheath_screen_date);
                lockDate = (TextView) getView(R.id.lock_screen_date);
                dateString = getFormattedDate();
                sheathDate.setText(dateString);
                lockDate.setText(dateString);
                mDate = currentDate;
            }
        } catch (Exception e) {
            Log.e(TAG, "Activity unable to update date views", e);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private boolean isAirplaneModeOn() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return Settings.System.getInt(getContentResolver(),
                    Settings.System.AIRPLANE_MODE_ON, 0) != 0;
        } else {
            return Settings.Global.getInt(getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
        }
    }


    protected void setDialerRunnable(int numPressed, int delay) {
        if (isSpeedDialEnabled() && !getPhoneCallActiveFlag()
                && numPressed > 0 && numPressed < 10) {
            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.speed_dial_preference_file_key),
                    Context.MODE_PRIVATE);
            String filename = getString(R.string.key_number_store_prefix_phone)
                    + numPressed;
            //Log.d(TAG, "Setting dialer runnable click on key " + b.getText());
            if (sharedPref.getString(filename, null) != null) {
                //Log.d(TAG, "Setting dialer runnable click on key " + b.getText());
                mHandler = new Handler();
                mRunnable = new DialerRunnable(numPressed);
                mHandler.postDelayed(mRunnable, delay);
                Vibrator vibrator =
                        (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(1);
            }
        }
    }

    protected void disableDialerRunnable() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
            mHandler = null;
            mRunnable = null;
        }
    }

    public void resetSheathScreen() {
        if (mSheathScreenOn && !mPhoneCallActiveFlag  && mBackgroundSetFlag) {
            mFlinged = false;
            prepareSheathScreenAnimation(true);
            doSheathTextAnimation(-1);
        }
    }

    /**
     * ---------------------------------------------------------------------------------------------
     * INTERNAL CLASSES
     * ---------------------------------------------------------------------------------------------
     */
    protected class DialerRunnable implements Runnable {
        private int num; // the digit of the button pressed

        public DialerRunnable(int num) {
            this.num = num;
        }

        public void run() {
            //Log.d(TAG, "dialerrunnable running");
            SharedPreferences sharedPref = getSharedPreferences(
                    getString(R.string.speed_dial_preference_file_key),
                    Context.MODE_PRIVATE);

            if (num == -1) { //error handling
                Log.d(TAG, "Error in finding view id.");
                return;
            }

            mLongPressFlag = true;

            String telNum = sharedPref.getString(
                    getString(R.string.key_number_store_prefix_phone) + num, null);
            if (telNum == null) {
                Log.e(TAG, "Unable to make call because phone number invalid");
                enableErrorViewsinView(
                        getString(R.string.error_title_invalid_number),
                        getString(R.string.error_description_invalid_number),
                        R.drawable.ic_error_white_48dp);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disableErrorViewsInView();
                    }
                }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
                return;
            }

            // Check for roaming
            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            if (telephony.isNetworkRoaming()) {
                enableErrorViewsinView(
                        getString(R.string.error_title_roaming),
                        getString(R.string.error_description_roaming),
                        R.drawable.ic_signal_cellular_connected_no_internet_2_bar_white_48dp);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disableErrorViewsInView();
                    }
                }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            } else if (isAirplaneModeOn()) {
                enableErrorViewsinView(
                        getString(R.string.error_title_airplane),
                        getString(R.string.error_description_airplane),
                        R.drawable.ic_airplanemode_on_white_48dp);
                Handler h = new Handler();
                h.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disableErrorViewsInView();
                    }
                }, getResources().getInteger(R.integer.lock_screen_phone_error_display_length));
            } else if (!getPhoneCallActiveFlag() && isSpeedDialEnabled()) {  //Only want to initiate the call if line is idle and speed dial enabled
                mSpeedDialNumPressed = num;
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + (telNum.trim())));
                startActivity(intent);
                //stopService(new Intent(context, LockScreenService.class));   //This caused errors when unlocking the screen during the call

                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(350);
                enableLongPressFlag();

                // Now set a runnable for the case where a OFF_HOOK intent is not timely received
                if (mErrorHandler != null && mErrorRunnable != null
                        && mErrorRemoveRunnable != null) {
                    int errorDisplayDelay = getResources()
                            .getInteger(R.integer.lock_screen_phone_error_call_failed_delay);
                    int errorDisplayLength = getResources()
                            .getInteger(R.integer.lock_screen_phone_error_display_length);
                    mErrorHandler.postDelayed(mErrorRunnable, errorDisplayDelay);
                    mErrorHandler.postDelayed(
                            mErrorRemoveRunnable,
                            errorDisplayDelay + errorDisplayLength
                    );
                }
            }

        }
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String TAG = "MyGestureListener";

        @Override
        public boolean onDown(MotionEvent event) {
            //Log.d(TAG, "onDown is " + event.getY());
            return true;
        }

        /**
         * Ocassionally getting strange error where velocity is positive but should be negative.
         * No solution found, but could just register a swipe on abs of velocity, and determine
         * direction by the e1 and e2 parameters.  Would cause some unintuitive results with creative
         * finger play, but might be better result
         *
         * @param e1
         * @param e2
         * @param velocityX
         * @param velocityY
         * @return
         */
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               float velocityX, float velocityY) {
            int minDistance = getResources().getInteger(R.integer.swipe_min_distance);
            int thresholdVel = getResources().getInteger(R.integer.swipe_threshold_velocity);

            //Log.d(TAG, "VelX = " + velocityX + " VelY = " + velocityY + " e1 = " + e1.getRawY() + " e2 = " + e2.getRawY());

            // Going for a somewhat unartful algorithm just to get some more consistent results
            if (e1.getRawY() - e2.getRawY() > minDistance
                    && Math.abs(velocityY) > thresholdVel) {
                //Log.d(TAG, "Threshold reached, animating.");
                doSheathScreenAnimation(true);
                mFlinged = true;
            } else if (e2.getRawY() - e1.getRawY() > minDistance
                    && Math.abs(velocityY) > thresholdVel) {
                //Log.d(TAG, "Threshold return reached, animating return");
                doSheathScreenAnimation(false);
                mFlinged = true;
            }
            /*if (Math.abs(e1.getRawY() - e2.getRawY()) > minDistance
                    && -velocityY > thresholdVel) {
                Log.d(TAG, "Threshold reached, animating.");
                doSheathScreenAnimation(true);
                mFlinged = true;
            } else if (Math.abs(e1.getRawY() - e2.getRawY()) > minDistance
                    && velocityY > thresholdVel) {
                Log.d(TAG, "Threshold return reached, animating return");
                doSheathScreenAnimation(false);
                mFlinged = true;
            }*/

            /*if (velocityY > 0) {
                 (TAG, "velocityY error:" + velocityY);
            }*/
            return false;
        }
    }
}


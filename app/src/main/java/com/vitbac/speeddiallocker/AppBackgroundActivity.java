package com.vitbac.speeddiallocker;

import android.app.Activity;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.vitbac.speeddiallocker.helpers.BitmapToViewHelper;

import java.util.concurrent.atomic.AtomicInteger;


public class AppBackgroundActivity extends Activity
        implements View.OnClickListener, View.OnTouchListener {

    public static final String APP_PIC = "com.vitaminbacon.lockscreendialer.app_pic";
    public static final int RANDOM_PIC = -1;
    private static final String TAG = "AppBGActivity";
    private static final int IMAGE_RATIO = 6;

    private int mNumPics;
    private float lastX;

    private ViewFlipper mFlipper;
    private TextView mCounterView;
    private TypedArray mAppPics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_background);

        mFlipper = (ViewFlipper) findViewById(R.id.app_content_view_flipper);
        mFlipper.setOnClickListener(this);
        mFlipper.setOnTouchListener(this);
        findViewById(R.id.about_background_button).setOnClickListener(this);
        findViewById(R.id.random_background_button).setOnClickListener(this);
        mCounterView = (TextView) findViewById(R.id.app_content_view_flipper_counter);
        mAppPics = getResources().obtainTypedArray(R.array.app_pics);
        mNumPics = mAppPics.length();
        for (int i = 0; i < mNumPics; i++) {
            View child = getLayoutInflater().inflate(R.layout.child_app_content_flipper, null);
            //final Drawable drawable = mAppPics.getDrawable(i);
            int drawableResourceId = mAppPics.getResourceId(i, -1);
            //if (drawable != null) {
            if (drawableResourceId != -1) {
                child.setId(i+1);  // Should be OK so long as we do a findView through the parent; need to set POSITIVE number
                // just set an image in the initial two views
                if (i <= 1) {
                    //setImageViewWithDrawable((ImageView) child.findViewById(R.id.flipper_image), drawable);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setImageViewWithDrawable(
                                (ImageView) child.findViewById(R.id.flipper_image),
                                getDrawable(drawableResourceId)
                        );
                    } else {
                        setImageViewWithDrawable(
                                (ImageView) child.findViewById(R.id.flipper_image),
                                getResources().getDrawable(drawableResourceId)
                        );
                    }
                }
                mFlipper.addView(child);
            }
        }
        mCounterView.setText(setCounterViewText(1, mNumPics));
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("numPics", mNumPics);
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        super.onRestoreInstanceState(bundle);
        mNumPics = bundle.getInt("numPics");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_app_background, menu);
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

    @Override
    public void onDestroy() {
        // Clear imageviews
        /*int i = 1;
        View child;
        do {
            child = mFlipper.findViewById(i);
            if (child != null) {
                final ImageView iView = (ImageView) child.findViewById(R.id.flipper_image);
                if (iView.getDrawable() != null) {
                    ((BitmapDrawable) iView.getDrawable()).getBitmap().recycle();
                }
                iView.setImageBitmap(null);
            }
            i++;
        } while (child != null);*/

        mAppPics.recycle();
        super.onDestroy();
    }
    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed()");
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                return false;  // Unless we return false here, the ACTION_UP event won't register with onClick

            case MotionEvent.ACTION_UP:
                float x = event.getX();
                int minDist = getResources().getInteger(R.integer.background_swipe_minimum_dist);
                if (Math.abs(lastX - x) < minDist) {
                    // "false" allows event to be consumed by onClick
                    return false;
                }
                if (lastX < x) {
                    // left to right swipe
                    if (mFlipper.getDisplayedChild() == 0) {
                        // Means no more children this direction
                        break;
                    }
                    mFlipper.setInAnimation(this, R.anim.in_from_left);
                    mFlipper.setOutAnimation(this, R.anim.out_to_right);
                    // Display the next screen
                    loadFlipperDrawable(mFlipper.getDisplayedChild() - 2);
                    unloadFlipperDrawable(mFlipper.getDisplayedChild()+1);
                    mFlipper.showPrevious();
                } else if (lastX > x) {
                    // right to left swipe
                    if (mFlipper.getDisplayedChild() == (mNumPics - 1)) {
                        //Means no more children this direction
                        break;
                    }
                    mFlipper.setInAnimation(this, R.anim.in_from_right);
                    mFlipper.setOutAnimation(this, R.anim.out_to_left);
                    // Display the previous screen
                    loadFlipperDrawable(mFlipper.getDisplayedChild() + 2);
                    unloadFlipperDrawable(mFlipper.getDisplayedChild()-1);
                    mFlipper.showNext();
                }
                mCounterView.setText(setCounterViewText(mFlipper.getDisplayedChild() + 1, mNumPics));
                break;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick called");
        switch (view.getId()) {
            case R.id.app_content_view_flipper:
                Intent picSelectedIntent = new Intent();
                TypedArray appPics = getResources().obtainTypedArray(R.array.app_pics);
                int returnValue = appPics.getResourceId(mFlipper.getDisplayedChild(), 0);
                if (returnValue == 0) {
                    Log.d(TAG, "Could not obtain resource id of app picture");
                    setResult(RESULT_CANCELED, picSelectedIntent);
                } else {
                    picSelectedIntent.putExtra(APP_PIC, returnValue);
                    setResult(RESULT_OK, picSelectedIntent);
                }
                finish();
                break;

            case R.id.random_background_button:
                Intent randomPicIntent = new Intent();
                randomPicIntent.putExtra(APP_PIC, RANDOM_PIC);
                setResult(RESULT_OK, randomPicIntent);
                finish();
                break;

            case R.id.about_background_button:
                // TODO: create dialog fragment for simple author information
                break;
        }
    }

    private String setCounterViewText(int num, int total) {
        return num + " of " + total;
    }

    private void loadFlipperDrawable (int num) {
        if (num < 0 || num >= mNumPics) {
            return;
        }
        int id = num + 1;
        //TypedArray appPics = getResources().obtainTypedArray(R.array.app_pics);
        View child = mFlipper.findViewById(id);
        /*setImageViewWithDrawable(
                (ImageView) child.findViewById(R.id.flipper_image), mAppPics.getDrawable(num)
        );*/
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setImageViewWithDrawable(
                    (ImageView) child.findViewById(R.id.flipper_image),
                    getDrawable(mAppPics.getResourceId(num, -1))
            );
        } else {
            setImageViewWithDrawable(
                    (ImageView) child.findViewById(R.id.flipper_image),
                    getResources().getDrawable(mAppPics.getResourceId(num, -1))
            );
        }
        //BitmapToViewHelper.resizeBitmapToView(iView, ((BitmapDrawable)drawable).getBitmap());
        //iView.setImageDrawable(d);

    }

    private void unloadFlipperDrawable (int num) {
        if (num < 0 || num >= mNumPics) {
            return;
        }
        int id = num + 1;
        View child = mFlipper.findViewById(id);
        final ImageView iView = (ImageView) child.findViewById(R.id.flipper_image);
        iView.setImageBitmap(null);
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                    //iView.setImageDrawable(null);
                iView.setImageBitmap(null);
            }
        }, getResources().getInteger(R.integer.flipper_delay));*/
    }

    private void setImageViewWithDrawable(final ImageView view, final Drawable drawable) {
        view.post(new Runnable() {
            @Override
            public void run() {
                BitmapToViewHelper.resizeBitmapToView(
                        view,
                        ((BitmapDrawable) drawable).getBitmap(),
                        0,
                        getDisplayWidth() / IMAGE_RATIO,
                        getDisplayHeight() / IMAGE_RATIO
                );
            }
        });
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

}

package com.vitbac.speeddiallocker.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

/**
 * Created by nick on 7/23/15.
 */
public class CropImageView extends ImageView {

    private RectF mCrop;
    private float mCropW, mCropH;
    private Paint mPaint;
    private boolean mIsPortraitCrop;
    private int mIntendedDisplayHeight, mIntendedDisplayWidth;

    private static final int DEFAULT_COLOR = Color.GREEN;
    private static final float DEFAULT_STROKE_WIDTH = 3f;
    private static final String TAG = "CropImageView";


    public CropImageView (Context context) {
        super(context);
    }

    public CropImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw (Canvas canvas) {
        super.onDraw(canvas);

        if (mCrop != null && mPaint != null) {
            //Log.d(TAG, "DRAWING");
            canvas.drawRect(mCrop, mPaint);
        }
    }

    @Override
    public boolean onTouchEvent (MotionEvent event) {
        //Log.d(TAG, "onTouchEvent");
        if (super.onTouchEvent(event)) {
            return true;
        }
        if (mCrop == null) {
            return false;
        }

        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                if (mIsPortraitCrop) {
                    // Portrait picture, so only track horizontal movement
                    float left = event.getX() - mCropW/2;
                    float right = event.getX() + mCropW/2;
                    if (left < 0) {
                        left = 0;
                        right = mCropW;
                    } else if (right > getWidth()) {
                        left = getWidth() - mCropW;
                        right = getWidth();
                    }
                    mCrop.set(
                            left,
                            mCrop.top,
                            right,
                            mCrop.bottom);
                    //Log.d(TAG, "Coord=(" +mCrop.left+", "+mCrop.top+", "+mCrop.right+", "+mCrop.bottom+")");
                } else {
                    float top = event.getY() - mCropH/2;
                    float bottom = event.getY() + mCropH/2;
                    if (top < 0) {
                        top = 0;
                        bottom = getHeight();
                    } else if (bottom > getHeight()) {
                        top = getHeight() - mCropH;
                        bottom = getHeight();
                    }
                    mCrop.set(
                            mCrop.left,
                            top,
                            mCrop.right,
                            bottom);
                }
                invalidate();
                return true;
        }
        return false;
    }

    public void setBitmapWithCrop (Bitmap bitmap, final int displayWidth, final int displayHeight,
                                   final Paint paint){
        //Log.d(TAG, "Setting image bitmap");
        setImageBitmap(bitmap);

        // Need to put in post so that numbers are retrieved when view is resized.
        post(new Runnable() {
            public void run() {
                if (paint == null) {
                    // Set default paint settings
                    mPaint = new Paint();
                    mPaint.setColor(DEFAULT_COLOR);
                    mPaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
                    mPaint.setStrokeCap(Paint.Cap.ROUND);
                    mPaint.setStyle(Paint.Style.STROKE);
                } else {
                    mPaint = paint;
                }

                // Shrink the larger dimension to meet the view's size
                if (displayWidth > displayHeight) {
                    mCropW = getWidth();
                    mCropH = displayHeight * (((float) getWidth()) / displayWidth);
                    mIsPortraitCrop = false;

                } else {
                    mCropH = getHeight();
                    mCropW = displayWidth * (((float) getHeight()) / displayHeight);
                    mIsPortraitCrop = true;
                }

                // Set the rect's coordinates based in the center
                mCrop = new RectF(
                        getWidth() / 2 - mCropW / 2, // left
                        getHeight() / 2 - mCropH / 2, // top
                        getWidth() / 2 + mCropW / 2, // right
                        getHeight() / 2 + mCropH / 2); // bottom
                invalidate();
            }
        });
        mIntendedDisplayHeight = displayHeight;
        mIntendedDisplayWidth = displayWidth;
    }

    public void setBitmapWithCrop (Bitmap bitmap, int displayWidth, int displayHeight) {
        setBitmapWithCrop(bitmap, displayWidth, displayHeight, null);
    }

    /**
     * Returns the crop rect based on the original display size sent to the view
     * @return
     */
    public RectF getCrop () {
        return mCrop;
    }

    /**
     * Returns a RectF scaled to a view with the passed dimension parameters.
     * DEPRECATED
     * @param scaledW
     * @param scaledH
     * @return
     * @throws IllegalArgumentException
     */
    public RectF getScaledCrop (int scaledW, int scaledH) throws IllegalArgumentException{
        // Check if the input scales correctly
        if (((float)scaledW)/scaledH != mCrop.width()/mCrop.height()) {
            throw new IllegalArgumentException("Scaled width/height of " + scaledW + "/" + scaledH
            + " not properly scaled to CropImageView with width/height of "
            + getWidth() + "/" + getHeight());
        }
        return new RectF(
                mCrop.left * ((float) scaledW)/ mCrop.width(),
                mCrop.top * ((float) scaledH)/mCrop.height(),
                mCrop.right * ((float) scaledW)/mCrop.width(),
                mCrop.bottom * ((float) scaledH)/mCrop.height()
        );
    }

    /**
     * Returns a scaled crop based on the initial display values passed to the View
     * @return
     * @throws IllegalArgumentException
     */
    public RectF getScaledCrop () throws IllegalArgumentException{
        Log.d(TAG, "getScaledCrop with intended w/h=" + mIntendedDisplayWidth + "/" + mIntendedDisplayHeight);
        return getScaledCrop(mIntendedDisplayWidth, mIntendedDisplayHeight);
    }


}

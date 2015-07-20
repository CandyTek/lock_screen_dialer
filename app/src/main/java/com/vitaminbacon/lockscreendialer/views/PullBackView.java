package com.vitaminbacon.lockscreendialer.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.vitaminbacon.lockscreendialer.R;

/**
 * Created by nick on 7/16/15.
 */
public class PullBackView extends View {

    private static int DEFAULT_PULLBACK_MAX_PERCENT = 10;
    private Paint mPaint;
    private RectF mRectF;
    private float mStartAngle;
    private float mPullbackMaxPercent;
    private float mTouchStartPos = 0;
    private boolean mIsPullBackActivated = false;

    public PullBackView(Context context) {
        super(context);

    }

    public PullBackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullBackView, 0, 0);
        mPullbackMaxPercent = (float) (0.01 * a.getInt(
                R.styleable.PullBackView_pullbackPercentOfViewMax,
                DEFAULT_PULLBACK_MAX_PERCENT));
    }

    public PullBackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullBackView, 0, 0);
        mPullbackMaxPercent = (float) (0.01 * a.getInt(
                R.styleable.PullBackView_pullbackPercentOfViewMax,
                DEFAULT_PULLBACK_MAX_PERCENT));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mIsPullBackActivated && mRectF != null) {
            canvas.drawArc(mRectF, mStartAngle, 180, true, mPaint);
        }
    }

    public void paintPullBackAtTop(Paint p, float touchCenter, float touchPos) {
        float touchDelta = (touchPos - mTouchStartPos) / 2;
        p.setAlpha(adjustAlphaIfOverThreshold(p.getAlpha(), getHeight(), touchDelta));
        float arcHeight = adjustArcHeightIfOverThreshold(getHeight(), touchDelta);
        mPaint = p;
        mStartAngle = 0;
        float delta = touchCenter - getWidth() / 2;

        // This method prevent the left or right edge of the oval being in the view
        if (touchCenter < getWidth() / 2) {
            mRectF = new RectF(
                    delta - arcHeight,
                    -arcHeight,
                    getWidth() + arcHeight,
                    arcHeight);
        } else {
            mRectF = new RectF(
                    -arcHeight,
                    -arcHeight,
                    delta + getWidth() + arcHeight,
                    arcHeight);
        }

        invalidate();
    }

    public void paintPullBackAtBottom(Paint p, float arcCenter, float touchPos) {
        float touchDelta = (touchPos - mTouchStartPos) / 2;
        p.setAlpha(adjustAlphaIfOverThreshold(p.getAlpha(), getHeight(), touchDelta));
        float arcHeight = adjustArcHeightIfOverThreshold(getHeight(), touchDelta);
        mPaint = p;
        mStartAngle = 180;
        float delta = arcCenter - getWidth() / 2;
        mRectF = new RectF(
                delta + getWidth() - arcHeight,
                getHeight() - arcHeight,
                delta + getWidth() + arcHeight,
                getHeight() + arcHeight);

        invalidate();
    }

    public void paintPullBackAtLeft(Paint p, float arcCenter, float touchPos) {
        float touchDelta = (touchPos - mTouchStartPos) / 2;
        p.setAlpha(adjustAlphaIfOverThreshold(p.getAlpha(), getWidth(), touchDelta));
        float arcHeight = adjustArcHeightIfOverThreshold(getHeight(), touchDelta);
        mPaint = p;
        mStartAngle = 270;
        float delta = arcCenter - getHeight() / 2;
        mRectF = new RectF(
                -arcHeight,
                delta - arcHeight,
                arcHeight,
                delta + arcHeight + getHeight());

        invalidate();
    }

    public void paintPullBackAtRight(Paint p, float arcCenter, float touchPos) {
        float touchDelta = (touchPos - mTouchStartPos) / 2;
        p.setAlpha(adjustAlphaIfOverThreshold(p.getAlpha(), getWidth(), touchDelta));
        float arcHeight = adjustArcHeightIfOverThreshold(getHeight(), touchDelta);
        mPaint = p;
        mStartAngle = 90;
        float delta = arcCenter - getHeight() / 2;
        mRectF = new RectF(
                getWidth() - arcHeight,
                delta - arcHeight,
                getWidth() + delta + arcHeight,
                delta + getHeight() + arcHeight);

        invalidate();
    }

    public void setTouchStartPos(float pos) {
        mTouchStartPos = pos;
        mIsPullBackActivated = true;
    }

    public void deactivate() {
        animate()
                .alpha(0f)
                .setDuration(getResources().getInteger(android.R.integer.config_mediumAnimTime))
                .setListener(new AnimatorListenerAdapter() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mIsPullBackActivated = false;
                        mTouchStartPos = 0;
                        mRectF = null;
                        setAlpha(1f);
                        invalidate();
                    }
                });
    }

    public boolean isPullBackActivated() {
        return mIsPullBackActivated;
    }

    /**
     * Returns an alpha value adjusted to how far the arc is going to be drawn in relation to the
     * view's tolerance for the size of the arc.  If over the tolerance, the alpha is darkened
     * instead of the arc size growing.  If it is not over the tolerance, the alpha remains
     * the same.
     *
     * @param alpha
     * @param viewDimenSize
     * @param arcHeight
     * @return
     */
    private int adjustAlphaIfOverThreshold(int alpha, float viewDimenSize, float arcHeight) {
        float threshold = viewDimenSize * mPullbackMaxPercent;
        if (arcHeight > threshold) {
            int alphaDelta = 255 - alpha;
            return (int) (alpha + alphaDelta * (arcHeight - threshold) / (viewDimenSize - threshold));
        }
        return alpha;
    }

    /**
     * Should only be called after adjustAlphaIfOverThreshold
     *
     * @param viewDimenSize
     * @param arcHeight
     * @return
     */
    private float adjustArcHeightIfOverThreshold(float viewDimenSize, float arcHeight) {
        if (arcHeight > viewDimenSize * mPullbackMaxPercent) {
            return viewDimenSize * mPullbackMaxPercent;
        } else {
            return arcHeight;
        }
    }

}

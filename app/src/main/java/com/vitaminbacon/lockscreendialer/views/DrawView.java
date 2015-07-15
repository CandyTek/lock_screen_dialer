package com.vitaminbacon.lockscreendialer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by nick on 6/23/15.
 */
public class DrawView extends View {

    private String TAG = "DrawView";
    private Set<Line> lines;


    public DrawView(Context context) {
        super(context);
        lines = new HashSet<Line>();
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        lines = new HashSet<Line>();
    }

    public DrawView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        lines = new HashSet<Line>();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Iterator<? extends Line> i = lines.iterator();
        while (i.hasNext()) {
            Line next = i.next();
            if (next instanceof StraightLine) {
                StraightLine l = (StraightLine) next;
                canvas.drawLine(l.startX, l.startY, l.endX, l.endY, l.paint);
            } else if (next instanceof Arc && !(next instanceof RotatedArc)) {
                Arc a = (Arc) next;
                canvas.drawArc(a.oval, a.startAngle, a.sweepAngle, a.useCenter, a.paint);
                //Log.d(TAG, "startAngle = " + a.startAngle + " sweepAngle = " +a.sweepAngle);
            } else {
                RotatedArc ra = (RotatedArc) next;
                Log.d(TAG, "startAngle = " + ra.startAngle + " sweepAngle = " + ra.sweepAngle + " rotation = " + ra.rotation + " rectF = " + ra.oval.toString());
                canvas.save();
                canvas.rotate(ra.rotation, getX() + getWidth() / 2, getY() + getHeight() / 2);
                canvas.drawArc(ra.oval, ra.startAngle + -ra.rotation,
                        ra.sweepAngle, ra.useCenter, ra.paint);
                canvas.restore();
            }
        }
    }

    public void addLine(float startX, float startY, float endX, float endY, Paint paint) {
        lines.add(new StraightLine(startX, startY, endX, endY, paint));
    }

    public void addLineWithAbsoluteCoords(float startX, float startY,
                                          float endX, float endY, Paint paint) {
        int[] myCoords = new int[2];
        getLocationOnScreen(myCoords);
        addLine(startX - myCoords[0], startY - myCoords[1],
                endX - myCoords[0], endY - myCoords[1], paint);
    }

    public void addArc(float left, float top, float right, float bottom,
                       float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        RectF rectF = new RectF(left, top, right, bottom);
        lines.add(new Arc(rectF, startAngle, sweepAngle, useCenter, paint));
    }

    public void addArcWithAbsoluteCoords(float left, float top, float right, float bottom,
                                         float startAngle, float sweepAngle, boolean useCenter,
                                         Paint paint) {
        int[] myCoords = new int[2];
        getLocationOnScreen(myCoords);
        addArc(left - myCoords[0], top - myCoords[1], right - myCoords[0], bottom - myCoords[1],
                startAngle, sweepAngle, useCenter, paint);
    }

    public void addRotatedArc(float left, float top, float right, float bottom, float rotation,
                              float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
        RectF rectF = new RectF(left, top, right, bottom);
        lines.add(new RotatedArc(rectF, rotation, startAngle, sweepAngle, useCenter, paint));
    }

    public void addRotatedArcWithAbsoluteCoords(float left, float top, float right, float bottom,
                                                float rotation, float startAngle, float sweepAngle,
                                                boolean useCenter, Paint paint) {
        int[] myCoords = new int[2];
        getLocationOnScreen(myCoords);
        addRotatedArc(left - myCoords[0], top - myCoords[1], right - myCoords[0], bottom - myCoords[1],
                rotation, startAngle, sweepAngle, useCenter, paint);
    }


    public void clearLines() {
        lines.clear();
        //invalidate();
    }

    private abstract class Line {
        public Paint paint;
    }

    private class StraightLine extends Line {
        public float startX, startY, endX, endY;

        public StraightLine(float startX, float startY, float endX, float endY, Paint paint) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
        }
    }

    private class Arc extends Line {
        public RectF oval;
        public float startAngle, sweepAngle;
        public boolean useCenter;

        public Arc(RectF oval, float startAngle, float sweepAngle, boolean useCenter, Paint paint) {
            this.oval = oval;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.useCenter = useCenter;
            this.paint = paint;
        }
    }

    private class RotatedArc extends Arc {
        public float rotation;

        public RotatedArc(RectF oval, float rotation, float startAngle,
                          float sweepAngle, boolean useCenter, Paint paint) {
            super(oval, startAngle, sweepAngle, useCenter, paint);
            this.rotation = rotation;
        }
    }

}

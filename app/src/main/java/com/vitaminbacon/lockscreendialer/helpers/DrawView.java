package com.vitaminbacon.lockscreendialer.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by nick on 6/23/15.
 */
public class DrawView extends View {

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
        Iterator<Line> i = lines.iterator();
        while (i.hasNext()) {
            Line l = i.next();
            canvas.drawLine(l.startX, l.startY, l.endX, l.endY, l.paint);
        }
    }

    public void addLine(float startX, float startY, float endX, float endY, Paint paint) {
        lines.add(new Line(startX, startY, endX, endY, paint));
    }

    public void addLineWithAbsoluteCoords(float startX, float startY,
                                          float endX, float endY, Paint paint) {
        int[] myCoords = new int[2];
        getLocationOnScreen(myCoords);
        addLine(startX - myCoords[0], startY - myCoords[1],
                endX - myCoords[0], endY - myCoords[1], paint);
    }

    public void clearLines() {
        lines.clear();
    }

    private class Line {
        public float startX, startY, endX, endY;
        public Paint paint;

        public Line(float startX, float startY, float endX, float endY, Paint paint) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
        }
    }

}

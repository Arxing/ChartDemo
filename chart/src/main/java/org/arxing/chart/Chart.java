package org.arxing.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.arxing.utils.Logger;

import java.util.ArrayList;
import java.util.List;

public class Chart extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Logger logger = new Logger("幹");
    private SurfaceHolder holder;
    private List<Point> points = new ArrayList<>();
    private PointF touchPoint = new PointF();
    private boolean drawing;
    private Canvas canvas;
    private GestureDetector gestureDetector;
    private Paint pTouchPoint = new Paint();

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);

        holder = getHolder();
        holder.addCallback(this);
        gestureDetector = new GestureDetector(getContext(), gestureListener);
        setClickable(true);
        setFocusable(true);
        initPaint();
    }

    private void initPaint() {
        pTouchPoint.setColor(Color.WHITE);
        pTouchPoint.setStrokeWidth(10);
        pTouchPoint.setStyle(Paint.Style.FILL);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        logger.e("建啦");
        drawing = true;
        new Thread(this).start();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        logger.e("沒了");
        drawing = false;
    }

    @Override public void run() {
        while (drawing) {
            drawFrame();
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private void drawFrame() {
        canvas = holder.lockCanvas();
        canvas.drawColor(Color.BLACK);
        canvas.drawCircle(touchPoint.x, touchPoint.y, 100, pTouchPoint);
        logger.e("draw %s", touchPoint.toString());
        holder.unlockCanvasAndPost(canvas);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override public boolean onDown(MotionEvent e) {
            touchPoint.set(e.getX(), e.getY());
            logger.e("???? %f %f", e.getX(), e.getY());
            return true;
        }
    };
}

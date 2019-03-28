package org.arxing.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.arxing.utils.Logger;
import org.arxing.utils.UnitParser;

import java.util.ArrayList;
import java.util.List;

public class Chart extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private Logger logger = new Logger("幹");
    private SurfaceHolder holder;
    private int screenWidth;
    private int screenHeight;
    private List<Point> points = new ArrayList<>();
    private PointF touchPoint = new PointF(-100, -100);
    private boolean drawing;
    private Canvas canvas;
    private GestureDetector gestureDetector;
    private Paint pTouchPoint = new Paint();
    private Paint pTouchGuide = new Paint();
    private Paint pFrame = new Paint();
    private TextPaint tpXDescription = new TextPaint();
    private Rect rectXDescription = new Rect();
    private int rowCount = 10;
    private int columnCount = 5;

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

        pTouchGuide.setColor(Color.YELLOW);
        pTouchGuide.setStrokeWidth(3);

        pFrame.setColor(Color.DKGRAY);
        pFrame.setStrokeWidth(1.5f);
        pFrame.setPathEffect(new DashPathEffect(new float[]{15, 5}, 0));

        tpXDescription.setTextSize(UnitParser.sp2px(getContext(), 30));
        tpXDescription.setColor(Color.MAGENTA);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        drawing = true;
        new Thread(this).start();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = getWidth();
        screenHeight = getHeight();
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        drawing = false;
    }

    @Override public void run() {
        while (drawing) {
            canvas = holder.lockCanvas(null);
            canvas.drawColor(Color.BLACK);
            drawFrame();
            drawPoint();
            drawDescription();
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            touchPoint.set(-100, -100);
            return true;
        } else
            return gestureDetector.onTouchEvent(event);
    }

    private void drawFrame() {
        int yStep = screenHeight / rowCount;
        for (int i = 1; i <= rowCount - 1; i++) {
            int y = yStep * i;
            canvas.drawLine(0, y, screenWidth, y, pFrame);
        }
        int xStep = screenWidth / columnCount;
        for (int i = 1; i <= columnCount - 1; i++) {
            int x = xStep * i;
            canvas.drawLine(x, 0, x, screenHeight, pFrame);
        }
    }

    private void drawPoint() {
        canvas.drawCircle(touchPoint.x, touchPoint.y, 10, pTouchPoint);
        pTouchGuide.setColor(Color.YELLOW);
        canvas.drawLine(0, touchPoint.y, screenWidth, touchPoint.y, pTouchGuide);
        pTouchGuide.setColor(Color.GREEN);
        canvas.drawLine(touchPoint.x, 0, touchPoint.x, screenHeight, pTouchGuide);
    }

    private void drawDescription() {
        String xShow = String.format("時間%.2f", touchPoint.x);
        tpXDescription.getTextBounds(xShow, 0, xShow.length(), rectXDescription);
        float xShowX = (touchPoint.x >= (screenWidth / 2)) ? touchPoint.x - rectXDescription.width() : touchPoint.x;
        canvas.drawText(xShow, xShowX, screenHeight, tpXDescription);

    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override public boolean onDown(MotionEvent e) {
            touchPoint.set(e.getX(), e.getY());
            return true;
        }

        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float x = e2.getX();
            float y = e2.getY();
            x = (x > screenWidth) ? screenWidth : (x < 0) ? 0 : x;
            y = (y > screenHeight) ? screenHeight : (y < 0) ? 0 : y;
            touchPoint.set(x, y);
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };
}

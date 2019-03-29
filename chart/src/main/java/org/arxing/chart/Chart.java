package org.arxing.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import org.arxing.utils.Logger;
import org.arxing.utils.UnitParser;

import java.util.List;

public class Chart extends SurfaceView implements SurfaceHolder.Callback, Handler.Callback {
    private Logger logger = new Logger("幹");
    private SurfaceHolder holder;
    private int screenWidth;
    private int screenHeight;
    private Canvas canvas;
    private GestureDetector gestureDetector;
    private DataSet dataSet;
    private Path pathData = new Path();
    private Path pathDataFill = new Path();
    private HandlerThread thread;
    private Handler handler;
    final static int OUTSIDE_XY = -1000;

    //paint
    private TextPaint tpXDescription = new TextPaint();
    private Paint pFrame = new Paint();
    private Paint pTargetPoint = new Paint();
    private Paint pTouchGuide = new Paint();
    private Paint pData = new Paint();
    private Paint pDataFill = new Paint();
    private Paint pClean = new Paint();

    //rect
    private Rect rectXDescription = new Rect();

    //point
    private PointF touchPoint = new PointF(OUTSIDE_XY, OUTSIDE_XY);

    private Properties properties;

    class Properties {

        int rows;
        int columns;

        {
            rows = 7;
            columns = 3;
        }
    }

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);

        gestureDetector = new GestureDetector(getContext(), gestureListener);
        properties = new Properties();
        setClickable(true);
        setFocusable(true);
        initPaint();
        holder = getHolder();
        holder.addCallback(this);
    }

    private void initPaint() {
        pTargetPoint.setColor(Color.RED);
        pTargetPoint.setStrokeWidth(10);
        pTargetPoint.setStyle(Paint.Style.FILL);
        pTargetPoint.setAntiAlias(true);

        pTouchGuide.setColor(Color.YELLOW);
        pTouchGuide.setStrokeWidth(1.5f);
        pTouchGuide.setAntiAlias(true);

        tpXDescription.setTextSize(UnitParser.sp2px(getContext(), 30));
        tpXDescription.setColor(Color.MAGENTA);
        tpXDescription.setAntiAlias(true);

        pFrame.setColor(Color.DKGRAY);
        pFrame.setStrokeWidth(1.5f);
        pFrame.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
        pFrame.setAntiAlias(true);

        pData.setStyle(Paint.Style.STROKE);
        pData.setStrokeWidth(3f);
        pData.setColor(Color.parseColor("#67abb9"));
        pData.setAntiAlias(true);

        pDataFill.setStyle(Paint.Style.FILL);
        pDataFill.setColor(Color.parseColor("#3067abb9"));
        pDataFill.setAntiAlias(true);
    }

    @Override public boolean handleMessage(Message msg) {
        canvas = holder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT);
        pClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(pClean);
        pClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        canvas.drawColor(Color.BLACK);
        drawFrame(properties.rows, properties.columns);
        if (dataSet != null) {
            drawData(dataSet.toPoints());
            drawPoint();
        }
        holder.unlockCanvasAndPost(canvas);
        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        updateDataSetSize();

        thread = new HandlerThread("name");
        thread.start();
        handler = new Handler(thread.getLooper(), this);
        refresh();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = getWidth();
        screenHeight = getHeight();
        updateDataSetSize();
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        thread.quit();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            touchPoint.set(OUTSIDE_XY, OUTSIDE_XY);
            refresh();
            return true;
        } else
            return gestureDetector.onTouchEvent(event);
    }

    private void refresh() {
        handler.removeMessages(100);
        handler.sendEmptyMessage(100);
    }

    private void drawFrame(int rows, int columns) {
        int xStep = screenWidth / (columns + 1);
        int yStep = screenHeight / (rows + 1);
        for (int i = 1; i < columns + 1; i++) {
            int x = xStep * i;
            canvas.drawLine(x, 0, x, screenHeight, pFrame);
        }
        for (int i = 1; i < rows + 1; i++) {
            int y = yStep * i;
            canvas.drawLine(0, y, screenWidth, y, pFrame);
        }
    }

    private void drawPoint() {
        float x = touchPoint.x;
        float y = touchPoint.y;
        pTouchGuide.setColor(Color.GREEN);
        canvas.drawLine(x, 0, x, screenHeight, pTouchGuide);
        pTouchGuide.setColor(Color.YELLOW);
        if (dataSet.isMatched(x)) {
            DataSet.Box box = dataSet.findMatched(x);
            dataSet.notifyMatched(box.data);
            PointF target = dataSet.findMatchedPoint(x);
            canvas.drawLine(0, target.y, screenWidth, target.y, pTouchGuide);
            canvas.drawCircle(target.x, target.y, 10, pTargetPoint);
        } else {
            canvas.drawLine(0, y, screenWidth, y, pTouchGuide);
        }
    }

    private void drawData(List<PointF> points) {
        pathData.moveTo(0, screenHeight);
        if (points.size() > 0) {
            PointF first = points.get(0);
            pathData.lineTo(first.x, first.y);
            for (PointF point : points) {
                float x = point.x;
                float y = point.y;
                pathData.lineTo(x, y);
            }
        }
        pathData.lineTo(screenWidth, screenHeight);
        pathData.lineTo(0, screenHeight);
        pathDataFill.set(pathData);
        canvas.drawPath(pathDataFill, pDataFill);
        canvas.drawPath(pathData, pData);
        pathDataFill.reset();
        pathData.reset();
    }

    private void updateDataSetSize() {
        if (dataSet != null) {
            dataSet.updatePoints(screenWidth, screenHeight);
        }
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

        @Override public boolean onDown(MotionEvent e) {
            touchPoint.set((int) e.getX(), (int) e.getY());
            refresh();
            return true;
        }

        @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int x = (int) e2.getX();
            int y = (int) e2.getY();
            x = (x > screenWidth) ? screenWidth : (x < 0) ? 0 : x;
            y = (y > screenHeight) ? screenHeight : (y < 0) ? 0 : y;
            touchPoint.set(x, y);
            refresh();
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    };


}

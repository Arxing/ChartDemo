package org.arxing.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
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
    private Rect rectLines = new Rect();

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
        pTargetPoint.setColor(Color.parseColor("#ae67abb9"));
        pTargetPoint.setStrokeWidth(10);
        pTargetPoint.setStyle(Paint.Style.FILL);
        pTargetPoint.setAntiAlias(true);

        pTouchGuide.setColor(Color.YELLOW);
        pTouchGuide.setStrokeWidth(1.5f);
        pTouchGuide.setAntiAlias(true);
        pTouchGuide.setColor(Color.parseColor("#a0ffff00"));

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
        canvas = holder.lockCanvas(rectLines);
        canvas.drawColor(Color.TRANSPARENT);
        pClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawPaint(pClean);
        pClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        int width = rectLines.width();
        int height = rectLines.height();

        canvas.drawColor(Color.BLACK);
        drawFrame(properties.rows, properties.columns, width, height);
        if (dataSet != null) {
            drawData(dataSet.toPoints(), width, height);
            drawPoint(width, height);
        }
        holder.unlockCanvasAndPost(canvas);
        return true;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        updateScreenSize(getWidth(), getHeight());

        thread = new HandlerThread("name");
        thread.start();
        handler = new Handler(thread.getLooper(), this);
        refresh();
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateScreenSize(getWidth(), getHeight());
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        thread.quit();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchPoint.set(x, y);
                refresh();
                return true;
            case MotionEvent.ACTION_MOVE:
                x = (x > screenWidth) ? screenWidth : (x < 0) ? 0 : x;
                y = (y > screenHeight) ? screenHeight : (y < 0) ? 0 : y;
                touchPoint.set(x, y);
                refresh();
                return true;
            case MotionEvent.ACTION_UP:
                touchPoint.set(OUTSIDE_XY, OUTSIDE_XY);
                refresh();
                return true;
            default:
                return gestureDetector.onTouchEvent(event);
        }
    }

    private void refresh() {
        handler.removeMessages(100);
        handler.sendEmptyMessage(100);
    }

    private void drawFrame(int rows, int columns, int width, int height) {
        int xStep = width / (columns + 1);
        int yStep = height / (rows + 1);
        for (int i = 1; i < columns + 1; i++) {
            int x = xStep * i;
            canvas.drawLine(x, 0, x, height, pFrame);
        }
        for (int i = 1; i < rows + 1; i++) {
            int y = yStep * i;
            canvas.drawLine(0, y, width, y, pFrame);
        }
    }

    private void drawPoint(int width, int height) {
        float x = touchPoint.x;
        float y = touchPoint.y;
        canvas.drawLine(x, 0, x, height, pTouchGuide);
        if (dataSet.isMatched(x)) {
            DataSet.Box box = dataSet.findMatched(x);
            dataSet.notifyMatched(box.data);
            PointF target = dataSet.findMatchedPoint(x);
            canvas.drawLine(0, target.y, width, target.y, pTouchGuide);
            canvas.drawCircle(target.x, target.y, 10, pTargetPoint);
        } else {
            canvas.drawLine(0, y, width, y, pTouchGuide);
        }
    }

    private void drawData(List<PointF> points, int width, int height) {
        if (points.size() > 0) {
            PointF first = points.get(0);
            pathData.moveTo(first.x, first.y);
            for (PointF point : points) {
                float x = point.x;
                float y = point.y;
                pathData.lineTo(x, y);
            }
        }
        pathDataFill.moveTo(0, height);
        pathDataFill.addPath(pathData);
        pathDataFill.lineTo(width, height);
        pathDataFill.lineTo(0, height);

        canvas.drawPath(pathDataFill, pDataFill);
        canvas.drawPath(pathData, pData);
        pathDataFill.reset();
        pathData.reset();
    }

    private void updateScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        rectLines.set(0, 0, screenWidth - 100, screenHeight);
        if (dataSet != null) {
            dataSet.updatePoints(rectLines.width(), rectLines.height());
        }
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

    };
}

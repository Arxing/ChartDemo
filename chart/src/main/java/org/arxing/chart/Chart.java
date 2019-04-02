package org.arxing.chart;

import android.content.Context;
import android.content.res.TypedArray;
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
import android.support.annotation.ColorInt;
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
    private int offsetX;
    private int offsetY = UnitParser.dp2px(getContext(), 20);

    //paint
    private TextPaint tpDescription = new TextPaint();
    private Paint pFrameDash = new Paint();
    private Paint pFrame = new Paint();
    private Paint pGiudePoint = new Paint();
    private Paint pGuideLine = new Paint();
    private Paint pData = new Paint();
    private Paint pDataFill = new Paint();
    private Paint pClean = new Paint();
    private Paint pTargetBg = new Paint();

    //rect
    private Rect rectXDescription = new Rect();
    private Rect rectChart = new Rect();
    private Rect rectTargetXBg = new Rect();
    private Rect rectTargetXText = new Rect();
    private Rect rectTargetYBg = new Rect();
    private Rect rectTargetYText = new Rect();

    //point
    private PointF touchPoint = new PointF(OUTSIDE_XY, OUTSIDE_XY);

    // properties
    private @ColorInt int frameDescriptionColor = Color.parseColor("#222222");
    private @ColorInt int lineColor = Color.parseColor("#67abb9");
    private @ColorInt int lineBgColor = Color.parseColor("#3067abb9");
    private @ColorInt int targetBgColor = Color.parseColor("#54b8e4");
    private @ColorInt int targetTextColor = Color.parseColor("#eeeeee");
    private @ColorInt int guideLineColor = Color.parseColor("#67abb9");
    private @ColorInt int guidePointColor = Color.parseColor("#ae67abb9");
    private int rows = 5;
    private int columns = 3;
    private String xTimePattern = "M/d HH:mm";
    private String yValPattern = "%.2f";
    private float scaleY = 1.0f;

    public Chart(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(attrs);
        gestureDetector = new GestureDetector(getContext(), gestureListener);
        setClickable(true);
        setFocusable(true);
        initPaint();
        holder = getHolder();
        holder.addCallback(this);
    }

    private void initAttr(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Chart);
        lineColor = a.getColor(R.styleable.Chart_chart_line_color, lineColor);
        lineBgColor = a.getColor(R.styleable.Chart_chart_line_bg_color, lineBgColor);
        targetBgColor = a.getColor(R.styleable.Chart_chart_target_bg_color, targetBgColor);
        targetTextColor = a.getColor(R.styleable.Chart_chart_target_text_color, targetTextColor);
        guideLineColor = a.getColor(R.styleable.Chart_chart_guide_line_color, guideLineColor);
        guidePointColor = a.getColor(R.styleable.Chart_chart_guide_point_color, guidePointColor);
        rows = a.getInt(R.styleable.Chart_chart_rows, rows);
        columns = a.getInt(R.styleable.Chart_chart_columns, columns);
        scaleY = a.getFloat(R.styleable.Chart_chart_scale_y, scaleY);
        if (a.hasValue(R.styleable.Chart_chart_x_time_pattern))
            xTimePattern = a.getString(R.styleable.Chart_chart_x_time_pattern);
        if (a.hasValue(R.styleable.Chart_chart_y_val_pattern))
            yValPattern = a.getString(R.styleable.Chart_chart_y_val_pattern);
        a.recycle();
    }

    private void initPaint() {
        pGiudePoint.setColor(guidePointColor);
        pGiudePoint.setStrokeWidth(10);
        pGiudePoint.setStyle(Paint.Style.FILL);
        pGiudePoint.setAntiAlias(true);

        pGuideLine.setColor(Color.YELLOW);
        pGuideLine.setStrokeWidth(1.5f);
        pGuideLine.setAntiAlias(true);
        pGuideLine.setColor(guideLineColor);

        tpDescription.setTextSize(UnitParser.sp2px(getContext(), 14));
        tpDescription.setColor(frameDescriptionColor);
        tpDescription.setAntiAlias(true);

        pFrameDash.setColor(Color.parseColor("#60888888"));
        pFrameDash.setStrokeWidth(1.2f);
        pFrameDash.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
        pFrameDash.setAntiAlias(true);

        pFrame.setColor(frameDescriptionColor);
        pFrame.setStrokeWidth(3f);
        pFrame.setAntiAlias(true);

        pData.setStyle(Paint.Style.STROKE);
        pData.setStrokeWidth(3f);
        pData.setColor(lineColor);
        pData.setAntiAlias(true);

        pDataFill.setStyle(Paint.Style.FILL);
        pDataFill.setColor(lineBgColor);
        pDataFill.setAntiAlias(true);

        pTargetBg.setColor(targetBgColor);
        pTargetBg.setAntiAlias(true);
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
        refresh();
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) {
        thread.quit();
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        int l = rectChart.left;
        int t = rectChart.top;
        int r = rectChart.right;
        int b = rectChart.bottom;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rectChart.contains((int) x, (int) y)) {
                    touchPoint.set(x, y);
                    refresh();
                    return true;
                }
                return false;
            case MotionEvent.ACTION_MOVE:
                x = (x > r) ? r : (x < 0) ? 0 : x;
                y = (y > b) ? b : (y < 0) ? 0 : y;
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

    @Override public boolean handleMessage(Message msg) {
        canvas = holder.lockCanvas();
        if (canvas == null)
            return false;

        clean();
        int l = rectChart.left;
        int t = rectChart.top;
        int r = rectChart.right;
        int b = rectChart.bottom;

        drawFrame(rows, columns, l, t, r, b);
        drawFrameDash(rows, columns, l, t, r, b);
        if (dataSet != null) {
            drawData(dataSet.toPoints(), l, t, r, b);
            drawPoint(l, t, r, b);
            drawTarget(l, t, r, b);
        }
        holder.unlockCanvasAndPost(canvas);
        return true;
    }

    void updateDataPoints() {
        if (dataSet != null && rectChart.width() * rectChart.height() > 0) {
            dataSet.updatePoints(rectChart.width(), rectChart.height());
        }
    }

    public void refresh() {
        if (handler != null) {
            handler.removeMessages(100);
            handler.sendEmptyMessage(100);
        }
    }

    private void drawFrame(int rows, int columns, int l, int t, int r, int b) {
        //vertical
        canvas.drawLine(r, 0, r, b, pFrame);

        //horizontal
        canvas.drawLine(0, b, r, b, pFrame);

        tpDescription.setColor(frameDescriptionColor);
        int xStep = (r - l) / (columns + 1);
        int yStep = (b - t) / (rows + 1);
        long[] xValues = dataSet.splitXValues(columns);
        for (int i = 0; i < columns + 1; i++) {
            if (i == 0)
                continue;
            int x = xStep * i;
            String show = dataSet.showX(xValues[i]);
            tpDescription.getTextBounds(show, 0, show.length(), rectXDescription);
            x -= rectXDescription.width() / 2;
            canvas.drawText(show, x, b + rectXDescription.height() + 10, tpDescription);
        }
        float[] yValues = dataSet.splitYValues(rows);
        for (int i = rows; i >= 0; i--) {
            if (i == 0)
                continue;
            int y = (b - t) - yStep * i;
            String show = dataSet.showY(yValues[i]);
            tpDescription.getTextBounds(show, 0, show.length(), rectXDescription);
            if (r + rectXDescription.width() + 10 > screenWidth) {
                offsetX = rectXDescription.width() + UnitParser.dp2px(getContext(), 5) + 10;
                updateScreenSize(screenWidth, screenHeight);
                refresh();
                return;
            }
            y += rectXDescription.height() / 2;
            canvas.drawText(show, r + 10, y, tpDescription);
        }
    }

    private void drawFrameDash(int rows, int columns, int l, int t, int r, int b) {
        int xStep = (r - l) / (columns + 1);
        int yStep = (b - t) / (rows + 1);
        for (int i = 1; i < columns + 1; i++) {
            int x = xStep * i;
            canvas.drawLine(x + l, t, x + l, b, pFrameDash);
        }
        for (int i = 1; i < rows + 1; i++) {
            int y = yStep * i;
            canvas.drawLine(l, y + t, r, y + t, pFrameDash);
        }
    }

    private void drawPoint(int l, int t, int r, int b) {
        float x = touchPoint.x;
        float y = touchPoint.y;
        canvas.drawLine(x, 0, x, b, pGuideLine);
        if (dataSet.isMatched(x)) {
            DataSet.Box box = dataSet.findMatched(x);
            dataSet.notifyMatched(box.data);
            PointF target = dataSet.findMatchedPoint(x);
            canvas.drawLine(0, target.y, r, target.y, pGuideLine);
            canvas.drawCircle(target.x, target.y, 10, pGiudePoint);
        } else {
            canvas.drawLine(0, y, 0, y, pGuideLine);
        }
    }

    private void drawTarget(int l, int t, int r, int b) {
        float x = touchPoint.x;
        float y = touchPoint.y;
        if (dataSet.isMatched(x)) {
            tpDescription.setColor(targetTextColor);

            DataSet.Box box = dataSet.findMatched(x);
            PointF target = dataSet.findMatchedPoint(x);
            String yShow = dataSet.showY(box.yValue);
            tpDescription.getTextBounds(yShow, 0, yShow.length(), rectTargetXText);
            int paddingVertical = UnitParser.dp2px(getContext(), 5);
            rectTargetXBg.set(rectTargetXText);
            rectTargetXBg.top = rectTargetXBg.top - paddingVertical;
            rectTargetXBg.bottom = rectTargetXBg.bottom + paddingVertical;
            rectTargetXBg.left = r;
            rectTargetXBg.right = screenWidth;
            rectTargetXBg.offsetTo(r, (int) (target.y - rectTargetXBg.height() / 2));
            canvas.drawRect(rectTargetXBg, pTargetBg);
            canvas.drawText(yShow,
                            rectTargetXBg.centerX() - rectTargetXText.width() / 2,
                            rectTargetXBg.centerY() + rectTargetXText.height() / 2,
                            tpDescription);

            String xShow = dataSet.showX(box.xValue);
            tpDescription.getTextBounds(xShow, 0, xShow.length(), rectTargetYText);
            int paddingHorizontal = UnitParser.dp2px(getContext(), 5);
            rectTargetYBg.set(rectTargetYText);
            rectTargetYBg.left = rectTargetYBg.left - paddingHorizontal;
            rectTargetYBg.right = rectTargetYBg.right + paddingHorizontal;
            rectTargetYBg.top = b;
            rectTargetYBg.bottom = screenHeight;
            rectTargetYBg.offsetTo((int) (target.x - rectTargetYBg.width() / 2), b);
            canvas.drawRect(rectTargetYBg, pTargetBg);
            canvas.drawText(xShow,
                            rectTargetYBg.centerX() - rectTargetYText.width() / 2,
                            rectTargetYBg.centerY() + rectTargetYText.height() / 2,
                            tpDescription);
        }
    }

    private void drawData(List<PointF> points, int l, int t, int r, int b) {
        if (points.size() > 0) {
            PointF first = points.get(0);
            pathData.moveTo(first.x + l, first.y + t);
            for (PointF point : points) {
                float x = point.x;
                float y = point.y;
                pathData.lineTo(x + l, y + t);
            }
        }
        pathDataFill.moveTo(l, b);
        pathDataFill.addPath(pathData);
        pathDataFill.lineTo(r, b);
        pathDataFill.lineTo(l, b);

        canvas.drawPath(pathDataFill, pDataFill);
        canvas.drawPath(pathData, pData);
        pathDataFill.reset();
        pathData.reset();
    }

    private void updateScreenSize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        rectChart.set(0, 0, screenWidth - offsetX, screenHeight - offsetY);
        updateDataPoints();
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
        dataSet.setHost(this);
        dataSet.setXTimePattern(xTimePattern);
        dataSet.setYValPattern(yValPattern);
        dataSet.setScaleY(scaleY);
        updateDataPoints();
        refresh();
    }

    public void clean() {
        if (canvas != null) {
            canvas.drawColor(Color.TRANSPARENT);
            pClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            canvas.drawPaint(pClean);
            pClean.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
            canvas.drawColor(Color.WHITE);
        }
    }

    private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {

    };
}

package org.arxing.chart;

import android.graphics.PointF;

import com.annimon.stream.Stream;

import org.arxing.chart.protocol.Function;
import org.arxing.utils.Logger;
import org.arxing.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.arxing.chart.Chart.OUTSIDE_XY;

/**
 * 時間緊迫亂亂寫 x軸鎖死為時間(long)  y軸鎖死為值
 *
 * @param <T>
 */
public class DataSet<T> {
    private List<Box> dataSet = new ArrayList<>();
    private List<PointF> tmpPoints = new ArrayList<>();

    private Function<T, Long> xValueTransfer;
    private Function<T, Float> yValueTransfer;
    private float yMax, yMin;
    private long xMax, xMin;
    private int width, height;
    private float scaleX = 1.0f;
    private float scaleY = 1.8f;
    private OnMatchedPointListener<T> listener;
    private float threshold = 20;
    private T tmpData;
    private String xTimePattern;
    private String yValPattern;

    private Chart host;

    public DataSet(Function<T, Long> xTrans, Function<T, Float> yTrans) {
        setXTransfer(xTrans);
        setYTransfer(yTrans);
    }

    void setHost(Chart host) {
        this.host = host;
    }

    private void updateVal() {
        xMin = getXMinValue();
        xMax = getXMaxValue();
        yMin = getYMinValue();
        yMax = getYMaxValue();
        float xHalf = (xMax - xMin) * (scaleX - 1f) * 0.5f;
        float yHalf = (yMax - yMin) * (scaleY - 1f) * 0.5f;
        xMin -= xHalf;
        xMax += xHalf;
        yMin -= yHalf;
        yMax += yHalf;
    }

    void notifyMatched(T data) {
        float xValue = xValueTransfer.apply(data);
        float yValue = yValueTransfer.apply(data);
        if (listener != null && data != tmpData) {
            tmpData = data;
            listener.onMatch(data, xValue, yValue);
        }
    }

    Box findMatched(float x) {
        return Stream.of(dataSet).filter(o -> Math.abs(o.point.x - x) < threshold).min((box1, box2) -> {
            float d1 = Math.abs(box1.point.x - x);
            float d2 = Math.abs(box2.point.x - x);
            return Float.compare(d1, d2);
        }).orElse(null);
    }

    void updatePoints(int width, int height) {
        this.width = width;
        this.height = height;
        tmpPoints.clear();
        Stream.of(dataSet).forEach(box -> {
            T data = box.data;
            //X軸的值
            long xVal = xValueTransfer.apply(data);
            //X軸的座標值
            float x = (int) ((xVal - xMin) * width / (xMax - xMin));
            //Y軸的值
            float yVal = yValueTransfer.apply(data);
            //Y軸的座標值
            float y = (int) ((yVal - yMin) * height / (yMax - yMin));
            box.point.set(x, y);
            tmpPoints.add(new PointF(x, height - y));
        });
    }

    public void setXTimePattern(String xTimePattern) {
        this.xTimePattern = xTimePattern;
    }

    public void setYValPattern(String yValPattern) {
        this.yValPattern = yValPattern;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public void setOnMatchedPointListener(OnMatchedPointListener<T> listener) {
        this.listener = listener;
    }

    public void setXTransfer(Function<T, Long> xTransfer) {
        this.xValueTransfer = xTransfer;
    }

    public void setYTransfer(Function<T, Float> yTransfer) {
        this.yValueTransfer = yTransfer;
    }

    public void setData(List<T> data) {
        dataSet.clear();
        dataSet.addAll(Stream.of(data).map(o -> {
            Box box = new Box();
            box.data = o;
            box.xValue = xValueTransfer.apply(o);
            box.yValue = yValueTransfer.apply(o);
            return box;
        }).toList());
        updateVal();
        host.updateDataPoints();
        host.refresh();
    }

    public long getXMinValue() {
        return Stream.of(dataSet).map(o -> xValueTransfer.apply(o.data)).min(Long::compare).get();
    }

    public long getXMaxValue() {
        return Stream.of(dataSet).map(o -> xValueTransfer.apply(o.data)).max(Long::compare).get();
    }

    public float getYMinValue() {
        return Stream.of(dataSet).map(o -> yValueTransfer.apply(o.data)).min(Float::compare).get();
    }

    public float getYMaxValue() {
        return Stream.of(dataSet).map(o -> yValueTransfer.apply(o.data)).max(Float::compare).get();
    }

    public int size() {
        return dataSet.size();
    }

    public List<PointF> toPoints() {
        return tmpPoints;
    }

    public boolean isMatched(float x) {
        return findMatched(x) != null;
    }

    public PointF findMatchedPoint(float x) {
        PointF result = new PointF();
        if (isMatched(x)) {
            Box box = findMatched(x);
            //再反轉回來
            result.set(box.point.x, height - box.point.y);
        } else {
            result.set(OUTSIDE_XY, OUTSIDE_XY);
        }
        return result;
    }

    public float[] splitYValues(int rows) {
        float[] result = new float[rows + 1];
        float step = (yMax - yMin) / (rows + 1);
        for (int i = 0; i < rows + 1; i++) {
            result[i] = yMin + step * i;
        }
        return result;
    }

    public long[] splitXValues(int columns) {
        long[] result = new long[columns + 1];
        long step = (xMax - xMin) / (columns + 1);
        for (int i = 0; i < columns + 1; i++) {
            result[i] = xMin + step * i;
        }
        return result;
    }

    public String showX(long v) {
        return TimeUtils.format(xTimePattern, v);
    }

    public String showY(float v) {
        return String.format(yValPattern, v);
    }

    class Box {
        T data;
        PointF point = new PointF();
        long xValue;
        float yValue;
    }

    public interface OnMatchedPointListener<T> {
        void onMatch(T data, float xValue, float yValue);
    }
}

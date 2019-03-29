package org.arxing.chart;

import android.graphics.Point;
import android.graphics.PointF;

import com.annimon.stream.Stream;

import org.arxing.chart.protocol.Function;

import java.util.ArrayList;
import java.util.List;

import static org.arxing.chart.Chart.OUTSIDE_XY;

public class DataSet<T> {
    private List<Box> dataSet = new ArrayList<>();
    private List<PointF> tmpPoints = new ArrayList<>();

    private Function<T, Float> xValueTransfer;
    private Function<T, Float> yValueTransfer;
    private float xMax, xMin, yMax, yMin;
    private int width, height;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private OnMatchedPointListener<T> listener;

    public DataSet(Function<T, Float> xTrans, Function<T, Float> yTrans) {
        setXTransfer(xTrans);
        setYTransfer(yTrans);
    }

    private void updateVal() {
        xMin = getXMinValue();
        xMax = getXMaxValue();
        yMin = getYMinValue();
        yMax = getYMaxValue();
    }

    void notifyMatched(T data) {
        float xValue = xValueTransfer.apply(data);
        float yValue = yValueTransfer.apply(data);
        if (listener != null)
            listener.onMatch(data, xValue, yValue);
    }

    Box findMatched(float x) {
        return Stream.of(dataSet).filter(o -> Math.abs(o.point.x - x) < 5).findFirst().orElse(null);
    }

    public void setOnMatchedPointListener(OnMatchedPointListener<T> listener) {
        this.listener = listener;
    }

    public void setXTransfer(Function<T, Float> xTransfer) {
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
    }

    public void addData(T o) {
        Box box = new Box();
        box.data = o;
        box.xValue = xValueTransfer.apply(o);
        box.yValue = yValueTransfer.apply(o);
        dataSet.add(box);
        updateVal();
    }

    public float getXMinValue() {
        return Stream.of(dataSet).map(o -> xValueTransfer.apply(o.data)).min(Float::compare).get();
    }

    public float getXMaxValue() {
        return Stream.of(dataSet).map(o -> xValueTransfer.apply(o.data)).max(Float::compare).get();
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

    public void updatePoints(int width, int height) {
        this.width = width;
        this.height = height;
        tmpPoints.clear();
        Stream.of(dataSet).forEach(box -> {
            T data = box.data;
            //X軸的值
            float xVal = xValueTransfer.apply(data);
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

    class Box {
        T data;
        PointF point = new PointF();
        float xValue;
        float yValue;
    }

    public interface OnMatchedPointListener<T> {
        void onMatch(T data, float xValue, float yValue);
    }
}

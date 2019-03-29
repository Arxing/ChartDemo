package org.arxing.chartdemo;

import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.arxing.chart.Chart;
import org.arxing.chart.DataSet;
import org.arxing.utils.Logger;

import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Chart chart;
    DataSet<PointF> dataSet;
    Random random = new Random();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chart = findViewById(R.id.chart);
        dataSet = new DataSet<>(o -> o.x, o -> o.y);

        dataSet.setOnMatchedPointListener((data, xValue, yValue) -> {
            Logger.defLogger.e("xValue=%f, yValue=%f", xValue, yValue);
        });

        int preVal = 0;
        for (int i = 0; i < 150; i++) {
            int val = random(preVal, 10);
            dataSet.addData(new PointF(i, val));
            preVal = val;
        }
        chart.setDataSet(dataSet);

    }

    private int random(int base, int range) {
        int result;
        do {
            result = random.nextInt() % 100;
        } while (Math.abs(result - base) > range);
        return result;
    }

}

package org.arxing.chartdemo;

import android.graphics.PointF;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

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
//            Logger.defLogger.e("xValue=%f, yValue=%f", xValue, yValue);
        });


        int preVal = random.nextInt(100);
        for (int i = 0; i < 100; i++) {
            int val = preVal + randomOffset(100);
            dataSet.addData(new PointF(i, val));
            preVal = val;
        }
        chart.setDataSet(dataSet);
    }

    private int randomOffset(int range) {
        int v = random.nextInt(range) - range/2;
        Logger.defLogger.e("v=%d",v);
        return v;
    }

}

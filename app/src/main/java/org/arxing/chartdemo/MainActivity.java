package org.arxing.chartdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import org.arxing.chart.Chart;
import org.arxing.chart.DataSet;
import org.arxing.utils.Logger;
import org.arxing.utils.ThreadUtil;
import org.arxing.utils.TimeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    Chart chart;
    DataSet<Data> dataSet;
    Random random = new Random();

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        chart = findViewById(R.id.chart);
        dataSet = new DataSet<>(o -> o.time, o -> o.val);

        dataSet.setOnMatchedPointListener((data, xValue, yValue) -> {
            Logger.defLogger.e("xValue=%f, yValue=%f", xValue, yValue);
        });

        chart.setDataSet(dataSet);
        dataSet.updateData(randomData());
    }

    private List<Data> randomData() {
        List<Data> list = new ArrayList<>();
        int preVal = random.nextInt(100);
        for (int i = 0; i < 100; i++) {
            int val = preVal + randomOffset(100);
            long time = random.nextLong() % (1554106194164L - 1554100000000L) + 1554100000000L;
            list.add(new Data(time, val));
            preVal = val;
        }
        Collections.sort(list, (o1, o2) -> Long.compare(o1.time, o2.time));
        return list;
    }

    private int randomOffset(int range) {
        int v = random.nextInt(range) - range / 2;
        return v;
    }

    public void c(View v) {
        dataSet.clearData();
        ThreadUtil.sleep(100);
        dataSet.updateData(randomData());
    }

    static class Data {
        long time;
        float val;

        public Data(String time, float val) {
            this.time = TimeUtils.parse("yyyy-MM-dd HH:mm:ss", time).getTime();
            this.val = val;
        }

        public Data(long time, float val) {
            this.time = time;
            this.val = val;
        }
    }

}

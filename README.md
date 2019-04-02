# 使用說明

1. build.gradle(Project)
```gradle
maven { url 'https://raw.githubusercontent.com/Arxing/axcore/master/' }
```

2. build.gradle(app)
```gradle
implementation('org.arxing:chart:latest') {
    exclude group: 'com.android.support'
}
```

3. 導入view
```xml
<org.arxing.chart.Chart
    android:id="@+id/chart"
    android:layout_width="match_parent"
    android:layout_height="0dp"
    app:layout_constraintHeight_percent="0.5"
    app:layout_constraintBottom_toBottomOf="parent"
    />
```

4. 建立資料模組
```java
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
```

5. 建立圖表資料集合物件
```java
DataSet<Data> dataSet;
//建構子參數為x,y軸值轉換器
dataSet = new DataSet<>(o -> o.time, o -> o.val);
//設置監聽
dataSet.setOnMatchedPointListener((data, xValue, yValue) -> {
    Logger.defLogger.e("xValue=%f, yValue=%f", xValue, yValue);
});
//Chart綁定DataSet
chart.setDataSet(dataSet);

//更新資料後 圖表自動刷新
dataSet.updateData(randomData());
//清空資料 清空圖表
dataSet.clearData();
```
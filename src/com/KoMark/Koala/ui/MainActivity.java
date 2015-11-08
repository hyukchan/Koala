package com.KoMark.Koala.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.data.SensorData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

public class MainActivity extends Activity implements AccReadingListener {

    LineChart chart;
    KoalaApplication context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = (KoalaApplication) getApplicationContext();
        context.getKoalaManager().kSensorManager.addAccReadingListener(this);

        chart = (LineChart) findViewById(R.id.chart);
        chart.setBackgroundColor(Color.rgb(77, 77, 77));
        chart.setGridBackgroundColor(Color.rgb(77, 77, 77));
        chart.setDrawGridBackground(false);
        chart.setData(new LineData());
        chart.setDragEnabled(true);
        chart.invalidate();
    }

    int[] mColors = ColorTemplate.VORDIPLOM_COLORS;

    @Override
    public void onAccReadingReceived(SensorData sensorData) {
        addDataToChart(sensorData);
    }

    public void addDataToChart(SensorData sensorData) {
        addEntry(sensorData);
    }

    private void addEntry(SensorData sensorData) {

        LineData data = chart.getData();

        if(data != null) {

            LineDataSet set = data.getDataSetByIndex(0);

            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }

            // add a new x-value first
            data.addXValue(sensorData.getTimestamp() + "");

            // choose a random dataSet
            int randomDataSetIndex = (int) (Math.random() * data.getDataSetCount());

            data.addEntry(new Entry(sensorData.getAcc(), set.getEntryCount()), randomDataSetIndex);

            // let the chart know it's data has changed
            chart.notifyDataSetChanged();

            chart.setVisibleXRangeMaximum(50);
            chart.setVisibleYRangeMaximum(100, YAxis.AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
            chart.moveViewTo(data.getXValCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "OnDestroy called");
        context.onTerminate();

    }

    private LineDataSet createSet() {

        LineDataSet set = new LineDataSet(null, "DataSet 1");
        set.setLineWidth(2.5f);
        set.setCircleSize(4.5f);
        set.setColor(Color.rgb(240, 99, 99));
        set.setCircleColor(Color.rgb(240, 99, 99));
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(10f);

        return set;
    }
}

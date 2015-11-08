package com.KoMark.Koala.ui;

import android.content.Context;
import android.graphics.Color;
import com.KoMark.Koala.data.SensorData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

/**
 * Created by Hyukchan on 04/11/2015.
 */
public class KChart {

    LineChart lineChart;

    public KChart(LineChart lineChart) {
        this.lineChart = lineChart;
    }

    public void initializeChart() {
        lineChart.setBackgroundColor(Color.rgb(77, 77, 77));
        lineChart.setGridBackgroundColor(Color.rgb(77, 77, 77));
        lineChart.setDrawGridBackground(false);
        lineChart.setData(new LineData());
        lineChart.setDragEnabled(true);
        lineChart.invalidate();
    }

    public void addEntry(SensorData sensorData) {

        LineData data = lineChart.getData();

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
            lineChart.notifyDataSetChanged();

            lineChart.setVisibleXRangeMaximum(50);
            lineChart.setVisibleYRangeMaximum(100, YAxis.AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
            lineChart.moveViewTo(data.getXValCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        }
    }

    public LineDataSet createSet() {

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

package com.KoMark.Koala.ui;

import android.graphics.Color;
import com.KoMark.Koala.data.SensorData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.HashMap;

/**
 * Created by Hyukchan on 04/11/2015.
 */
public class KChart {

    LineChart lineChart;
    HashMap<String, Integer> deviceDatasets;

    int devicesetIndex = 1;

    public KChart(LineChart lineChart) {
        this.lineChart = lineChart;
        deviceDatasets = new HashMap<String, Integer>();
    }

    public void initializeChart() {
        lineChart.setBackgroundColor(Color.rgb(77, 77, 77));
        lineChart.setGridBackgroundColor(Color.rgb(77, 77, 77));
        lineChart.setDrawGridBackground(false);
        lineChart.setData(new LineData());
        lineChart.setDragEnabled(true);
        lineChart.invalidate();
    }

    public void addEntry(SensorData sensorData, String deviceName) {
        int deviceDatasetIndex = deviceDatasets.containsKey(deviceName) ? deviceDatasets.get(deviceName) : devicesetIndex++;
        addEntryToDataset(sensorData, deviceName, deviceDatasetIndex);
    }


    public void addEntry(SensorData sensorData) {
        addEntryToDataset(sensorData, "Current Device", 0);
    }

    public void addEntryToDataset(SensorData sensorData, String deviceName, int datasetIndex) {
        LineData data = lineChart.getData();

        if(data != null) {
            LineDataSet set = data.getDataSetByIndex(datasetIndex);

            if (set == null) {
                set = createSet(deviceName, Color.rgb(240, 99, 99));
                data.addDataSet(set);
            }

            // add a new x-value first
            data.addXValue(sensorData.getTimestamp() + "");
            data.addEntry(new Entry(sensorData.getAcc(), set.getEntryCount()), datasetIndex);

            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();

            lineChart.setVisibleXRangeMaximum(50);
            lineChart.setVisibleYRangeMaximum(100, YAxis.AxisDependency.LEFT);
//
//            // this automatically refreshes the chart (calls invalidate())
            lineChart.moveViewTo(data.getXValCount() - 7, 50f, YAxis.AxisDependency.LEFT);
        }
    }

    public LineDataSet createSet(String dataSetName, int color) {

        LineDataSet set = new LineDataSet(null, dataSetName);
        set.setLineWidth(2.5f);
        set.setCircleSize(4.5f);
        set.setColor(color);
        set.setCircleColor(color);
        set.setHighLightColor(Color.rgb(190, 190, 190));
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setValueTextSize(10f);

        return set;
    }
}

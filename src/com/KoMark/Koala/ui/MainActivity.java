package com.KoMark.Koala.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.data.SensorData;
import com.github.mikephil.charting.charts.LineChart;

public class MainActivity extends Activity implements AccReadingListener {
    KChart kChart;
    KoalaApplication context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = (KoalaApplication) getApplicationContext();
        context.getKoalaManager().kSensorManager.addAccReadingListener(this);

        LineChart lineChart = (LineChart) findViewById(R.id.chart);

        kChart = new KChart(lineChart);
        kChart.initializeChart();
    }

    @Override
    public void onAccReadingReceived(SensorData sensorData) {
        addDataToChart(sensorData);
    }

    public void addDataToChart(SensorData sensorData) {
        kChart.addEntry(sensorData);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "OnDestroy called");
        context.onTerminate();
    }
}

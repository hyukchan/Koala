package com.KoMark.Koala.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.core.listeners.AccidentDetectedListener;
import com.KoMark.Koala.core.listeners.KCommListener;
import com.KoMark.Koala.core.listeners.SensorDataPackageReceiveListener;
import com.KoMark.Koala.data.SensorData;
import com.github.mikephil.charting.charts.LineChart;

import java.util.ArrayList;

public class MainActivity extends Activity implements AccReadingListener, SensorDataPackageReceiveListener, KCommListener, AccidentDetectedListener {
    KChart kChart;
    KoalaApplication context;
    TextView koalaNetworkSizeTextView;

    AlertDialog.Builder alertDialogBuilder;
    AlertDialog alertDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = (KoalaApplication) getApplicationContext();
        context.setActivityContext(this);

        LineChart lineChart = (LineChart) findViewById(R.id.chart);

        kChart = new KChart(lineChart);
        kChart.initializeChart();

        Log.e("MainActivity", "onCreate");

        alertDialogBuilder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Accident Detected !")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert);

        alertDialog = alertDialogBuilder.create();

        context.onResume();
    }

    public void resumeKChart() {
        kChart.resumeChart(context.getKoalaManager().kCluster.getAccReadings());
    }

    @Override
    public void onAccReadingReceived(SensorData sensorData) {
        addDataToChart(sensorData);
    }

    public void addDataToChart(SensorData sensorData) {
        kChart.addEntry(sensorData);
    }

    @Override
    protected void onResume() {
        super.onResume();
        koalaNetworkSizeTextView = (TextView) findViewById(R.id.koala_network_size);
        if(context.getKoalaManager() != null) {
            updateKoalaNetworkSize();
        }
    }

    public void updateKoalaNetworkSize() {
        koalaNetworkSizeTextView.setText("" + (context.getKoalaManager().kComm.getPeerList().size() + 1));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MainActivity", "OnDestroy called");
        //context.onTerminate();
    }

    @Override
    public void onSensorDataPackageReceive(ArrayList<SensorData> sensorDataPackage, String senderDeviceName) {
        kChart.addReceivedSensorDataPackage(sensorDataPackage, senderDeviceName);
        Log.i("MainActivity", "Receive sensor data package");
    }

    public void onClickKoalaNetworkTitle(View view) {
        Intent intent = new Intent(this, ScanViewActivity.class);
        startActivity(intent);

    }

    public void setListeners() {
        context.getKoalaManager().kSensorManager.addAccReadingListener(this);
        context.getKoalaManager().kComm.addSensorDataPackageReceiveListener(this);
        context.getKoalaManager().kComm.addKCommListener(this);
        context.getKoalaManager().kCluster.addAccidentDetectedListener(this);
    }

    public void onClickChartFocus(View view) {
        if(kChart.focusChart()) {
            ((ImageView) view).setImageResource(R.drawable.button_chart_focus_active);
        } else {
            ((ImageView) view).setImageResource(R.drawable.button_chart_focus_unactive);
        }
    }

    @Override
    public void onDeviceFound(BluetoothDevice newDevice) {

    }

    @Override
    public void onDevicePaired(BluetoothDevice newDevice) {

    }

    @Override
    public void onDeviceConnected(BluetoothDevice newDevice) {
        Log.i("MainActivity", "device connected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateKoalaNetworkSize();
            }
        });
    }

    @Override
    public void onStartScan() {

    }

    @Override
    public void onStopScan() {

    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice disconnectedDevice) {
        Log.i("MainActivity", "device disconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateKoalaNetworkSize();
            }
        });
    }

    @Override
    public void onDeviceUnpaired(BluetoothDevice device) {

    }

    @Override
    public void onAccidentDetected() {
        if(!alertDialog.isShowing()) {
            alertDialog.show();
        }
    }
}

package com.KoMark.Koala.ui;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;
import com.KoMark.Koala.core.listeners.KCommListener;

import java.util.ArrayList;

/**
 * Created by Hyukchan on 28/11/2015.
 */
public class ScanViewActivity extends Activity implements KCommListener {

    ListView bluetoothDevicesListView;
    ListView connectedBluetoothDevicesListView;
    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    ArrayList<BluetoothDevice> connectedBluetoothDevices = new ArrayList<>();
    BluetoothDeviceAdapter bluetoothDeviceAdapter;
    ConnectedBluetoothDeviceAdapter connectedBluetoothDeviceAdapter;

    KoalaApplication context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanview);

        bluetoothDevicesListView = (ListView) findViewById(R.id.scanview_devices);
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(this, bluetoothDevices);
        bluetoothDevicesListView.setAdapter(bluetoothDeviceAdapter);


        connectedBluetoothDevicesListView = (ListView) findViewById(R.id.scanview_connecteddevices);
        connectedBluetoothDeviceAdapter = new ConnectedBluetoothDeviceAdapter(this, connectedBluetoothDevices);
        connectedBluetoothDevicesListView.setAdapter(connectedBluetoothDeviceAdapter);

        context = (KoalaApplication) getApplicationContext();
        context.getKoalaManager().kComm.addKCommListener(this);
    }

    @Override
    public void onDeviceFound(BluetoothDevice newDevice) {
        addDevice(newDevice);
    }

    @Override
    public void onDevicePaired(BluetoothDevice newDevice) {
        addDevice(newDevice);
    }

    @Override
    public void onDeviceConnected(BluetoothDevice newConnectedDevice) {
        addConnectedDevice(newConnectedDevice);
    }

    public void addDevice(BluetoothDevice newDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothDevices.add(newDevice);
                bluetoothDeviceAdapter.add(newDevice);
                bluetoothDeviceAdapter.notifyDataSetChanged();
            }
        });
    }

    public void addConnectedDevice(BluetoothDevice newConnectedDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectedBluetoothDevices.add(newConnectedDevice);
                connectedBluetoothDeviceAdapter.add(newConnectedDevice);
                connectedBluetoothDeviceAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStartScan() {
        //empty existing lists
        /*bluetoothDevices = new ArrayList<>();
        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(this, bluetoothDevices);
        bluetoothDeviceAdapter.notifyDataSetChanged();

        connectedBluetoothDevices = new ArrayList<>();
        connectedBluetoothDeviceAdapter = new ConnectedBluetoothDeviceAdapter(this, connectedBluetoothDevices);
        connectedBluetoothDeviceAdapter.notifyDataSetChanged();*/
    }

    @Override
    public void onStopScan() {

    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice disconnectedDevice) {

    }

    @Override
    public void onDeviceUnpaired(BluetoothDevice device) {

    }

    public void onClickBackButton(View view) {
        onBackPressed();
    }

    public void onClickRefreshButton(View view) {

    }
}
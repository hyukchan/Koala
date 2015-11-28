package com.KoMark.Koala.ui;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ListView;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;
import com.KoMark.Koala.core.listeners.KCommListener;

import java.util.ArrayList;

/**
 * Created by Hyukchan on 28/11/2015.
 */
public class ScanViewActivity extends Activity implements KCommListener {

    ListView listView;
    ArrayList<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    BluetoothDeviceAdapter bluetoothDeviceAdapter;

    KoalaApplication context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanview);

        listView = (ListView) findViewById(R.id.scanview_listview);

        bluetoothDeviceAdapter = new BluetoothDeviceAdapter(this, bluetoothDevices);
        listView.setAdapter(bluetoothDeviceAdapter);

        context = (KoalaApplication) getApplicationContext();
        context.getKoalaManager().kComm.addKCommListener(this);

        context.getKoalaManager().kComm.scanForPeers();
    }

    @Override
    public void onDeviceFound(BluetoothDevice newDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothDevices.add(newDevice);
                //bluetoothDeviceAdapter.setList(bluetoothDevices);
                bluetoothDeviceAdapter.add(newDevice);
                bluetoothDeviceAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDevicePaired(BluetoothDevice newDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothDevices.add(newDevice);
                bluetoothDeviceAdapter.setList(bluetoothDevices);
                bluetoothDeviceAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDeviceConnected(BluetoothDevice newDevice) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothDevices.add(newDevice);
                bluetoothDeviceAdapter.setList(bluetoothDevices);
                bluetoothDeviceAdapter.notifyDataSetChanged();
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

    }

    @Override
    public void onDeviceUnpaired(BluetoothDevice device) {

    }
}
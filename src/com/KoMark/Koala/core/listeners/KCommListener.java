package com.KoMark.Koala.core.listeners;

import android.bluetooth.BluetoothDevice;

/**
 * Created by TeraByte on 25-11-2015.
 */
public interface KCommListener {
    void onDeviceFound(BluetoothDevice newDevice);
    void onDevicePaired(BluetoothDevice newDevice);
    void onDeviceConnected(BluetoothDevice newDevice);
    void onStartScan();
    void onStopScan();
    void onDeviceDisconnected(BluetoothDevice disconnectedDevice);
    void onDeviceUnpaired(BluetoothDevice device);
}

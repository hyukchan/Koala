package com.KoMark.Koala.data;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by TeraByte on 08-11-2015.
 */
public class KGroupData implements Serializable {

    private static final long serialVersionUID = -3088240274207500418L;
    ArrayList<String> deviceNames = new ArrayList<>();
    ArrayList<String> deviceAddresses = new ArrayList<>();

    public KGroupData(ArrayList<BluetoothDevice> deviceList) {
        for (BluetoothDevice device : deviceList) {
            deviceNames.add(device.getName());
            deviceAddresses.add(device.getAddress());
        }
    }

    public ArrayList<String> getDeviceNames() {
        return deviceNames;
    }

    public ArrayList<String> getDeviceAddresses() {
        return deviceAddresses;
    }
}

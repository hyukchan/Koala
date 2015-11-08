package com.KoMark.Koala.core.listeners;

import com.KoMark.Koala.data.SensorData;

import java.util.ArrayList;

/**
 * Created by Hyukchan on 08/11/2015.
 */
public interface SensorDataPackageReceiveListener {
    void onSensorDataPackageReceive(ArrayList<SensorData> sensorDataPackage);
}

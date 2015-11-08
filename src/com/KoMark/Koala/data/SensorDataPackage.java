package com.KoMark.Koala.data;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Serializable object to send SensorData over Bluetooth
 */
public class SensorDataPackage implements Serializable {
    private ArrayList<SensorData> mPackage;


    public SensorDataPackage(ArrayList<SensorData> mPackage) {
        this.mPackage = mPackage;
    }
}

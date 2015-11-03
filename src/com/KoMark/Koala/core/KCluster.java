package com.KoMark.Koala.core;

import android.content.Context;
import android.util.Log;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.data.SensorData;

import java.util.ArrayList;

/**
 * Responsible for clustering data received from KoalaNetwork devices to decide whether or not that has been an accident
 */
public class KCluster implements AccReadingListener{
    ArrayList<SensorData> accReadings;
    ArrayList<SensorData> speedReadings;

    public KCluster(Context context) {
        accReadings = new ArrayList<SensorData>();
        speedReadings = new ArrayList<SensorData>();

        ((KoalaApplication) context).getKoalaManager().kSensorManager.addAccReadingListener(this);
    }

    public void addAccReading(SensorData sensorData) {
        if(accReadings.size() > 5000) {
            accReadings.remove(0);
        }
        accReadings.add(sensorData);
    }

    @Override
    public void onAccReadingReceived(SensorData sensorData) {
        addAccReading(sensorData);
    }
}

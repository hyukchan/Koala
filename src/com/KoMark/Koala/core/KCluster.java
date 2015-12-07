package com.KoMark.Koala.core;

import android.content.Context;
import android.hardware.Sensor;
import android.util.Log;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.data.SensorData;

import java.util.ArrayList;

/**
 * Responsible for clustering data received from KoalaNetwork devices to decide whether or not that has been an accident
 */
public class KCluster implements AccReadingListener {

    KoalaManager koalaManager;
    ArrayList<SensorData> accReadings;
    ArrayList<SensorData> speedReadings;

    long lastSentTimestamp = 0;

    public KCluster(Context context) {
        accReadings = new ArrayList<SensorData>();
        speedReadings = new ArrayList<SensorData>();
        koalaManager = ((KoalaApplication) context).getKoalaManager();

        koalaManager.kSensorManager.addAccReadingListener(this);
    }

    public ArrayList<SensorData> getAccReadings() {
        return accReadings;
    }

    public void addAccReading(SensorData sensorData) {
        if (accReadings.size() > 5000) {
            accReadings.remove(0);
        }
        accReadings.add(sensorData);

        //FIXME: Sensitivity needs to be refined
        if (sensorData.getAcc() > 20) {
            SensorData peakSensorData = accReadings.get(accReadings.size() - 1);
            if(lastSentTimestamp == 0 || peakSensorData.getTimestamp() - lastSentTimestamp > 2000) {
                //Sends data to master device
                if(koalaManager.kComm.isSlave()) {
                    koalaManager.kComm.sendAccReadings(new ArrayList<SensorData>(accReadings.subList(accReadings.size() - 10, accReadings.size())));
                }
                lastSentTimestamp = peakSensorData.getTimestamp();
            }
        }
    }

    @Override
    public void onAccReadingReceived(SensorData sensorData) {
        addAccReading(sensorData);
    }
}

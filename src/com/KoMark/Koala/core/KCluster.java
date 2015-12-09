package com.KoMark.Koala.core;

import android.content.Context;
import android.hardware.Sensor;
import android.util.Log;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.core.listeners.AccidentDetectedListener;
import com.KoMark.Koala.core.listeners.SensorDataPackageReceiveListener;
import com.KoMark.Koala.data.SensorData;

import java.util.*;

/**
 * Responsible for clustering data received from KoalaNetwork devices to decide whether or not that has been an accident
 */
public class KCluster implements AccReadingListener, SensorDataPackageReceiveListener {

    KoalaManager koalaManager;
    ArrayList<SensorData> accReadings;
    ArrayList<SensorData> speedReadings;

    HashMap<String, Long> accidentDeviceTimes = new HashMap<>();

    final static String currentDevice = "Current Device";



    ArrayList<AccidentDetectedListener> accidentDetectedListeners = new ArrayList<>();

    long lastSentTimestamp = 0;

    public KCluster(Context context) {
        accReadings = new ArrayList<SensorData>();
        speedReadings = new ArrayList<SensorData>();
        koalaManager = ((KoalaApplication) context).getKoalaManager();

        koalaManager.kSensorManager.addAccReadingListener(this);
        koalaManager.kComm.addSensorDataPackageReceiveListener(this);

        accidentDeviceTimes.put(currentDevice, (long) 0);
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

            accidentDeviceTimes.put(currentDevice, sensorData.getTimestamp());

            SensorData peakSensorData = accReadings.get(accReadings.size() - 1);
            if(lastSentTimestamp == 0 || peakSensorData.getTimestamp() - lastSentTimestamp > 2000) {
                //Sends data to master device
                if(koalaManager.kComm.isSlave()) {
                    koalaManager.kComm.sendAccReadings(new ArrayList<SensorData>(accReadings.subList(accReadings.size() - 10, accReadings.size())));
                }
                lastSentTimestamp = peakSensorData.getTimestamp();

            }
            checkAccident();
        }
    }

    @Override
    public void onAccReadingReceived(SensorData sensorData) {
        addAccReading(sensorData);
    }

    @Override
    public void onSensorDataPackageReceive(ArrayList<SensorData> sensorDataPackage, String senderDeviceName) {
        float receivedMaxAcc = 0;
        for (SensorData receivedSensorData : sensorDataPackage) {
            if(receivedSensorData.getAcc() > receivedMaxAcc) {
                receivedMaxAcc = receivedSensorData.getAcc();
            }
        }

        if(receivedMaxAcc > 20) {
            accidentDeviceTimes.put(senderDeviceName, System.currentTimeMillis());
            checkAccident();
        }
    }

    private void checkAccident() {
        long min = Long.MAX_VALUE;
        long max = 0;

        int i = 0;

        for(long time : accidentDeviceTimes.values()) {
            if(time < min) {
                min = time;
            }

            if(time > max) {
                max = time;
            }
            i++;
        }

        if(i > 1 && max - min < 3000) {
            for (AccidentDetectedListener accidentDetectedListener : accidentDetectedListeners) {
                accidentDetectedListener.onAccidentDetected();
            }

        }
    }

    public void addAccidentDetectedListener(AccidentDetectedListener accidentDetectedListener) {
        accidentDetectedListeners.add(accidentDetectedListener);
    }
}

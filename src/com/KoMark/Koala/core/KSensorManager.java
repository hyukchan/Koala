package com.KoMark.Koala.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.core.listeners.AccReadingListener;
import com.KoMark.Koala.data.SensorData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KSensorManager implements SensorEventListener {

    SensorManager mSensorManager;
    Sensor mSensor;
    long lastUpdate;
    Context context;

    final float alpha = 0.8F;

    float gravity[];
    float linear_acceleration[];

    private List<AccReadingListener> accReadingListeners = new ArrayList<AccReadingListener>();

    public KSensorManager(Context context) {
        this.context = context;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        gravity = new float[3];
        linear_acceleration = new float[3];
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor _sensor = sensorEvent.sensor;

        if (_sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 50) {
                lastUpdate = curTime;

                // Isolate the force of gravity with the low-pass filter.
                gravity[0] = alpha * gravity[0] + (1 - alpha) * sensorEvent.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * sensorEvent.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * sensorEvent.values[2];

                // Remove the gravity contribution with the high-pass filter.
                linear_acceleration[0] = sensorEvent.values[0] - gravity[0];
                linear_acceleration[1] = sensorEvent.values[1] - gravity[1];
                linear_acceleration[2] = sensorEvent.values[2] - gravity[2];

                float acc = Math.abs(linear_acceleration[0]) + Math.abs(linear_acceleration[1]) + Math.abs(linear_acceleration[2]);

                SensorData sensorData = new SensorData(acc, sensorEvent.timestamp);
                notifyAccReadingListeners(sensorData);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void notifyAccReadingListeners(SensorData sensorData) {
        for (AccReadingListener listener : accReadingListeners) {
            listener.onAccReadingReceived(sensorData);
        }
    }

    public void addAccReadingListener(AccReadingListener accReadingListener) {
        accReadingListeners.add(accReadingListener);
    }
}

package com.KoMark.Koala.core;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import com.KoMark.Koala.KoalaApplication;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KSensorManager implements SensorEventListener{

    SensorManager mSensorManager;
    Sensor mSensor;
    long lastUpdate;
    Context context;

    public KSensorManager(Context context) {
        this.context = context;
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor _sensor = sensorEvent.sensor;

        if (_sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[0];
            float y = sensorEvent.values[1];
            float z = sensorEvent.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 100) {
                lastUpdate = curTime;
                Log.e("KSensorManager", "x: " + x + ", y: " + y + ", z: " + z);
                ((KoalaApplication) context).getKoalaManager().kCluster.addAccReadings();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

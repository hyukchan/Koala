package com.KoMark.Koala.core.listeners;

import com.KoMark.Koala.data.SensorData;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public interface AccReadingListener {
    void onAccReadingReceived(SensorData sensorData);
}

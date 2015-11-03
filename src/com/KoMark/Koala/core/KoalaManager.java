package com.KoMark.Koala.core;

import android.content.Context;
import android.util.Log;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KoalaManager {
    public KCluster kCluster;
    public KSensorManager kSensorManager;
    public KAlertingAgent kAlertingAgent;
    public KComm kComm;

    public void initializeComponents(Context applicationContext) {
        kSensorManager = new KSensorManager(applicationContext);
        kCluster = new KCluster(applicationContext);
        kAlertingAgent = new KAlertingAgent();
        kComm = new KComm();
    }
}

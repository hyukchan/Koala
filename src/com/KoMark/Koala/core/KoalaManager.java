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
        kCluster = new KCluster();
        kSensorManager = new KSensorManager(applicationContext);
        kAlertingAgent = new KAlertingAgent();
        kComm = new KComm(applicationContext);

        Log.e("KoalaManager", "initializeComponents");
    }

    public void test() {
        Log.e("KoalaManager", "haha ça marche !");
    }

    public void shutdown() {
        kComm.closeAllConnections();
    }
}

package com.KoMark.Koala.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.KoMark.Koala.KoalaApplication;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KoalaService extends Service {
    KoalaManager koalaManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        koalaManager = ((KoalaApplication) getApplicationContext()).getKoalaManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(koalaManager.kComm);
        Log.i("KService", "OnDestroy called.");
        koalaManager.shutdown();
    }


}

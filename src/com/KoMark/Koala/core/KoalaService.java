package com.KoMark.Koala.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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
    public void onDestroy() {
        super.onDestroy();
        koalaManager.shutdown();
    }
}

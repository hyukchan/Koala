package com.KoMark.Koala.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;

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
        Intent notificationIntent = new Intent(this, KoalaService.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification.Builder build = new Notification.Builder(getApplicationContext());
        build.setContentIntent(pendingIntent)
                .setContentTitle("Koala")
                .setContentText("Running")
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis());
        Notification n = build.getNotification();
        startForeground(999, n);
        Log.i("KoalaService", "OnCreate executed");
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(koalaManager.kComm);
        Log.i("KService", "OnDestroy called.");
        koalaManager.shutdown();
    }


}

package com.KoMark.Koala.core;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.KoMark.Koala.KoalaApplication;
import com.KoMark.Koala.R;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KoalaService extends Service {
    KoalaManager koalaManager;
    private final String CLASS_TAG = "KService";
    private final IBinder myBinder = new KServiceBinder();



    @Override
    public IBinder onBind(Intent intent) {
        Log.i(CLASS_TAG, "OnBind called");
        return myBinder;
    }

    public void initKManager(Context context) {
        if(koalaManager == null) {
            koalaManager = new KoalaManager();
            ((KoalaApplication) context).setKManager(koalaManager);
            koalaManager.initializeComponents(context);
            Log.i(CLASS_TAG, "Created new KManager");
        } else {
            ((KoalaApplication) context).setKManager(koalaManager);
            Log.i(CLASS_TAG, "Bound to old KManager");
        }
    }

    public class KServiceBinder extends Binder {
        public KoalaService getService() {
            return KoalaService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

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
        Log.i(CLASS_TAG, "OnCreate executed");
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
        //return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("KService", "OnDestroy called.");
        unregisterReceiver(koalaManager.kComm);
        koalaManager.shutdown();
    }


}

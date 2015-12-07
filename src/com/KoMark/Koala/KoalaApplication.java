package com.KoMark.Koala;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.KoMark.Koala.core.KoalaManager;
import com.KoMark.Koala.core.KoalaService;
import com.KoMark.Koala.ui.MainActivity;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KoalaApplication extends Application {

    private final String CLASS_TAG = "KApplication";
    private KoalaManager koalaManager;
    private static Context context;
    private KoalaService kService = null;
    private ServiceConnection servConn;
    private MainActivity activityContext;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(CLASS_TAG, "onCreate");
        context = this;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(CLASS_TAG, "OnTerminate");
        //koalaManager.shutdown();
    }

    public KoalaManager getKoalaManager() {
        return koalaManager;
    }

    public void setKManager(KoalaManager KManager) {
        this.koalaManager = KManager;
    }

    public void setActivityContext(MainActivity activityContext) {
        this.activityContext = activityContext;
    }

    public void onResume() {
        servConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(CLASS_TAG, "Service connected: "+name);
                kService = ((KoalaService.KServiceBinder)service).getService();
                kService.initKManager(context);
                activityContext.setListeners();
                activityContext.resumeKChart();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(CLASS_TAG, "Service disconnected: "+name);
            }
        };

        startService((new Intent(this, KoalaService.class)).setFlags(Intent.FLAG_RECEIVER_FOREGROUND));
        bindService(new Intent(this, KoalaService.class).setFlags(Intent.FLAG_RECEIVER_FOREGROUND), servConn, BIND_AUTO_CREATE);
    }
}

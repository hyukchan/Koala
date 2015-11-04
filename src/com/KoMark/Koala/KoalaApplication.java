package com.KoMark.Koala;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import com.KoMark.Koala.core.KoalaManager;
import com.KoMark.Koala.core.KoalaService;

/**
 * Created by Hyukchan on 03/11/2015.
 */
public class KoalaApplication extends Application {

    private KoalaManager koalaManager;
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        koalaManager = new KoalaManager();
        koalaManager.initializeComponents(this);

        startService(new Intent(this, KoalaService.class));
    }

    public KoalaManager getKoalaManager() {
        return koalaManager;
    }


}

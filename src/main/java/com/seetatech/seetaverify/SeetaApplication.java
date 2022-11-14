package com.seetatech.seetaverify;

import android.app.Application;

import com.df.lib_seete6.utils.EnginHelper;


public class SeetaApplication extends Application {
    private static SeetaApplication instance;

    public static SeetaApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        new Thread(() -> {
            EnginHelper.getInstance().initEngine(this, true);
        }).start();
    }
}

package com.seetatech.seetaverify;

import android.app.Application;


public class SeetaApplication extends Application {
    private static SeetaApplication instance;

    public static SeetaApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }


}

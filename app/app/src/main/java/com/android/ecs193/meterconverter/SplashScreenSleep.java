package com.android.ecs193.meterconverter;

import android.app.Application;
import android.os.SystemClock;

public class SplashScreenSleep extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SystemClock.sleep(3000);
    }
}

package com.android.mechanicalspeedometerconverter;

import android.app.Application;
import android.os.SystemClock;

public class SplashScreenSleep extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SystemClock.sleep(3000);
    }
}

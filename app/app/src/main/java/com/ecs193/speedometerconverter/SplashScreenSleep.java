package com.ecs193.speedometerconverter;

import android.app.Application;
import android.os.SystemClock;

public class SplashScreenSleep extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SystemClock.sleep(3000);
    }
}

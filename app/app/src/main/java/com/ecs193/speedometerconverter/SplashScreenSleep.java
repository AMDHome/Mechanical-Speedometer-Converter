package com.ecs193.speedometerconverter;

import android.app.Application;
import android.os.SystemClock;

public class SplashScreenSleep extends Application {
    private int speedometer_half;

    public Integer getspeedometerHalf() {
        return speedometer_half;
    }

    public void setspeedometerHalf(int speedometer_half) {
        this.speedometer_half = speedometer_half;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setspeedometerHalf(10);
        SystemClock.sleep(3000);
    }
}

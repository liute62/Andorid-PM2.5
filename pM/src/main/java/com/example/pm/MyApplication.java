package com.example.pm;

import android.app.Application;
import android.app.Service;
import android.os.Vibrator;

import com.baidu.mapapi.SDKInitializer;

/**
 * Created by liuhaodong1 on 15/11/14.
 */
public class MyApplication extends Application {

    public Vibrator mVibrator;

    @Override
    public void onCreate() {
        super.onCreate();
        mVibrator = (Vibrator) getApplicationContext().getSystemService(Service.VIBRATOR_SERVICE);
        SDKInitializer.initialize(getApplicationContext());
    }

}

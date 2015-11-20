package com.example.pm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

import app.utils.SharedPreferencesUtil;
import app.utils.ShortcutUtil;

public class FirstActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_first);
        mHandler.sendEmptyMessageDelayed(0, 1000);
    }

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            boolean isAlreadyInit = SharedPreferencesUtil.getBooleanValue(
                    getApplicationContext(),
                    "isAlreadyInit"
                            + ShortcutUtil.getAppVersionCode(FirstActivity.this));
            if (!isAlreadyInit) {
                ShortcutUtil.createShortCut(FirstActivity.this,
                        R.drawable.ic_launcher, R.string.app_name);
                startActivity(new Intent(getApplicationContext(),
                        NotificationActivity.class));
                SharedPreferencesUtil
                        .setValue(
                                FirstActivity.this,
                                "isAlreadyInit"
                                        + ShortcutUtil.getAppVersionCode(FirstActivity.this),
                                true);
            } else {
                startActivity(new Intent(FirstActivity.this,
                        MainActivity.class));
            }
            FirstActivity.this.finish();
        }
    };
}

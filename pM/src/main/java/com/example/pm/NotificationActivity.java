package com.example.pm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import app.utils.ACache;
import app.utils.Const;
import app.utils.SharedPreferencesUtil;
import app.utils.ShortcutUtil;
import app.view.widget.DialogInputWeight;

public class NotificationActivity extends Activity implements View.OnClickListener {

    Button mSure;
    Button mCancel;
    ACache aCache;

    Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == Const.Handler_Input_Weight) {
              SharedPreferencesUtil
                     .setValue(getBaseContext(), "isAlreadyInit"
                             + ShortcutUtil.getAppVersionCode(getBaseContext()), true);
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                aCache = ACache.get(NotificationActivity.this.getApplicationContext());
                aCache.put(Const.Cache_User_Weight, (String)msg.obj, 10000000);
                Log.e("NotificationActivity", (String) msg.obj);
                startActivity(intent);
                NotificationActivity.this.finish();
         }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        aCache = ACache.get(this);
        mSure = (Button) findViewById(R.id.notification_sure);
        mCancel = (Button) findViewById(R.id.notification_cancel);
        mSure.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.notification_sure:
                DialogInputWeight dialogInputWeight = new DialogInputWeight(NotificationActivity.this,mHandler);
                dialogInputWeight.show();
                break;
            case R.id.notification_cancel:
                SharedPreferencesUtil
                        .setValue(getBaseContext(), "isAlreadyInit"
                                + ShortcutUtil.getAppVersionCode(getBaseContext()), false);
                this.finish();
                break;
        }
    }
}

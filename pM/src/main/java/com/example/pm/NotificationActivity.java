package com.example.pm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import app.utils.ACache;
import app.utils.SharedPreferencesUtil;
import app.utils.ShortcutUtil;

public class NotificationActivity extends Activity implements View.OnClickListener{

    Button mSure;
    Button mCancel;
    ACache aCache;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        aCache = ACache.get(this);
        mSure = (Button)findViewById(R.id.notification_sure);
        mCancel = (Button)findViewById(R.id.notification_cancel);
        mSure.setOnClickListener(this);
        mCancel.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.notification_sure:
                SharedPreferencesUtil
                        .setValue(getBaseContext(), "isAlreadyInit"
                                + ShortcutUtil.getAppVersionCode(getBaseContext()), true);
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
                NotificationActivity.this.finish();
                break;
            case R.id.notification_cancel:
                this.finish();
                break;
        }
    }
}

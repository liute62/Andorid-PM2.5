package com.example.pm;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import app.utils.SharedPreferencesUtil;
import app.utils.ShortcutUtil;

public class FirstActivity extends Activity{

    JudgeTask mTask;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_first);
        Log.e("FirstActivity","Oncreate");
        mTask = new JudgeTask();
        mTask.execute("execute");
        Log.e("FirstActivity","Oncreate2");
    }

    private class JudgeTask extends AsyncTask<String,Integer,String> {

        @Override
        protected String doInBackground(String... arg0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e("FirstActivity","doing");
            mHandler.sendEmptyMessage(1);
            return null;
        }

    }

    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            Log.e("FirstActivity","Handle Msg");
//            boolean isAlreadyInit = SharedPreferencesUtil.getBooleanValue(
//                    getApplicationContext(),
//                    "isAlreadyInit"
//                            + ShortcutUtil.getAppVersionCode(FirstActivity.this));
            boolean isAlreadyInit = true;
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
                startActivity(new Intent(getApplicationContext(),
                        MainActivity.class));
            }
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.e("onResume","1");
    }

    @Override
    protected void onDestroy() {
        mTask = null;
        super.onDestroy();
        Log.e("onDestroy","2");
    }
}

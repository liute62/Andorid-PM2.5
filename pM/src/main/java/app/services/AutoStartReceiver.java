package app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import app.utils.FileUtil;

/**
 * Created by liuhaodong1 on 16/6/2.
 */
public class AutoStartReceiver extends BroadcastReceiver {

    public static final String TAG = "AutoStartReceiver";

    //BackgroundService alarm = BackgroundService.getInstance();

    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
        {
            FileUtil.appendStrToFile("auto started");
            BackgroundService alarm = BackgroundService.getInstance(context);
            alarm.SetAlarm(context);
        }
    }
}

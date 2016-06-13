package app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.utils.FileUtil;

/**
 * Created by liuhaodong1 on 16/6/2.
 * This is a receiver to receive auto start instructions.
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
        }
    }
}

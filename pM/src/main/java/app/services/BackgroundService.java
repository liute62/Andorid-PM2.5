package app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import app.utils.Const;
import app.utils.FileUtil;
import app.utils.ShortcutUtil;
import app.utils.StableCache;

/**
 * Created by liuhaodong1 on 16/6/2.
 * This service is intended to running on background and do some discrete tasks.
 * Every time there is only one background service running
 */
public class BackgroundService extends BroadcastReceiver {

    public static final String TAG = "BackgroundService";

    private static BackgroundService instance;

    public final long Max_Interval = 1000 * 30; //maximum interval between wakelock acquire and release, currently 30s

    long startTime = 0;

    int repeatingCycle = 0; //

    StableCache cache; //

    PowerManager.WakeLock wakeLock = null;

    Context mContext = null;

    Handler handler = new Handler();

    boolean isRunning = false; //if the background process is running.

    boolean isFinished = false;

    LocationService locationService;

    MotionService motionService;

    Runnable process = new Runnable() {
        @Override
        public void run() {
            if(!isFinished)
                processInner();
            checkUsingMuchTime();
            if(isFinished){
                if(wakeLock != null)
                  wakeLock.release();
                isRunning = false;
            }else {
                handler.postDelayed(process, 1000); //every 1 second
            }
        }
    };

    private BackgroundService(Context context){
        mContext = context;
        cache = StableCache.getInstance(mContext);
    }

    public static BackgroundService getInstance(Context context){
        if(instance == null)
            instance = new BackgroundService(context);
        return instance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        FileUtil.appendStrToFile("onReceive");
        //SetAlarm();
        Log.e(TAG,"onReceive");
        if(!isRunning) {
            mContext = context;
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backgroundWake");
            wakeLock.acquire();
            startInner();
        }
    }

    private void startInner(){
        startTime = System.currentTimeMillis();
        processInner();
        saveValues();
        onFinished();
        //handler.post(process);
    }

    private void processInner(){
        String tmp = cache.getAsString(Const.Cache_Repeating_Time);
        if(ShortcutUtil.isStringOK(tmp)){
            repeatingCycle = Integer.valueOf(tmp);
        }else {
            repeatingCycle = 1;
            cache.put(Const.Cache_Repeating_Time,repeatingCycle);
        }
    }

    private void saveValues(){
        repeatingCycle++;
        cache.put(Const.Cache_Repeating_Time,repeatingCycle);
        FileUtil.appendStrToFile(-10000,"cycle "+repeatingCycle);
    }

    private void onFinished(){
        isFinished = true;
        wakeLock.release();
        isRunning = false;
    }

    private void checkUsingMuchTime(){
        if(System.currentTimeMillis() - startTime > Max_Interval) isFinished = true;
    }

    public void SetAlarm(Context context)
    {
        FileUtil.appendStrToFile("setAlarm");
        Log.e(TAG,"SetAlarm");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //Intent i = new Intent(context, BackgroundService.class);
        Intent i = new Intent(context,BackgroundService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
       // am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 6, pi); // Millisec * Second * Minute
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 3000, pi);
    }

    public void CancelAlarm(Context context)
    {
        Intent intent = new Intent(context, BackgroundService.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}

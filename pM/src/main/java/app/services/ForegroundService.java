package app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.example.pm.MainActivity;
import com.example.pm.R;

import app.Entity.State;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DataCalculator;
import app.utils.FileUtil;
import app.utils.ShortcutUtil;


/**
 * Created by liuhaodong1 on 15/11/10.
 * This is a service running in the foreground intended to update chart result and other GUI part
 */
public class ForegroundService extends Service {

    public static final String TAG = "app.services.ForegroundService";

    /**
     * for data operations
     */
    private ACache aCache;
    private DataServiceUtil dataServiceUtil = null;
    /**
     * for PM State
     */
    private double longitude;  //the newest longitude
    private double latitude;  // the newest latitude
    /**
     * for cycling
     */
    private int DBRunTime;
    private final int State_TooMuch = 600;
    private int DB_Chart_Loop = 12;
    private boolean isPMSearchSuccess;
    private boolean isRefreshRunning;
    private String isBackground = null;
    private final static String bgStr = "false";

    /**
     * for indoor and outdoor judgement
     **/
    //private InOutdoorService inOutdoorService;

    /**
     * for motion detection
     */
    MotionServiceUtil motionServiceUtil;

    private volatile HandlerThread mHandlerThread;
    private Handler refreshHandler;

    Handler DBHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DBRunnable.run();
        }
    };

    private Runnable DBRunnable = new Runnable() {

        Intent intentText;
        Intent intentChart;

        @Override
        public void run() {

            int runTimeInterval = Const.DB_Run_Time_INTERVAL;
            /***** DB Run First time *****/
            if (DBRunTime == 0) {   //The initial state, set cache for chart

                intentChart = new Intent(Const.Action_Chart_Cache);
                SQLiteDatabase db = dataServiceUtil.getDBHelper().getReadableDatabase();
                DataCalculator.getIntance(db).updateLastTwoHourState();
                DataCalculator.getIntance(db).updateLastDayState();
                DataCalculator.getIntance(db).updateLastWeekState();
                aCache.put(Const.Cache_Chart_1, DataCalculator.getIntance(db).calChart1Data());
                aCache.put(Const.Cache_Chart_2, DataCalculator.getIntance(db).calChart2Data());
                aCache.put(Const.Cache_Chart_3, DataCalculator.getIntance(db).calChart3Data());
                aCache.put(Const.Cache_Chart_4, DataCalculator.getIntance(db).calChart4Data());
                aCache.put(Const.Cache_Chart_5, DataCalculator.getIntance(db).calChart5Data());
                aCache.put(Const.Cache_Chart_6, DataCalculator.getIntance(db).calChart6Data());
                if (aCache.getAsObject(Const.Cache_Chart_7) == null) {
                    aCache.put(Const.Cache_Chart_7, DataCalculator.getIntance(db).calChart7Data());
                    aCache.put(Const.Cache_Chart_7_Date, DataCalculator.getIntance(db).getLastWeekDate());
                }
                aCache.put(Const.Cache_Chart_8, DataCalculator.getIntance(db).calChart8Data());
                aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                aCache.put(Const.Cache_Chart_10, DataCalculator.getIntance(db).calChart10Data());
                if (aCache.getAsObject(Const.Cache_Chart_12) == null) {
                    aCache.put(Const.Cache_Chart_12, DataCalculator.getIntance(db).calChart12Data());
                    aCache.put(Const.Cache_Chart_12_Date, DataCalculator.getIntance(db).getLastWeekDate());
                }
                sendBroadcast(intentChart);
                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                // searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));

            }
            isBackground = aCache.getAsString(Const.Cache_Is_Background);
            if (isBackground == null) { //App first run
                isBackground = "false";
                aCache.put(Const.Cache_Is_Background, isBackground);
                String userId = aCache.getAsString(Const.Cache_User_Id);
                if (userId == null) aCache.put(Const.Cache_User_Id, "0");
            }

            if (isBackground.equals("false")) {
                runTimeInterval = Const.DB_Run_Time_INTERVAL;
                /** notify user whether using the old PM2.5 density **/

                if ((longitude == 0.0 && latitude == 0.0) || !isPMSearchSuccess) {
                    Intent intent = new Intent(Const.Action_DB_Running_State);
                    intent.putExtra(Const.Intent_DB_Run_State, 1);
                    sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(Const.Action_DB_Running_State);
                    intent.putExtra(Const.Intent_DB_Run_State, 0);
                    sendBroadcast(intent);
                }
                if (DBRunTime % 5 == 0) {
                    NotifyServiceUtil.notifyLocationChanged(ForegroundService.this, latitude, longitude);
                }
                /***** DB Running Normally *****/
                State state = dataServiceUtil.getCurrentState();
                if(state == null) return;
                if (DBRunTime == 0) { //Initialize the state when DB start
                    DBRunTime = 1;

                }
                if (state.getId() > State_TooMuch) DB_Chart_Loop = 24;
                else DB_Chart_Loop = 12;
                Bundle mBundle = new Bundle();
                SQLiteDatabase db = dataServiceUtil.getDBHelper().getReadableDatabase();
                switch (DBRunTime % DB_Chart_Loop) { //Send chart data to mainfragment
                    case 3:
                        UpdateServiceUtil.run(getApplicationContext(), aCache, dataServiceUtil.getDBHelper());
                        break;
                    case 5:
                        intentChart = new Intent(Const.Action_Chart_Result_1);
                        DataCalculator.getIntance(db).updateLastTwoHourState();
                        mBundle.putSerializable(Const.Intent_chart4_data, DataCalculator.getIntance(db).calChart4Data());
                        mBundle.putSerializable(Const.Intent_chart5_data, DataCalculator.getIntance(db).calChart5Data());
                        mBundle.putSerializable(Const.Intent_chart8_data, DataCalculator.getIntance(db).calChart8Data());
                        aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                        intentChart.putExtras(mBundle);
                        sendBroadcast(intentChart);
                        break;
                    case 7:
                        intentChart = new Intent(Const.Action_Chart_Result_2);
                        DataCalculator.getIntance(db).updateLastDayState();
                        Bundle mBundle2 = new Bundle();
                        mBundle2.putSerializable(Const.Intent_chart1_data, DataCalculator.getIntance(db).calChart1Data());
                        mBundle2.putSerializable(Const.Intent_chart2_data, DataCalculator.getIntance(db).calChart2Data());
                        mBundle2.putSerializable(Const.Intent_chart3_data, DataCalculator.getIntance(db).calChart3Data());
                        mBundle2.putSerializable(Const.Intent_chart6_data, DataCalculator.getIntance(db).calChart6Data());
                        mBundle2.putSerializable(Const.Intent_chart10_data, DataCalculator.getIntance(db).calChart10Data());
                        intentChart.putExtras(mBundle2);
                        sendBroadcast(intentChart);
                        break;
                    case 10:
                        intentChart = new Intent(Const.Action_Chart_Result_3);
                        DataCalculator.getIntance(db).updateLastWeekState();
                        Bundle mBundle3 = new Bundle();
                        mBundle3.putSerializable(Const.Intent_chart7_data, DataCalculator.getIntance(db).calChart7Data());
                        mBundle3.putSerializable(Const.Intent_chart_7_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                        mBundle3.putSerializable(Const.Intent_chart12_data, DataCalculator.getIntance(db).calChart12Data());
                        mBundle3.putSerializable(Const.Intent_chart_12_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                        intentChart.putExtras(mBundle3);
                        sendBroadcast(intentChart);
                        break;
                }

                //every 5 second to check and to update the text in Mainfragment, even though there is no newly data calculated.
                int mul = 1;
                if (state != null && state.getId() > State_TooMuch) {
                    mul = 2;
                }
                //to much data here, we need to slow it down, every 1 min to check it
                intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                if (DBRunTime % (3 * mul) == 0) { //15s 30s
                    intentText.putExtra(Const.Intent_DB_PM_Hour, DataCalculator.getIntance(db).calLastHourPM());
                }
                if (DBRunTime % (6 * mul) == 0) {//30s 1min
                    intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                }
                if (DBRunTime % (12 * mul) == 0) {//1min 2min
                    intentText.putExtra(Const.Intent_DB_PM_Week, DataCalculator.getIntance(db).calLastWeekAvgPM());
                }
                sendBroadcast(intentText);
            } else {
                //using a more soft way to notify user that DB is not running
                Intent intent = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State, -1);
                sendBroadcast(intent);
            }

            //every 10 min to open the GPS and if get the last location, close it.
//            if (DBRunTime % 120 == 0) { //120 240 360, 480, 600, 720
//                FileUtil.appendStrToFile(DBRunTime, "the location service open and get in/outdoor state");
//                locationService.run(LocationService.TYPE_BAIDU);
//                Loc_Runtime = 0;
//            }
            // Loc_Runtime++;
//            if (Loc_Runtime >= DB_Loc_Close_Per) {
//                Loc_Runtime = Integer.MIN_VALUE;
//                FileUtil.appendStrToFile(DBRunTime, "the location service close");
//                locationService.stop();
//                int tmp = locationService.getIndoorOutdoor();
//                inOutDoor = tmp;
//                aCache.put(Const.Cache_Indoor_Outdoor, String.valueOf(inOutDoor));
//                FileUtil.appendStrToFile(DBRunTime, "stop getting the in/outdoor state with state == " + inOutDoor);
//            }
            //every 1 min to calculate the pm result
//            if (DBRunTime % 12 == 0) {
//               // State last = state;
//                //state = calculatePM25(longitude, latitude);
//                State now = state;
//                if (!isSurpass(last, now)) {
//                    dataService.insertState(state);
//                } else {
//                    reset();
//                }
//            }
            //every 1 hour to check if it need to search the PM density from server
//            String lastTime = aCache.getAsString(Const.Cache_DB_Lastime_searchDensity);
//            if (!ShortcutUtil.isStringOK(lastTime)) {
//                lastTime = String.valueOf(System.currentTimeMillis());
//                aCache.put(Const.Cache_DB_Lastime_searchDensity, lastTime);
//            }
//            Long curTime = System.currentTimeMillis();
//            if (curTime - Long.valueOf(lastTime) > Const.Min_Search_PM_Time) {
//                FileUtil.appendStrToFile(DBRunTime, "every 1 hour to search the PM density from server");
//                aCache.put(Const.Cache_DB_Lastime_searchDensity, String.valueOf(System.currentTimeMillis()));
//                searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
//            }
            //every 1 hour to check if some data need to be uploaded
//            String lastUploadTime = aCache.getAsString(Const.Cache_DB_Lastime_Upload);
//            if (!ShortcutUtil.isStringOK(lastUploadTime)) {
//                lastUploadTime = String.valueOf(System.currentTimeMillis());
//                aCache.put(Const.Cache_DB_Lastime_Upload, lastUploadTime);
//            }
            //every 1 hour to check pm data for upload
//            if (curTime - Long.valueOf(lastUploadTime) > Const.Min_Upload_Check_Time) {
//                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
//                FileUtil.appendStrToFile(DBRunTime, "every 1 hour to check pm data for upload");
//                //check it user have login
//                checkPMDataForUpload();
//            }
            DBRunTime++;
            if (DBRunTime >= 721) DBRunTime = 1; //1/5s, 12/min 720/h 721` a cycle

            DBHandler.postDelayed(DBRunnable, runTimeInterval);
        }
    };

    private void initialThread() {
        mHandlerThread = new HandlerThread("DBHandlerThread");
        mHandlerThread.start();
        refreshHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Intent intentChart;
                Bundle mBundle = new Bundle();
                SQLiteDatabase db = dataServiceUtil.getDBHelper().getReadableDatabase();
                State state = dataServiceUtil.getCurrentState();
                if (msg.what == Const.Handler_Refresh_Text) {
                    if (state == null) return;
                    DataCalculator.getIntance(db).updateLastTwoHourState();
                    DataCalculator.getIntance(db).updateLastWeekState();
                    Intent intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                    intentText.putExtra(Const.Intent_DB_PM_Hour, DataCalculator.getIntance(db).calLastHourPM());
                    intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                    intentText.putExtra(Const.Intent_DB_PM_Week, DataCalculator.getIntance(db).calLastWeekAvgPM());
                    sendBroadcast(intentText);
                } else if (msg.what == Const.Handler_Refresh_Chart1) {
                    intentChart = new Intent(Const.Action_Chart_Result_1);
                    DataCalculator.getIntance(db).updateLastTwoHourState();
                    mBundle.putSerializable(Const.Intent_chart4_data, DataCalculator.getIntance(db).calChart4Data());
                    mBundle.putSerializable(Const.Intent_chart5_data, DataCalculator.getIntance(db).calChart5Data());
                    mBundle.putSerializable(Const.Intent_chart8_data, DataCalculator.getIntance(db).calChart8Data());
                    aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                    intentChart.putExtras(mBundle);
                    sendBroadcast(intentChart);
                } else if (msg.what == Const.Handler_Refresh_Chart2) {
                    intentChart = new Intent(Const.Action_Chart_Result_2);
                    DataCalculator.getIntance(db).updateLastDayState();
                    mBundle.putSerializable(Const.Intent_chart1_data, DataCalculator.getIntance(db).calChart1Data());
                    mBundle.putSerializable(Const.Intent_chart2_data, DataCalculator.getIntance(db).calChart2Data());
                    mBundle.putSerializable(Const.Intent_chart3_data, DataCalculator.getIntance(db).calChart3Data());
                    mBundle.putSerializable(Const.Intent_chart6_data, DataCalculator.getIntance(db).calChart6Data());
                    mBundle.putSerializable(Const.Intent_chart10_data, DataCalculator.getIntance(db).calChart10Data());
                    intentChart.putExtras(mBundle);
                    sendBroadcast(intentChart);
                } else if (msg.what == Const.Handler_Refresh_Chart3) {
                    intentChart = new Intent(Const.Action_Chart_Result_3);
                    DataCalculator.getIntance(db).updateLastWeekState();
                    mBundle.putSerializable(Const.Intent_chart7_data, DataCalculator.getIntance(db).calChart7Data());
                    mBundle.putSerializable(Const.Intent_chart_7_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                    mBundle.putSerializable(Const.Intent_chart12_data, DataCalculator.getIntance(db).calChart12Data());
                    mBundle.putSerializable(Const.Intent_chart_12_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                    intentChart.putExtras(mBundle);
                    sendBroadcast(intentChart);
                    isRefreshRunning = false;
                    Toast.makeText(ForegroundService.this.getApplicationContext(), Const.Info_Refresh_Chart_Success, Toast.LENGTH_SHORT).show();
                }
            }
        };

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FileUtil.appendStrToFile(TAG + "OnCreate");
        initialThread();
        longitude = 0.0;
        latitude = 0.0;
        DBRunTime = 0;
        //isPMSearchSuccess = false;
        isRefreshRunning = false;
        dataServiceUtil = DataServiceUtil.getInstance(this);
        aCache = ACache.get(getApplicationContext());
        registerAReceiver();
        /**
         * init motion detection
         */
        //motionService = MotionService.getInstance(this);
        //motionService.start();

        serviceStateInitial();
        DBHandler.sendEmptyMessageDelayed(0, 10000);//10s
        BackgroundService.SetAlarm(this);
    }

    @Override
    public void onDestroy() {
        FileUtil.appendStrToFile(TAG + " onDestory");
        motionServiceUtil.stop();
        mHandlerThread.quit();
        super.onDestroy();
        DBRunnable = null;
    }

    private void registerAReceiver() {
        Receiver receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.Action_Search_Density_ToService);
        filter.addAction(Const.Action_Bluetooth_Hearth);
        filter.addAction(Const.Action_Get_Location_ToService);
        filter.addAction(Const.Action_Refresh_Chart_ToService);
        this.registerReceiver(receiver, filter);
    }

    private void serviceStateInitial() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.app_service_running))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true);
        startForeground(12450, mBuilder.build());
    }

    /**
     *
     */
    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Const.Action_Bluetooth_Hearth)) {
                String hearthStr = intent.getStringExtra(Const.Intent_Bluetooth_HearthRate);
                if (ShortcutUtil.isStringOK(hearthStr)) {
                    FileUtil.appendStrToFile(DBRunTime, "using hearth rate from bluetooth " + hearthStr);
                    try {
                        int rate = Integer.valueOf(hearthStr);
                        //avg_rate = String.valueOf(rate);
                    } catch (Exception e) {
                        //avg_rate = "12";
                    }
                }
            } else if (intent.getAction().equals(Const.Action_Search_Density_ToService)) {
                Intent intentTmp = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State, 0);
                sendBroadcast(intentTmp);
                isPMSearchSuccess = true;
                double PM25Density = intent.getDoubleExtra(Const.Intent_PM_Density, 0.0);
                if (PM25Density != 0.0)
                    aCache.put(Const.Cache_PM_Density, PM25Density);
            } else if (intent.getAction().equals(Const.Action_Get_Location_ToService)) {
                double lati = intent.getDoubleExtra(Const.Intent_DB_PM_Lati, 0.0);
                double longi = intent.getDoubleExtra(Const.Intent_DB_PM_Longi, 0.0);
                if (lati != 0.0 && longi != 0.0) {
                    latitude = lati;
                    longitude = longi;
                }
            } else if (intent.getAction().equals(Const.Action_Refresh_Chart_ToService)) {
                //when open the phone, check if it need to refresh.
                if (!isRefreshRunning) {
                    isRefreshRunning = true;
                    refreshAll();
                }
            }
        }
    }

    private void refreshAll() {
        refreshHandler.sendEmptyMessage(Const.Handler_Refresh_Text);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart1, 2000);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart2, 3000);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart3, 4000);
        NotifyServiceUtil.notifyCityChanged(this, dataServiceUtil.getLatitude(), dataServiceUtil.getLongitude());
        //if (!isPMSearchSuccess)
        //searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
    }
}

package app.services;

import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pm.MainActivity;
import com.example.pm.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.model.PMModel;
import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBConstants;
import app.utils.DBHelper;
import app.utils.DataCalculator;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 15/11/10.
 * //need update ....
 * DBService Sequences
 * -----Oncreate-----
 * 1.params Initial.
 * 2.Get default PM density from cache.
 * 3.DB Initial: get the value of last hour, today, last week avg pm from database and set cache.
 * 4.Sensor Initial
 * 5.GPS Initial:
 * Location Initial:
 * 1.Success, lati&longi are set to  getLastKnownLocation
 * 2.Failed,  lati&longi are set to  Shanghai location
 * For location changed:
 * 1.last time lati&longi equals current lati&longi, means no change, don't need search result from server.
 * 2.If location changed, download pm density from server.
 * 5.DB Runnable begin running.
 * -----DB Runnable-----
 * 0. first time running after app installed, set cache to isbackground = false, that means currently App running in front
 * 1. DB running start, and DBRuntime == 0, get the newest data for chart, set cache and update GUI
 * 2. if DB Can Run, that means we could get the location successfully, otherwise DB couldn't run and we tell user, currently DB is not running
 * 3. if DB == 0, initialize the state, and set DB Runtime = 1, The range of DBRuntime is always between 1 and 1 * 12 * 60 (1 hour)
 * 4. For Chart, if DBRuntime =
 * 5. Every a minute to calculate PM Data and upload it
 * 1. if upload success, change the value of upload to 1, and insert it into DB
 * 2. if upload failed, no change and insert it into DB directly
 * 6. Every a DBRuntime to update the GUI about PM Info Text, no matter there are newly data calculated or not.
 * 7. Every a hour to upload the data and set DBRuntime = 1
 */
public class DBService extends Service {

    public static final String TAG = "app.services.DBService";

    /**
     * data
     **/
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private ACache aCache;
    PMModel pmModel;
    State state;
    /**
     * PM State
     **/
    private double longitude;  //the newest longitude
    private double latitude;  // the newest latitude
    private int PM25Source; //1 or 2
    private double PM25Density;
    private double PM25Today;
    private Long IDToday;
    private double venVolToday;
    private String avg_rate;
    /****/
    private int DBRunTime;
    final int State_Much_Index = 500;
    int DB_Chart_Loop = 12;
    private boolean isPMSearchRunning;
    private boolean isPMSearchSuccess;
    private boolean DBCanRun;
    private boolean isLocationChanged;
    private boolean isUploadRunning;
    private boolean isRefreshRunning;
    private String isBackground = null;
    private final static String bgStr = "false";
    /**
     * Location
     **/
    LocationService locationService;
    Location mLastLocation;
    private boolean isGPSRun = false;
    /**
     * Sensor
     **/
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleStepDetector simpleStepDetector;
    private int numSteps;
    private int numStepsTmp; //to insert the step value to state
    private long time1;
    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;
    private final int Motion_Detection_Interval = 60 * 1000; //1min
    private final int Motion_Run_Thred = 100; //100 step / min
    private final int Motion_Walk_Thred = 20; // > 10 step / min -- walk
    /**
     * Wake the thread
     **/
    private PowerManager powerManager;
    PowerManager.WakeLock wakeLock = null;
    private boolean isSavingBattery;

    Handler DBHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if ((longitude == 0.0 && latitude == 0.0) && PM25Density == 0.0) {
                Log.e(TAG, "DBCanRun == False, longitude == 0.0 && latitude == 0.0 && PM25Density == 0.0");
                DBCanRun = false;
            } else {
                DBCanRun = true;
            }
            if (DBRunnable != null) {
                DBRunnable.run();
            }
        }
    };

    Handler refreshHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Intent intentChart;
            Bundle mBundle = new Bundle();
            Log.e(TAG, "refreshHandler " + msg.what + " " + ShortcutUtil.refFormatDateAndTime(System.currentTimeMillis()));
            if(msg.what == Const.Handler_Refresh_Text){
                Intent intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                intentText.putExtra(Const.Intent_DB_PM_Hour, calLastHourPM());
                intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                intentText.putExtra(Const.Intent_DB_PM_Week, calLastWeekAvgPM());
                sendBroadcast(intentText);
            }else if (msg.what == Const.Handler_Refresh_Chart1) {
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
                Toast.makeText(DBService.this.getApplicationContext(), Const.Info_Refresh_Chart_Success, Toast.LENGTH_SHORT).show();
            }
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
                if (state != null && state.getId() > State_Much_Index) {
                    //so many data stored, don't want to refresh every time after starting
                } else {
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
                }

            }
            if(wakeLock == null){
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
                wakeLock.acquire();
            }
            isBackground = aCache.getAsString(Const.Cache_Is_Background);
            String userId = aCache.getAsString(Const.Cache_User_Id);
            if (isBackground == null) { //App first run
                isBackground = "false";
                aCache.put(Const.Cache_Is_Background, isBackground);
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
                    intentText = new Intent(Const.Action_DB_MAIN_Location);
                    intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
                    intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
                    sendBroadcast(intentText);
                }
                /***** DB Running Normally *****/
                if (DBCanRun) {

                    if (DBRunTime == 0) { //Initialize the state when DB start
                        if (state != null && state.getId() > State_Much_Index) {
                            DBRunTime = 1;
                        } else {
                            state = calculatePM25(longitude, latitude);
                            state.print();
                            DBRunTime = 1;
                        }
                    }
                    //Log.e(TAG,"isBackground == "+isBackground);
                    if (state.getId() > State_Much_Index) DB_Chart_Loop = 24;
                    else DB_Chart_Loop = 12;
                    Bundle mBundle = new Bundle();
                    switch (DBRunTime % DB_Chart_Loop) { //Send chart data to mainfragment
                        case 1:
                            Log.e(TAG,"check for upload start");
                            checkPMDataForUpload();
                            Log.e(TAG, "check for upload end");
                            break;
                        case 3:
                            Log.e(TAG,"check  for update start");
                            UpdateService.run(getApplicationContext(), aCache, dbHelper);
                            Log.e(TAG, "check  for update end");
                            break;
                        case 5:
                            Bundle mBundle = new Bundle();
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
                    if (state != null && state.getId() > State_Much_Index) {
                        mul = 2;
                    }
                    //to much data here, we need to slow it down, every 1 min to check it
                    intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                    if (DBRunTime % (3 * mul) == 0) { //15s 30s
                        intentText.putExtra(Const.Intent_DB_PM_Hour, calLastHourPM());
                    }
                    if (DBRunTime % (6 * mul) == 0) {//30s 1min
                        intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                    }
                    if (DBRunTime % (12 * mul) == 0) {//1min 2min
                        intentText.putExtra(Const.Intent_DB_PM_Week, calLastWeekAvgPM());
                    }
                    sendBroadcast(intentText);
                } else {
                    //using a more soft way to notify user that DB is not running
                    Intent intent = new Intent(Const.Action_DB_Running_State);
                    intent.putExtra(Const.Intent_DB_Run_State, -1);
                    sendBroadcast(intent);
                }
            }

                //every 10 min to open the GPS and if get the last location, close it.
                if (DBRunTime % 120 == 0) { //120 * 5s = 10min
                    //FileUtil.appendStrToFile(DBRunTime, "Add status listener and request location Updates");
                    if(!isSavingBattery)
                        locationService.run(LocationService.TYPE_BAIDU);
                }
                if (DBRunTime % 130 == 0) { //open for 10 * 5 = 50s
                    //FileUtil.appendStrToFile(DBRunTime, "remove status listener, remove request location Updates");
                    locationService.stop();
                }

                //every 1 min to calculate the pm result
                if (DBRunTime % 12 == 0) {
                    State last = state;
                    state = calculatePM25(longitude, latitude);
                    State now = state;
                    if (!isSurpass(last, now)) {
                        //uploadPMData(state); //no upload action
                        insertState(state);
                    } else {
                        //TODO Check if Runtime logic success
                        reset();
                    }
                }
                //every 1 hour to check if it need to search the PM density from server
                String lastTime = aCache.getAsString(Const.Cache_DB_Lastime_searchDensity);
                if (!ShortcutUtil.isStringOK(lastTime)) {
                    aCache.put(Const.Cache_DB_Lastime_searchDensity, String.valueOf(System.currentTimeMillis()));
                    searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                } else {
                    Long curTime = System.currentTimeMillis();
                    if (curTime - Long.valueOf(lastTime) > Const.Min_Search_PM_Time) {
                        //FileUtil.appendStrToFile(DBRunTime, "every 1 hour to search the PM density from server");
                        aCache.put(Const.Cache_DB_Lastime_searchDensity, String.valueOf(System.currentTimeMillis()));
                        searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                    }
                }
                //every 1 hour to check if some data need to be uploaded
                String lastUploadTime = aCache.getAsString(Const.Cache_DB_Lastime_Upload);
                if (!ShortcutUtil.isStringOK(lastUploadTime))
                    aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                else {
                    Long curTime = System.currentTimeMillis();
                    //every 1 hour to check pm data for upload
                    if (curTime - Long.valueOf(lastUploadTime) > Const.Min_Upload_Check_Time) {
                        aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                        //FileUtil.appendStrToFile(DBRunTime, "every 1 hour to check pm data for upload");
                        //check it user have login
                        checkPMDataForUpload();
                    }
                }
                DBRunTime++;
                if (DBRunTime >= 721) DBRunTime = 1; //1/5s, 12/min 720/h 721` a cycle

            DBHandler.postDelayed(DBRunnable, runTimeInterval);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mLastLocation = null;
        PM25Source = 0;
        PM25Density = 0.0;
        longitude = 0.0;
        latitude = 0.0;
        DBCanRun = false;
        DBRunTime = 0;
        avg_rate = "12";
        isPMSearchRunning = false;
        isLocationChanged = false;
        isUploadRunning = false;
        isPMSearchSuccess = false;
        isRefreshRunning = false;
        isSavingBattery = false;
        aCache = ACache.get(getApplicationContext());
        locationService = LocationService.getInstance(this);
        locationService.setGetTheLocationListener(getTheLocation);
        locationService.getIndoorOutdoor();
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
        wakeLock.acquire();
        //todo each time to run the data and
        if (aCache.getAsString(Const.Cache_PM_Density) != null) {
            PM25Density = Double.valueOf(aCache.getAsString(Const.Cache_PM_Density));
            Log.e(TAG, "onCreate PM25 Density " + String.valueOf(PM25Density));
        }
        if (aCache.getAsString(Const.Cache_PM_Source) != null) {
            int source;
            try {
                source = Integer.valueOf(aCache.getAsString(Const.Cache_PM_Source));
            } catch (Exception e) {
                source = 0;
            }
            PM25Source = source;
        }
        registerAReceiver();
        locationInitial();
        DBInitial();
        serviceStateInitial();
        //sensorInitial();
        if (mLastLocation != null) {
            locationService.stop();
            Intent intentText = new Intent(Const.Action_DB_MAIN_Location);
            intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
            intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
            sendBroadcast(intentText);
        }
        String isSaving = aCache.getAsString(Const.Cache_Is_Saving_Battery);
        if(ShortcutUtil.isStringOK(isSaving) && isSaving.equals(Const.IS_SAVING_BATTERY))
            openSavingBattery();
        else closeSavingBattery();
        DBHandler.sendEmptyMessageDelayed(0, 15000);//15s
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

    @Override
    public void onDestroy() {
        FileUtil.appendStrToFile(-100,"DBService onDestory");
        if (wakeLock != null) wakeLock.release();
        super.onDestroy();
        DBRunnable = null;
    }

    private void DBInitial() {
        if (null==dbHelper) {
            dbHelper = new DBHelper(getApplicationContext());
        }
        db = dbHelper.getReadableDatabase();

        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);

        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();

        /**Get states of today **/
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        if (states.isEmpty()) {
            PM25Today = 0.0;
            venVolToday = 0.0;
            IDToday = Long.valueOf(0);
        } else {
            State state = states.get(states.size() - 1);
            state.print();
            PM25Today = Double.parseDouble(state.getPm25());
            venVolToday = Double.parseDouble(state.getVentilation_volume());
            IDToday = Long.valueOf(state.getId()) + 1;
        }
    }

    private void serviceStateInitial() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Bio3Air")
                        .setContentText("Service Running")
                        .setContentIntent(pendingIntent)
                        .setOngoing(true);
        startForeground(12450, mBuilder.build());
    }

    private void sensorInitial() {
        numSteps = 0;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(new StepListener() {
            @Override
            public void step(long timeNs) {
                //Log.d(TAG, "Time: " + ShortcutUtil.refFormatNowDate(timeNs) + " Step: " + String.valueOf(numSteps));
                numSteps++;
                numStepsTmp++;
            }
        });
        time1 = System.currentTimeMillis();
        mSensorManager.registerListener(sensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void locationInitial() {
        mLastLocation = locationService.getLastKnownLocation();
        if (mLastLocation != null) {
            isGPSRun = true;
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();
            //Log.d(TAG, "Location Service is running" + String.valueOf(latitude) + " " + String.valueOf(longitude));
            FileUtil.appendStrToFile(DBRunTime, "locationInitial getLastKnownLocation " + String.valueOf(latitude) + " " + String.valueOf(longitude));
            aCache.put(Const.Cache_Latitude, latitude);
            aCache.put(Const.Cache_Longitude, longitude);
            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
        } else {
            //new a thread to get the location
            String lati = aCache.getAsString(Const.Cache_Latitude);
            String longi = aCache.getAsString(Const.Cache_Longitude);
            if (ShortcutUtil.isStringOK(lati) && ShortcutUtil.isStringOK(longi)) {
                FileUtil.appendStrToFile(DBRunTime, "Using the cache location as default location" + lati + " " + longi);
                longitude = Double.valueOf(longi);
                latitude = Double.valueOf(lati);
                searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
            } else {
                FileUtil.appendStrToFile(DBRunTime, "locationInitial new a thread to get the location");
                locationService.run(LocationService.TYPE_BAIDU);
            }
        }
    }

    SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                simpleStepDetector.updateAccel(
                        event.timestamp, event.values[0], event.values[1], event.values[2]);
            }
            long time2 = System.currentTimeMillis();
            if (time2 - time1 > Motion_Detection_Interval) {
                if (numSteps > Motion_Run_Thred)
                    mMotionStatus = Const.MotionStatus.RUN;
                else if (numSteps <= Motion_Run_Thred && numSteps >= Motion_Walk_Thred)
                    mMotionStatus = Const.MotionStatus.WALK;
                else
                    mMotionStatus = Const.MotionStatus.STATIC;
                numSteps = 0;
                time1 = time2;
                //Log.v(TAG, "Motion Status: " + String.valueOf(mMotionStatus));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    LocationService.GetTheLocation getTheLocation = new LocationService.GetTheLocation() {
        @Override
        public void onGetLocation(Location location) {

        }

        @Override
        public void onSearchStop(Location location) {
            if (location != null) {
                isLocationChanged = true;
                mLastLocation = location;
                longitude = mLastLocation.getLongitude();
                latitude = mLastLocation.getLatitude();
                aCache.put(Const.Cache_Longitude, longitude);
                aCache.put(Const.Cache_Latitude, latitude);
                FileUtil.appendStrToFile(DBRunTime, "onSearchStop lati: " + latitude + " longi: " + longitude);
                searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
            }
        }
    };

    /**
     * density: (ug/m3)
     * breath:  (L/min)
     * Calculate today the number of pm2.5 breathed until now
     *
     * @param longi
     * @param lati
     * @return
     */
    private State calculatePM25(double longi, double lati) {
        Double breath = 0.0;
        Double density = PM25Density;
        boolean isConnected = ShortcutUtil.isNetworkAvailable(this);
//        double ratio = 1;
//        if (!isConnected) {
//            ratio = this.getLastSevenDaysInOutRatio();
//            density = ratio * density + (1-ratio)*density/3;
//            if (ratio>0.5) {
//                Const.CURRENT_OUTDOOR = 0;
//            } else {
//                Const.CURRENT_OUTDOOR = 1;
//            }
//        } else {
//            if (Const.CURRENT_OUTDOOR == 0) {
//                density /= 3;
//            }
//        }
        double static_breath = ShortcutUtil.calStaticBreath(aCache.getAsString(Const.Cache_User_Weight));
        if (static_breath == 0.0) {
            if(isBackground != null && isBackground.equals(bgStr))
                Toast.makeText(getApplicationContext(), Const.Info_Weight_Null, Toast.LENGTH_SHORT).show();
            static_breath = 6.6; // using the default one
        }
        Log.d(TAG, "Static Breath " + String.valueOf(static_breath));
        if (mMotionStatus == Const.MotionStatus.STATIC) {
            breath = static_breath;
        } else if (mMotionStatus == Const.MotionStatus.WALK) {
            breath = static_breath * 2.1;
        } else if (mMotionStatus == Const.MotionStatus.RUN) {
            breath = static_breath * 6;
        }
        venVolToday += breath;
        breath = breath / 1000; //change L/min to m3/min
        PM25Today += density * breath;
        Const.CURRENT_OUTDOOR = locationService.getIndoorOutdoor();
        State state = new State(IDToday, aCache.getAsString(Const.Cache_User_Id), Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                String.valueOf(Const.CURRENT_OUTDOOR),
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numStepsTmp), avg_rate, String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), String.valueOf(PM25Source), 0, isConnected ? 1 : 0);
        numStepsTmp = 0;
        return state;
    }


    private double getLastSevenDaysInOutRatio() {
        List<State> states = new ArrayList<State>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 1; i <= 7; i++) {
            Calendar nowTime = Calendar.getInstance();
            nowTime.add(Calendar.DAY_OF_MONTH, -i);
            nowTime.add(Calendar.MINUTE, -5);
            String left = formatter.format(nowTime.getTime());
            nowTime.add(Calendar.MINUTE, 10);
            String right = formatter.format(nowTime.getTime());
            List<State> temp = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", left, right).list();
            states.addAll(temp);
        }
        int count = 0;
        for (State state : states) {
            if (state.getOutdoor().equals("1")) {
                count++;
            }
        }
        if (states.size() == 0) {
            return 0.5;
        }
        double ratio = count * 1.0 / states.size();
        System.out.println(ratio);
        return ratio;
    }

    private String calLastWeekAvgPM() {
        Double result = 0.0;
        Double tmp = 0.0;
        int num = 0;
        List<List<State>> datas = DataCalculator.getIntance(db).getLastWeekStates();
        if (datas.isEmpty()) {
            return String.valueOf(result);
        }
        for (int i = 0; i != datas.size(); i++) {
            List<State> states = datas.get(i);
            if (!states.isEmpty()) {
                num++;
                tmp = Double.valueOf(states.get(states.size() - 1).getPm25());
            } else {
                tmp = 0.0;
            }
            result += tmp;
        }
        return String.valueOf(result / num);
    }

    private String calLastHourPM() {
        boolean firstHour = false;
        Double result = 0.0;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        Time t = new Time();
        t.setToNow();
        int currentHour = t.hour;
        int currentMin = t.minute;
        calendar.set(year, month, day, currentHour, currentMin, 59);
        Long nowTime = calendar.getTime().getTime();
        int lastHourH = currentHour - 1;
        if (lastHourH < 0) lastHourH = 0;
        calendar.set(year, month, day, lastHourH, currentMin, 0);
        Long lastTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 0, 0, 0);
        Long originTime = calendar.getTime().getTime();
        List<State> test = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", originTime.toString(), lastTime.toString()).list();
        if (test.isEmpty()) firstHour = true;
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", lastTime.toString(), nowTime.toString()).list();
        if (states.isEmpty()) {
            return String.valueOf(result);
        } else if (states.size() == 1) {
            return states.get(states.size() - 1).getPm25();
        } else {
            State state1 = states.get(states.size() - 1); // the last one
            if (firstHour) {
                result = Double.valueOf(state1.getPm25()) - 0;
            } else {
                State state2 = states.get(0); //the first one
                result = Double.valueOf(state1.getPm25()) - Double.valueOf(state2.getPm25());
            }
        }
        return String.valueOf(result);
    }

    /**
     * DB Operations, insert a calculated pm state model to DB
     *
     * @param state
     */
    private void insertState(State state) {
        //check a conflict,
        //ex. 12.2 23.59 - 12.3 0.01 check if current day == insert day, if yes, insert it, else not insert it
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String str = "";
        if (db == null)
            str = "db = null ";
        else str = "db != null ";
        //Log.d(TAG, "-------insert ------state --------- begin");
        state.print();
        //Log.d(TAG, "-------insert ------state --------- finish");
        long r = cupboard().withDatabase(db).put(state);
        str += "entity Id " + String.valueOf(r);
        IDToday++;
        str += " idToday = " + IDToday;
        aCache.put(Const.Cache_Lastime_Timepoint, state.getTime_point());
        //FileUtil.appendStrToFile(DBRunTime,"insert state "+str);
    }

    /**
     * Get and Update Current PM info.
     *
     * @param longitude
     * @param latitude
     */
    private void searchPMRequest(String longitude, String latitude) {
        isPMSearchRunning = true;
        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isPMSearchRunning = false;
                try {
                    int status = response.getInt("status");
                    if (status == 1) {
                        pmModel = PMModel.parse(response.getJSONObject("data"));
                        Intent intent = new Intent(Const.Action_DB_MAIN_PMDensity);
                        intent.putExtra(Const.Intent_PM_Density, pmModel.getPm25());
                        //set current pm density for calculation
                        PM25Density = Double.valueOf(pmModel.getPm25());
                        PM25Source = pmModel.getSource();
                        Log.e(TAG, "searchPMRequest PM2.5 Density " + String.valueOf(PM25Density) + " Source: " + PM25Source);
                        aCache.put(Const.Cache_PM_Density, PM25Density);
                        aCache.put(Const.Cache_PM_Source, String.valueOf(PM25Source));
                        sendBroadcast(intent);
                        DBCanRun = true;
                        isPMSearchSuccess = true;
                        FileUtil.appendStrToFile(DBRunTime, " search pm density success, density: " + PM25Density);
                    } else {
                        isPMSearchRunning = false;
                        isPMSearchSuccess = false;
                        FileUtil.appendStrToFile(DBRunTime, "search pm density failed");
                        if(isBackground != null && isBackground.equals(bgStr))
                             Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Failed, Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.v(TAG, "searchPMRequest resp:" + response.toString());
                if(isBackground != null && isBackground.equals(bgStr))
                    Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isPMSearchRunning = false;
                isPMSearchSuccess = false;
                FileUtil.appendStrToFile(DBRunTime, "search pm density failed");
                if(isBackground != null && isBackground.equals(bgStr))
                    Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Failed, Toast.LENGTH_SHORT).show();
            }

        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                Const.Default_Timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Deprecated
    public void uploadPMData(final State state) {
        Log.d(TAG, "uploadPMData State density: " + state.getDensity());
        isUploadRunning = true;
        String url = HttpUtil.Upload_url;
        JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));
        Log.d("json", tmp.toString());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, tmp, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isUploadRunning = false;
                FileUtil.appendStrToFile(DBRunTime, "upload data success");
                Log.v("response", response.toString());
                insertState(state);
                updateStateUpLoad(state, 1);
                Toast.makeText(getApplicationContext(), Const.Info_Upload_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "uploadPMData onError Response");
                FileUtil.appendStrToFile(DBRunTime, "upload data failed");
                isUploadRunning = false;
                String id = aCache.getAsString(Const.Cache_User_Id);
                if (ShortcutUtil.isStringOK(id) && !id.equals("0"))
                    Toast.makeText(getApplicationContext(), Const.Info_Upload_Failed, Toast.LENGTH_SHORT).show();
                insertState(state);
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void updateStateUpLoad(State state, int upload) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, upload);
        cupboard().withDatabase(db).update(State.class, values, "id = ?", state.getId() + "");
    }

    /**
     * Check DB if there are some data for uploading
     */
    public void checkPMDataForUpload() {
        /*
         TODO: 16/2/9
         TODO 1.Size of states should have a range. Ex. just query last week data for upload
         TODO 2.The largest size of a week data should be 7 * 24 * 3600, is it decent to upload at one time?
         TODO 3.Is is necessary for server to have a authentication, since it looks currently everyone could upload through api.
          */

        String idStr = aCache.getAsString(Const.Cache_User_Id);
        if (ShortcutUtil.isStringOK(idStr) && !idStr.equals("0")) {
            Log.e("upload", "upload batch start");
            final List<State> states = cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_HAS_UPLOAD + "=?", "0").list();
            Log.e("upload", "upload size " + states.size());
            FileUtil.appendStrToFile(DBRunTime, "checkPMDataForUpload upload batch start size = " + states.size());
            isUploadRunning = true;
            String url = HttpUtil.UploadBatch_url;
            JSONArray array = new JSONArray();
            final int size = states.size()<1000?states.size():1000;
            for (int i=0;i<size;i++) {
                JSONObject tmp = State.toJsonobject(states.get(i), aCache.getAsString(Const.Cache_User_Id));
                array.put(tmp);
            }
            JSONObject batchData = null;
            try {
                batchData = new JSONObject();
                batchData.put("data", array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //Log.e("batchData", batchData.toString());
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, batchData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    isUploadRunning = false;
                    Log.e(TAG, "checkPMDataForUpload response " + response.toString());
                    try {
                        String value = response.getString("succeed_count");
                        FileUtil.appendStrToFile(DBRunTime, "checkPMDataForUpload upload success value = " + value);
                        if (Integer.valueOf(value) == size) {
                            for (int i=0;i<size;i++) {
                                updateStateUpLoad(states.get(i), 1);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (isBackground != null && isBackground.equals(bgStr))
                        Toast.makeText(getApplicationContext(), Const.Info_Upload_Success, Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.getMessage() != null)
                        Log.e(TAG, "checkPMDataForUpload error getMessage" + error.getMessage());
                    if (error.networkResponse != null)
                        Log.e(TAG, "checkPMDataForUpload networkResponse statusCode " + error.networkResponse.statusCode);
                    Log.e(TAG, "checkPMDataForUpload error " + error.toString());
                    isUploadRunning = false;
                    if (isBackground != null && isBackground.equals(bgStr))
                        Toast.makeText(getApplicationContext(), Const.Info_Upload_Failed, Toast.LENGTH_SHORT).show();
                }
            }) {
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json; charset=utf-8");
                    return headers;
                }
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Const.Default_Timeout,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }

    }

    class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Const.Action_Bluetooth_Hearth)) {
                String hearthStr = intent.getStringExtra(Const.Intent_Bluetooth_HearthRate);
                if (ShortcutUtil.isStringOK(hearthStr)) {
                    FileUtil.appendStrToFile(DBRunTime, "using hearth rate from bluetooth " + hearthStr);
                    try {
                        int rate = Integer.valueOf(hearthStr);
                        avg_rate = String.valueOf(rate);
                    } catch (Exception e) {
                        avg_rate = "12";
                    }
                }
            } else if (intent.getAction().equals(Const.Action_Search_Density_ToService)) {
                Log.e(TAG, "Action_Search_Density_ToService");
                Intent intentTmp = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State, 0);
                sendBroadcast(intentTmp);
                isPMSearchSuccess = true;
                PM25Density = intent.getDoubleExtra(Const.Intent_PM_Density, 0.0);
                if (PM25Density != 0.0)
                    aCache.put(Const.Cache_PM_Density, PM25Density);
            } else if (intent.getAction().equals(Const.Action_Get_Location_ToService)) {
                Log.e(TAG, "Action_Get_Location_ToService");
                double lati = intent.getDoubleExtra(Const.Intent_DB_PM_Lati, 0.0);
                double longi = intent.getDoubleExtra(Const.Intent_DB_PM_Longi, 0.0);
                if (lati != 0.0 && longi != 0.0) {
                    latitude = lati;
                    longitude = longi;
                }
            } else if (intent.getAction().equals(Const.Action_Refresh_Chart_ToService)) {
                //when open the phone, check if it need to refresh.
                if(!isRefreshRunning) {
                    isRefreshRunning = true;
                    //ProgressDialog.show(getApplicationContext(),"title","message",true,false);
                    refreshAll();
                }
            }else if(intent.getAction().equals(Const.Action_Low_Battery_ToService)){
                String state = intent.getStringExtra(Const.Intent_Low_Battery_State);
                if(state != null && state.equals(Const.IS_SAVING_BATTERY))
                    openSavingBattery();
                else if(state != null && state.equals(Const.Not_SAVING_BATTERY))
                    closeSavingBattery();
            }
        }
    }

    private void openSavingBattery(){
        isSavingBattery = true;
        if(mSensorManager != null)mSensorManager.registerListener(sensorEventListener,
                mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void closeSavingBattery(){
        isSavingBattery = false;
        if(mSensorManager != null)mSensorManager.unregisterListener(sensorEventListener);
    }

    private void refreshAll(){
        refreshHandler.sendEmptyMessage(Const.Handler_Refresh_Text);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart1, 2000);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart2, 3000);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart3, 4000);
    }

    /**
     * Check if service running surpass a day
     *
     * @param lasttime
     * @return
     */
    private boolean isSurpass(State lasttime, State nowTime) {
        boolean result = false;
        String last = null;
        String now = null;
        try {
            last = ShortcutUtil.refFormatOnlyDate(Long.valueOf(lasttime.getTime_point()));
            now = ShortcutUtil.refFormatOnlyDate(Long.valueOf(nowTime.getTime_point()));
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        if (last.equals(now)) result = false;
        else result = true;
        return result;
    }

    /**
     * if Service running surpass a day, then reset data parmas
     */
    private void reset() {
        //todo test it !
        longitude = 0.0;
        latitude = 0.0;
        IDToday = Long.valueOf(0);
        venVolToday = Long.valueOf(0);
        PM25Today = 0;
        PM25Source = 0;
        //PM25Density = 0.0;
        DBCanRun = true;
        DBRunTime = 0;
        isPMSearchRunning = false;
        isLocationChanged = false;
        isUploadRunning = false;
        isPMSearchSuccess = false;
        refreshAll();
        locationInitial();
        DBInitial();
        sensorInitial();
    }
}

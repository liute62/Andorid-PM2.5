package app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.model.PMModel;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBConstants;
import app.utils.DBHelper;
import app.utils.DataCalculator;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.StableCache;
import app.utils.VolleyQueue;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 15/11/10.
 * //need update ....
 * DBService Sequences
 * -----onCreate-----
 */
public class ForegroundService extends Service {

    public static final String TAG = "app.services.ForegroundService";

    /**
     * for data operations
     */
    private ACache aCache;
    private StableCache stableCache;
    private PMModel pmModel;
    private State state;
    private DataService dataService = null;
    /**
     * for PM State
     */
    private double longitude;  //the newest longitude
    private double latitude;  // the newest latitude
    private int PM25Source; //1 or 2
    private double PM25Density;
    private double PM25Today;
    private Long IDToday;
    private double venVolToday;
    private String avg_rate;
    private int inOutDoor;
    /**
     * for cycling
     */
    private int DBRunTime;
    private final int State_TooMuch = 600;
    private int DB_Chart_Loop = 12;
    private int Loc_Runtime = 0;
    private int IO_Runtime = 0;
    private final int DB_Loc_Close_Per = 6;
    private boolean isPMSearchSuccess;
    private boolean DBCanRun;
    private boolean isRefreshRunning;
    private String isBackground = null;
    private final static String bgStr = "false";
    /**
     * Location
     **/
    private LocationService locationService;
    private Location mLastLocation;

    /**
     * for indoor and outdoor judgement
     **/
    private InOutdoorService inOutdoorService;

    /**
     * for motion detection
     */
    MotionService motionService;

    private volatile HandlerThread mHandlerThread;
    private Handler refreshHandler;

    Handler DBHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if ((longitude == 0.0 && latitude == 0.0) && PM25Density == 0.0)
                DBCanRun = false;
            else
                DBCanRun = true;
            if (DBRunnable != null)
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
                if (state != null && state.getId() > State_TooMuch) {
                    //so many data stored, don't want to refresh every time after starting
                } else {
                    SQLiteDatabase db = dataService.getDBHelper().getReadableDatabase();
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
                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
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
                    intentText = new Intent(Const.Action_DB_MAIN_Location);
                    intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
                    intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
                    sendBroadcast(intentText);
                }
                /***** DB Running Normally *****/
                if (DBCanRun) {

                    if (DBRunTime == 0) { //Initialize the state when DB start
                        if (state != null && state.getId() > State_TooMuch) {
                            DBRunTime = 1;
                        } else {
                            state = calculatePM25(longitude, latitude);
                            state.print();
                            DBRunTime = 1;
                        }
                    }
                    //Log.e(TAG,"isBackground == "+isBackground);
                    if (state.getId() > State_TooMuch) DB_Chart_Loop = 24;
                    else DB_Chart_Loop = 12;
                    Bundle mBundle = new Bundle();
                    SQLiteDatabase db = dataService.getDBHelper().getReadableDatabase();
                    switch (DBRunTime % DB_Chart_Loop) { //Send chart data to mainfragment
                        case 1:
                            checkPMDataForUpload();
                            break;
                        case 3:
                            UpdateService.run(getApplicationContext(), aCache, dataService.getDBHelper());
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
            }

            //every 10 min to open the GPS and if get the last location, close it.
            if (DBRunTime % 120 == 0) { //120 240 360, 480, 600, 720
                FileUtil.appendStrToFile(DBRunTime, "the location service open and get in/outdoor state");
                locationService.run(LocationService.TYPE_BAIDU);
                Loc_Runtime = 0;
            }
            Loc_Runtime++;
            if (Loc_Runtime >= DB_Loc_Close_Per) {
                Loc_Runtime = Integer.MIN_VALUE;
                FileUtil.appendStrToFile(DBRunTime, "the location service close");
                locationService.stop();
                int tmp = locationService.getIndoorOutdoor();
                inOutDoor = tmp;
                aCache.put(Const.Cache_Indoor_Outdoor, String.valueOf(inOutDoor));
                FileUtil.appendStrToFile(DBRunTime, "stop getting the in/outdoor state with state == " + inOutDoor);
            }
            //every 1 min to calculate the pm result
            if (DBRunTime % 12 == 0) {
                State last = state;
                state = calculatePM25(longitude, latitude);
                State now = state;
                if (!isSurpass(last, now)) {
                    dataService.insertState(state);
                } else {
                    reset();
                }
            }
            //every 1 hour to check if it need to search the PM density from server
            String lastTime = aCache.getAsString(Const.Cache_DB_Lastime_searchDensity);
            if (!ShortcutUtil.isStringOK(lastTime)) {
                lastTime = String.valueOf(System.currentTimeMillis());
                aCache.put(Const.Cache_DB_Lastime_searchDensity, lastTime);
            }
            Long curTime = System.currentTimeMillis();
            if (curTime - Long.valueOf(lastTime) > Const.Min_Search_PM_Time) {
                FileUtil.appendStrToFile(DBRunTime, "every 1 hour to search the PM density from server");
                aCache.put(Const.Cache_DB_Lastime_searchDensity, String.valueOf(System.currentTimeMillis()));
                searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
            }
            //every 1 hour to check if some data need to be uploaded
            String lastUploadTime = aCache.getAsString(Const.Cache_DB_Lastime_Upload);
            if (!ShortcutUtil.isStringOK(lastUploadTime)) {
                lastUploadTime = String.valueOf(System.currentTimeMillis());
                aCache.put(Const.Cache_DB_Lastime_Upload, lastUploadTime);
            }
            //every 1 hour to check pm data for upload
            if (curTime - Long.valueOf(lastUploadTime) > Const.Min_Upload_Check_Time) {
                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                FileUtil.appendStrToFile(DBRunTime, "every 1 hour to check pm data for upload");
                //check it user have login
                checkPMDataForUpload();
            }
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
                SQLiteDatabase db = dataService.getDBHelper().getReadableDatabase();
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

        FileUtil.appendStrToFile(TAG + " OnCreate");
        initialThread();

        mLastLocation = null;
        PM25Source = 0;
        PM25Density = 0.0;
        longitude = 0.0;
        latitude = 0.0;
        DBCanRun = false;
        DBRunTime = 0;
        avg_rate = "12";

        isPMSearchSuccess = false;
        isRefreshRunning = false;

        aCache = ACache.get(getApplicationContext());
        stableCache = StableCache.getInstance(this);
        registerAReceiver();

        /**
         * init location
         */
        locationService = LocationService.getInstance(this);
        inOutdoorService = new InOutdoorService(this);
        locationService.setGetTheLocationListener(getTheLocation);
        locationInitial();
        inOutDoor = LocationService.Indoor;
        aCache.put(Const.Cache_Indoor_Outdoor, String.valueOf(inOutDoor));
        /**
         * init default data
         */
        dataService = new DataService(this);
        dataService.initDefaultData();
        PM25Density = dataService.getPM25Density();
        PM25Source = dataService.getPM25Source();
        PM25Today = dataService.getPM25Today();
        IDToday = dataService.getIDToday();
        venVolToday = dataService.getVenVolToday();
        avg_rate = "12";

        /**
         * init motion detection
         */
        motionService = MotionService.getInstance(this);
        motionService.start();

        if (mLastLocation != null) {
            locationService.stop();
            Intent intentText = new Intent(Const.Action_DB_MAIN_Location);
            intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
            intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
            sendBroadcast(intentText);
        }
        serviceStateInitial();
        DBHandler.sendEmptyMessageDelayed(0, 15000);//15s
        BackgroundService alarm = BackgroundService.getInstance(this);
        alarm.SetAlarm(this);
    }

    @Override
    public void onDestroy() {
        FileUtil.appendStrToFile(TAG+" onDestory");
        motionService.stop();
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

    private void locationInitial() {
        mLastLocation = locationService.getLastKnownLocation();
        if (mLastLocation != null) {
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();
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

    LocationService.GetTheLocation getTheLocation = new LocationService.GetTheLocation() {
        @Override
        public void onGetLocation(Location location) {
        }

        @Override
        public void onSearchStop(Location location) {
            if (location != null) {
                mLastLocation = location;
                longitude = mLastLocation.getLongitude();
                latitude = mLastLocation.getLatitude();
                String lastLati = aCache.getAsString(Const.Cache_Last_Max_Lati);
                String lastLongi = aCache.getAsString(Const.Cache_Last_Max_Longi);
                boolean isEnough = false;
                if (ShortcutUtil.isStringOK(lastLati) && ShortcutUtil.isStringOK(lastLongi)) {
                    try {
                        isEnough = ShortcutUtil.isLocationChangeEnough(Double.valueOf(lastLati), latitude,
                                Double.valueOf(lastLongi), longitude);
                    } catch (Exception e) {
                        isEnough = false;
                    }
                } else {
                    aCache.put(Const.Cache_Last_Max_Lati, latitude);
                    aCache.put(Const.Cache_Last_Max_Longi, longitude);
                }
                if (isEnough) {
                    FileUtil.appendStrToFile(0, "max latitude " + lastLati + " max longitude" + lastLongi + " latitude " + latitude + " " +
                            "longitude " + longitude);
                    searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                    aCache.put(Const.Cache_Last_Max_Lati, latitude);
                    aCache.put(Const.Cache_Last_Max_Longi, longitude);
                }
                aCache.put(Const.Cache_Longitude, longitude);
                aCache.put(Const.Cache_Latitude, latitude);
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
        boolean isSuccessPMDensity = isPMSearchSuccess;
        /*double ratio;
        if (!isConnected) {
            ratio = this.getLastSevenDaysInOutRatio();
            FileUtil.appendStrToFile(DBRunTime,"no network connection, using ratio == "+ratio);
            density = ratio * density + (1-ratio)*density/3;
            FileUtil.appendStrToFile(DBRunTime,"no network connection, using ratio to get density "+density);
            if (ratio > 0.5)  inOutDoor = LocationService.Indoor;
            else inOutDoor = LocationService.Outdoor;
        } else {
            if (inOutDoor == LocationService.Indoor) density /= 3;
        }*/
        Const.MotionStatus mMotionStatus = motionService.getMotionStatus();
        int numStepsForRecord = motionService.getNumStepsForRecord();

        double static_breath = ShortcutUtil.calStaticBreath(stableCache.getAsString(Const.Cache_User_Weight));
        if (static_breath == 0.0) {
            if (isBackground != null && isBackground.equals(bgStr))
                Toast.makeText(getApplicationContext(), Const.Info_Weight_Null, Toast.LENGTH_SHORT).show();
            static_breath = 6.6; // using the default one
        }
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

        State state = new State(IDToday, aCache.getAsString(Const.Cache_User_Id), Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                String.valueOf(inOutDoor),
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numStepsForRecord), avg_rate, String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), String.valueOf(PM25Source), 0, isConnected ? 1 : 0);
        numStepsForRecord = 0;
        return state;
    }

    /**
     * Get and Update Current PM info.
     *
     * @param longitude
     * @param latitude
     */
    private void searchPMRequest(String longitude, String latitude) {
        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");
                    if (status == 1) {
                        pmModel = PMModel.parse(response.getJSONObject("data"));
                        Intent intent = new Intent(Const.Action_DB_MAIN_PMDensity);
                        intent.putExtra(Const.Intent_PM_Density, pmModel.getPm25());
                        PM25Density = Double.valueOf(pmModel.getPm25());
                        PM25Source = pmModel.getSource();
                        aCache.put(Const.Cache_PM_Density, PM25Density);
                        aCache.put(Const.Cache_PM_Source, String.valueOf(PM25Source));
                        sendBroadcast(intent);
                        DBCanRun = true;
                        isPMSearchSuccess = true;
                        FileUtil.appendStrToFile(DBRunTime, "3.search pm density success, density: " + PM25Density);
                    } else {
                        isPMSearchSuccess = false;
                        FileUtil.appendErrorToFile(DBRunTime, "search pm density failed, status != 1");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (isBackground != null && isBackground.equals(bgStr))
                    Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isPMSearchSuccess = false;
                FileUtil.appendErrorToFile(DBRunTime, "search pm density failed " + error.getMessage() + " " + error);
            }

        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                Const.Default_Timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Check DB if there are some data for uploading
     */
    public void checkPMDataForUpload() {

        String idStr = aCache.getAsString(Const.Cache_User_Id);
        if (ShortcutUtil.isStringOK(idStr) && !idStr.equals("0")) {
            final List<State> states = dataService.getPMDataForUpload();
            //FileUtil.appendStrToFile(DBRunTime, "1.checkPMDataForUpload upload batch start size = " + states.size());
            String url = HttpUtil.UploadBatch_url;
            JSONArray array = new JSONArray();
            final int size = states.size() < 1000 ? states.size() : 1000;
            for (int i = 0; i < size; i++) {
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
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, batchData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String value = response.getString("succeed_count");
                        FileUtil.appendStrToFile(DBRunTime, "1.checkPMDataForUpload upload success value = " + value);
                        if (Integer.valueOf(value) == size) {
                            for (int i = 0; i < size; i++) {
                                dataService.updateStateUpLoad(states.get(i), 1);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.getMessage() != null)
                        FileUtil.appendErrorToFile(DBRunTime, "1.checkPMDataForUpload error getMessage" + error.getMessage());
                    if (error.networkResponse != null)
                        FileUtil.appendErrorToFile(DBRunTime, "1.checkPMDataForUpload networkResponse statusCode " + error.networkResponse.statusCode);
                    FileUtil.appendErrorToFile(DBRunTime, "1.checkPMDataForUpload error " + error.toString());
                }
            }) {
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json; charset=utf-8");
                    return headers;
                }
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Const.Default_Timeout_Long,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
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
                        avg_rate = String.valueOf(rate);
                    } catch (Exception e) {
                        avg_rate = "12";
                    }
                }
            } else if (intent.getAction().equals(Const.Action_Search_Density_ToService)) {
                Intent intentTmp = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State, 0);
                sendBroadcast(intentTmp);
                isPMSearchSuccess = true;
                PM25Density = intent.getDoubleExtra(Const.Intent_PM_Density, 0.0);
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
        refreshCity();
        if (!isPMSearchSuccess)
            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
    }

    private void refreshCity() {
        Intent intentText = new Intent(Const.Action_DB_MAIN_Location);
        intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
        intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
        intentText.putExtra(Const.Intent_DB_City_Ref, 1);
        sendBroadcast(intentText);
    }

    /**
     * Check if service running surpass a day
     *
     * @param lastTime
     * @param nowTime
     * @return result
     */
    private boolean isSurpass(State lastTime, State nowTime) {
        boolean result;
        String last;
        String now;
        try {
            last = ShortcutUtil.refFormatOnlyDate(Long.valueOf(lastTime.getTime_point()));
            now = ShortcutUtil.refFormatOnlyDate(Long.valueOf(nowTime.getTime_point()));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        result = !last.equals(now);
        return result;
    }

    /**
     * if Service running surpass a day, then reset data params
     */
    private void reset() {
        longitude = 0.0;
        latitude = 0.0;
        IDToday = 0L;
        venVolToday = 0L;
        PM25Today = 0;
        PM25Source = 0;
        DBCanRun = true;
        DBRunTime = 0;
        refreshAll();
        locationInitial();
        dataService.reset();
        motionService.reset();
    }
}

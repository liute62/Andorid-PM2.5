package app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

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
import java.util.List;

import app.Entity.State;
import app.model.PMModel;
import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBHelper;
import app.utils.DataCalculator;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 15/11/10.
 *
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

    public static final String ACTION = "app.services.DBService";

    private DBHelper dbHelper;
    private SQLiteDatabase db;

    double longitude;  //the newest longitude
    double latitude;  // the newest latitude
    double last_long;  // the last time longitude
    double last_lati;  // the last time latitude
    double PM25Density;
    double PM25Today;
    Long IDToday;
    double venVolToday;

    private LocationManager mManager;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleStepDetector simpleStepDetector;
    private int numSteps;
    private long time1;
    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;
    PMModel pmModel;
    private Handler DBHandler = new Handler();
    boolean isPMSearchRun;
    private boolean DBCanRun;
    private boolean ChartTaskCanRun;
    private boolean isLocationChanged;
    private boolean isUploadTaskRun;
    private int DBRunTime;
    private int ChartRunTime;
    ACache aCache;
    private final int GPS_Min_Frequency = 1000 * 60 * 59;
    private final int GPS_Min_Distance = 100;
    private final int Indoor_Outdoor_Frequency = 1;
    private final int upload_Frequency = 1;
    private final int State_Much_Index = 100;
    private final int DB_Chart_Loop = 24;
    Location mLastLocation;

    private Runnable DBRunnable = new Runnable() {
        State state;
        Intent intentText;
        Intent intentChart;

        @Override
        public void run() {

            String isBackground = aCache.getAsString(Const.Cache_Is_Background);
            String userId = aCache.getAsString(Const.Cache_User_Id);
            if (isBackground == null) { //App first run
                isBackground = "false";
                aCache.put(Const.Cache_Is_Background, isBackground);
                if (userId == null) aCache.put(Const.Cache_User_Id, "0");
            }
            /**Time interval longer than 30 min, refresh the GUI **/
            if(Const.CURRENT_NEED_REFRESH){
                //Todo refresh the GUI and notify mainfragment to dismiss the progress bar, meanwhile we don't need DB run first time.
                Const.CURRENT_NEED_REFRESH = false;
                //update graph
                //update textview
                //Todo check if some data need to be upload.
            }
            /***** DB Run First time *****/
            if (DBRunTime == 0) {   //The initial state, set cache for chart
                intentChart = new Intent(Const.Action_Chart_Cache);
                if(state != null && state.getId() > State_Much_Index){
                    //so many data stored, don't want to refresh every time after starting
                }else {
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
                    aCache.put(Const.Cache_Chart_10, DataCalculator.getIntance(db).calChart10Data());
                    if (aCache.getAsObject(Const.Cache_Chart_12) == null) {
                        aCache.put(Const.Cache_Chart_12, DataCalculator.getIntance(db).calChart12Data());
                        aCache.put(Const.Cache_Chart_12_Date, DataCalculator.getIntance(db).getLastWeekDate());
                    }
                    sendBroadcast(intentChart);
                }
            }
            /***** DB Running Normally *****/
            if (DBCanRun) {
                if (DBRunTime == 0) { //Initialize the state when DB start
                    if(state != null && state.getId() > State_Much_Index){
                        DBRunTime = 1;
                    }else {
                        state = calculatePM25(longitude, latitude);
                        state.print();
                        DBRunTime = 1;
                    }
                }
                Bundle mBundle = new Bundle();
                //todo slow down the DB based on the size
                switch (DBRunTime % DB_Chart_Loop) { //Send chart data to mainfragment
                    case 5:
                        intentChart = new Intent(Const.Action_Chart_Result_2);
                        DataCalculator.getIntance(db).updateLastDayState();
                        mBundle.putSerializable(Const.Intent_chart1_data, DataCalculator.getIntance(db).calChart1Data());
                        mBundle.putSerializable(Const.Intent_chart2_data, DataCalculator.getIntance(db).calChart2Data());
                        mBundle.putSerializable(Const.Intent_chart3_data, DataCalculator.getIntance(db).calChart3Data());
                        mBundle.putSerializable(Const.Intent_chart6_data, DataCalculator.getIntance(db).calChart6Data());
                        mBundle.putSerializable(Const.Intent_chart10_data, DataCalculator.getIntance(db).calChart10Data());
                        if (isBackground.equals("false")) {
                            intentChart.putExtras(mBundle);
                            sendBroadcast(intentChart);
                        }
                        break;
                    case 10:
                        intentChart = new Intent(Const.Action_Chart_Result_3);
                        DataCalculator.getIntance(db).updateLastWeekState();
                        mBundle.putSerializable(Const.Intent_chart7_data, DataCalculator.getIntance(db).calChart7Data());
                        mBundle.putSerializable(Const.Intent_chart_7_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                        mBundle.putSerializable(Const.Intent_chart12_data, DataCalculator.getIntance(db).calChart12Data());
                        mBundle.putSerializable(Const.Intent_chart_12_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                        if (isBackground.equals("false")) {
                            intentChart.putExtras(mBundle);
                            sendBroadcast(intentChart);
                        }
                        break;
                    case 1:
                    case 3:
                    case 7:
                        intentChart = new Intent(Const.Action_Chart_Result_1);
                        DataCalculator.getIntance(db).updateLastTwoHourState();
                        mBundle.putSerializable(Const.Intent_chart4_data, DataCalculator.getIntance(db).calChart4Data());
                        mBundle.putSerializable(Const.Intent_chart5_data, DataCalculator.getIntance(db).calChart5Data());
                        mBundle.putSerializable(Const.Intent_chart8_data, DataCalculator.getIntance(db).calChart8Data());
                        if (isBackground.equals("false")) {
                            intentChart.putExtras(mBundle);
                            sendBroadcast(intentChart);
                        }
                        break;
                }
                boolean isUpdate = true;
                //every 5 second to check and to update the text in Mainfragment, even though there is no newly data calculated.
                if(state != null && state.getId() > State_Much_Index){
                    //to much data here, we need to slow it down, every 1 min to check it
                    if (DBRunTime % 12 == 0)
                        isUpdate = true;
                    else isUpdate = false;
                }else {
                  isUpdate = true;
                }
                if(isUpdate){
                    intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                    intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                    intentText.putExtra(Const.Intent_DB_PM_Hour, calLastHourPM());
                    intentText.putExtra(Const.Intent_DB_PM_Week, calLastWeekAvgPM());
                }
                if (isUpdate && isBackground.equals("false")) {
                    sendBroadcast(intentText);
                }
                //TODO change to a more soft way by using system.currentime
                if (DBRunTime == 12 * 60) {
                    //means a hour
                    Time t = new Time();
                    t.setToNow();
                    int currentHour = t.hour;
                    searchPMRequest(String.valueOf(longitude),String.valueOf(latitude));
                    DBRunTime = 1;
                }
                //TODO Every 10 min to open the GPS and if get the last location, close it.
                if (DBRunTime % 12 == 0) {
                    //every 1 min to calculate
                    State last = state;
                    //state = calculatePM25(116.329,39.987);
                    state = calculatePM25(longitude, latitude);
                    State now = state;
                    if (!isSurpass(last, now)) {
                        uploadPMData(state);
                    } else {
                        //TODO Check if Runtime logic success
                        reset(DBRunTime);
                    }
                }
                DBRunTime++;
                if (DBRunTime % 5 == 0) {
                    intentText = new Intent(Const.Action_DB_MAIN_Location);
                    intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
                    intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
                    if (isBackground.equals("false")) {
                        sendBroadcast(intentText);
                    }
                }

            } else {

                //Todo using a more soft way to notify user.
            }
            DBHandler.postDelayed(DBRunnable, Const.DB_Run_Time_INTERVAL);
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
        PM25Density = 0.0;
        longitude = 0.0;
        latitude = 0.0;
        last_lati = -0.1;
        last_long = -0.1;
        DBCanRun = false;
        DBRunTime = 0;
        ChartRunTime = -1;
        isPMSearchRun = false;
        isUploadTaskRun = false;
        isLocationChanged = false;
        ChartTaskCanRun = true;
        //todo each time to run the data and
        aCache = ACache.get(getApplicationContext());
        if (aCache.getAsString(Const.Cache_PM_Density) != null) {
            PM25Density = Double.valueOf(aCache.getAsString(Const.Cache_PM_Density));
        }
        DBInitial();
        serviceStateInitial();
        sensorInitial();
        GPSInitial();
        if (mLastLocation != null){
            mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_Min_Frequency,GPS_Min_Distance, locationListener);
        }
        if((longitude == 0.0 && latitude == 0.0) && PM25Density == 0.0){
            DBCanRun = false;
        }else {
            DBCanRun = true;
        }
        DBRunnable.run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DBRunnable = null;
    }

    private void DBInitial() {
        dbHelper = new DBHelper(getApplicationContext());
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
            Log.e("Today size", String.valueOf(states.size()));
            Log.e("Today Last state", "begin");
            state.print();
            PM25Today = Double.parseDouble(state.getPm25());
            venVolToday = Double.parseDouble(state.getVentilation_volume());
            IDToday = Long.valueOf(state.getId());
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
                numSteps++;
            }
        });
        time1 = System.currentTimeMillis();
        mSensorManager.registerListener(new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    simpleStepDetector.updateAccel(
                            event.timestamp, event.values[0], event.values[1], event.values[2]);
                }

                long time2 = System.currentTimeMillis();
                if (time2 - time1 > 5000) {
                    if (numSteps > 70)
                        mMotionStatus = Const.MotionStatus.RUN;
                    else if (numSteps <= 70 && numSteps >= 30)
                        mMotionStatus = Const.MotionStatus.WALK;
                    else
                        mMotionStatus = Const.MotionStatus.STATIC;
                    numSteps = 0;
                    time1 = time2;
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        }, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void GPSInitial() {
        boolean isGPSRun = false;
        String provider = null;
        String[] providers = {LocationManager.GPS_PROVIDER,LocationManager.PASSIVE_PROVIDER,LocationManager.NETWORK_PROVIDER};
        mManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        for (int i = 0; i != providers.length; i++){
            if(mManager.isProviderEnabled(providers[i])){
                provider = providers[i];
            }
        }
        if (provider != null)
            mLastLocation = mManager.getLastKnownLocation(provider);
        if (mLastLocation == null) {
            for (int i = 0; i != providers.length; i++){
                if(mManager.isProviderEnabled(providers[i])){
                    mManager.requestLocationUpdates(providers[i], 0, 0, locationListener);
                }
            }
            Toast.makeText(getApplicationContext(), Const.Info_GPS_No_Cache, Toast.LENGTH_SHORT).show();
        } else {
            isGPSRun = true;
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();
            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
        }
        mManager.addGpsStatusListener(gpsStatusListener);
    }

    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {

        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                //Log.e("GPS_EVENT_FIRST_FIX","yes");
            } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                //Log.e("GPS_EVENT_STARTED","yes");
            } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
              //  Log.e("GPS_EVENT_STOPPED", "yes");
            }
        }
    };

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            if (location != null) {
                isLocationChanged = true;
                mLastLocation = location;
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                if (last_long == longitude && last_lati == latitude) {
                    //means no changes
                } else {
                    //location has been changed, check if changes big enough
                    if (ShortcutUtil.isLocationChangeEnough(last_lati,latitude,last_long,longitude)) {
                        searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                    }
                    last_lati = latitude;
                    last_long = longitude;
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int status, Bundle bundle) {
            if (status == LocationProvider.AVAILABLE) {
                Toast.makeText(getApplicationContext(), Const.Info_GPS_Available, Toast.LENGTH_SHORT).show();
            } else if (status == LocationProvider.OUT_OF_SERVICE) {
                Toast.makeText(getApplicationContext(), Const.Info_GPS_OutOFService, Toast.LENGTH_SHORT).show();
            } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                Toast.makeText(getApplicationContext(), Const.Info_GPS_Pause, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onProviderEnabled(String s) {
            Toast.makeText(getApplicationContext(), Const.Info_GPS_Open,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String s) {
            Toast.makeText(getApplicationContext(), Const.Info_GPS_Turnoff,
                    Toast.LENGTH_SHORT).show();
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

        boolean isConnected = isNetworkAvailable(this);
        double ratio = 1;
        if (!isConnected) {
            ratio = this.getLastSevenDaysInOutRatio();
            density = ratio * density + (1-ratio)*density/3;
            if (ratio>0.5) {
                Const.CURRENT_INDOOR = true;
            } else {
                Const.CURRENT_INDOOR = false;
            }
        } else {
            if (Const.CURRENT_INDOOR) {
                density /= 3;
            }
        }

        if (mMotionStatus == Const.MotionStatus.STATIC) {
            breath = Const.static_breath;
        } else if (mMotionStatus == Const.MotionStatus.WALK) {
            breath = Const.walk_breath;
        } else if (mMotionStatus == Const.MotionStatus.RUN) {
            breath = Const.run_breath;
        }
        venVolToday += breath;
        breath = breath / 1000; //change L/min to m3/min
        PM25Today += density * breath;

        State state = new State(IDToday, aCache.getAsString(Const.Cache_User_Id), Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                Const.CURRENT_INDOOR ? "1" : "0",
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numSteps), "12", String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), "1", 0, isConnected ? 1 : 0);
        return state;
    }

    /*
    check the availabilty of the network
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private double getLastSevenDaysInOutRatio() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        List<State> states = new ArrayList<State>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i=1;i<=7;i++) {
            Calendar nowTime = Calendar.getInstance();
            nowTime.add(Calendar.DAY_OF_MONTH,-i);
            nowTime.add(Calendar.MINUTE,-5);
            String left = formatter.format(nowTime.getTime());
            nowTime.add(Calendar.MINUTE, 10);
            String right = formatter.format(nowTime.getTime());
            List<State> temp = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", left, right).list();
            states.addAll(temp);
        }
        int count = 0;
        for (State state: states) {
            if (state.getOutdoor().equals("1")) {
                count++;
            }
        }
        if (states.size()==0) {
            return 0.5;
        }
        double ratio = count*1.0/states.size();
        System.out.println(ratio);
        return ratio;
    }

    private String calLastWeekAvgPM() {
        Double result = 0.0;
        Double tmp;
        int num = 0;
        List<List<State>> datas = DataCalculator.getIntance(db).getLastWeekStates();
        if (datas.isEmpty()) {
            return String.valueOf(result);
        }
        for (int i = 0; i != datas.size(); i++) {
            List<State> states = datas.get(i);
            if (states.isEmpty()) {
                break;
            } else {
                num++;
                tmp = Double.valueOf(states.get(states.size() - 1).getPm25());
                result += tmp;
            }
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
            //Log.e("state1",state1.getPm25());
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
     * DB Operations
     *
     * @param state
     */
    private void insertState(State state) {
//        //check a conflict,
//        //ex. 12.2 23.59 - 12.3 0.01 check if current day == insert day, if yes, insert it, else not insert it
//        String insert = ShortcutUtil.refFormatOnlyDate(Long.valueOf(state.getTime_point()));
//        Time t = new Time();
//        t.setToNow();
//        String now =  ShortcutUtil.refFormatOnlyDate(t.toMillis(true));
//        //Log.e("insertState","now"+now+"insert"+insert);
//        if(insert.equals(now)) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Log.e("state insert","-------insert ------state --------- begin");
        state.print();
        Log.e("state insert", "-------insert ------state --------- finish");
        cupboard().withDatabase(db).put(state);
        //Log.e("State,Inserted upload", String.valueOf(state.getUpload()));
        IDToday++;
//        }else {
//            Toast.makeText(getApplicationContext(),Const.Info_DB_Insert_Date_Conflict,Toast.LENGTH_SHORT);
//        }
    }

    private void searchState() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);

        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        if (!states.isEmpty()) {
            for (int i = 0; i != states.size(); i++) {
                states.get(i).print();
            }
        }
    }

    /**
     * Get and Update Current PM info.
     *
     * @param longitude
     * @param latitude
     */
    private void searchPMRequest(String longitude, String latitude) {
        isPMSearchRun = true;
        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isPMSearchRun = false;
                try {
                    pmModel = PMModel.parse(response);
                    Intent intent = new Intent(Const.Action_DB_MAIN_PMDensity);
                    intent.putExtra(Const.Intent_PM_Density, pmModel.getPm25());
                    //set current pm density for calculation
                    PM25Density = Double.valueOf(pmModel.getPm25());
                    aCache.put(Const.Cache_PM_Density, PM25Density);
                    sendBroadcast(intent);
                    DBCanRun = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("searchPMRequest resp", response.toString());
                Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                DBCanRun = false;
                isPMSearchRun = false;
                Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Failed, Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    public void uploadPMData(final State state) {
        isUploadTaskRun = true;
        String url = HttpUtil.Upload_url;
        JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));
        Log.e("json", tmp.toString());
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, tmp, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isUploadTaskRun = false;
                Log.e("response", response.toString());
                State tmp;
                tmp = state;
                tmp.setUpload(1);
                insertState(tmp);
                Toast.makeText(getApplicationContext(), Const.Info_Upload_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isUploadTaskRun = false;
                Toast.makeText(getApplicationContext(), Const.Info_Upload_Failed, Toast.LENGTH_SHORT).show();
                insertState(state);
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Check DB if there are some data for uploading
     */
    public void checkPMDataForUpload() {
        Log.d("upload","upload batch start");
        final List<State> states = (List<State>) cupboard().withDatabase(db).query(State.class).withSelection("upload=?","0");
        isUploadTaskRun = true;
        String url = HttpUtil.UploadBatch_url;
        JSONArray array = new JSONArray();
        for (State state:states) {
            JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));
            array.put(tmp);
        }
        JSONObject batchData = null;
        try {
            batchData = new JSONObject();
            batchData.put("data",array);
        }  catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, batchData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isUploadTaskRun = false;
                Log.e("response", response.toString());
                try {
                    String value = response.getString("succeed_count");
                    if (Integer.valueOf(value)==states.size()) {
                        for (State state : states) {
                            State tmp;
                            tmp = state;
                            tmp.setUpload(1);
                            insertState(tmp);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Toast.makeText(getApplicationContext(), Const.Info_Upload_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isUploadTaskRun = false;
                Toast.makeText(getApplicationContext(), Const.Info_Upload_Failed, Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Check if Service running surpass a day
     *
     * @param lasttime
     * @return
     */
    private boolean isSurpass(State lasttime, State nowTime) {
        boolean result = false;
        String last = ShortcutUtil.refFormatOnlyDate(Long.valueOf(lasttime.getTime_point()));
        String now = ShortcutUtil.refFormatOnlyDate(Long.valueOf(nowTime.getTime_point()));
        if (last.equals(now)) result = false;
        else result = true;
        return result;
    }

    /**
     * if Service running surpass a day, then reset data parmas
     */
    private void reset(int runtime) {
        runtime = -1;
        longitude = 0.0;
        latitude = 0.0;
        last_lati = -0.1;
        last_long = -0.1;
        DBCanRun = false;
        DBRunTime = 0;
        ChartRunTime = -1;
        isPMSearchRun = false;
        isUploadTaskRun = false;
        isLocationChanged = false;
        ChartTaskCanRun = true;
        DBInitial();
        sensorInitial();
        GPSInitial();
    }
}

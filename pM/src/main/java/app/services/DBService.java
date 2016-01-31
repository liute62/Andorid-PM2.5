package app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
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
import java.util.Iterator;
import java.util.List;
import app.Entity.State;
import app.model.PMModel;
import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBConstants;
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

    public static final String TAG = "app.services.DBService";

    /** main **/
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private ACache aCache;
    Handler DBHandler = new Handler();
    PMModel pmModel;
    final int State_Much_Index = 500;
    int DB_Chart_Loop = 12;
    private double longitude;  //the newest longitude
    private double latitude;  // the newest latitude
    private double last_long;  // the last time longitude
    private double last_lati;  // the last time latitude
    private double enough_lati; // lati value to see if location changed enough
    private double enough_longi; //longi value to see if location changed enough
    private double PM25Density;
    private double PM25Today;
    private Long IDToday;
    private double venVolToday;
    private int DBRunTime;
    private boolean isPMSearchRun;
    private boolean isPMSearchSuccess;
    private boolean DBCanRun;
    private boolean ChartTaskCanRun;
    private boolean isLocationChanged;
    private boolean isUploadTaskRun;
    private boolean isUploadRun;
    /** GPS **/
    private LocationManager mManager;
    Location mLastLocation;
    private final int GPS_Min_Frequency = 1000 * 60 * 59;
    private final int GPS_Min_Distance = 10;
    /** Sensor **/
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleStepDetector simpleStepDetector;
    private int numSteps;
    private int numStepsTmp; //to insert the step value to state
    private long time1;
    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;
    private final int Indoor_Outdoor_Frequency = 1;
    private final int upload_Frequency = 1;
    private final int Motion_Detection_Interval = 60 * 1000; //1min
    private final int Motion_Run_Thred = 100; //100 step / min
    private final int Motion_Walk_Thred = 20; // > 10 step / min -- walk

    private Runnable DBRunnable = new Runnable() {
        State state;
        Intent intentText;
        Intent intentChart;

        @Override
        public void run() {
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
                    aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                    aCache.put(Const.Cache_Chart_10, DataCalculator.getIntance(db).calChart10Data());
                    if (aCache.getAsObject(Const.Cache_Chart_12) == null) {
                        aCache.put(Const.Cache_Chart_12, DataCalculator.getIntance(db).calChart12Data());
                        aCache.put(Const.Cache_Chart_12_Date, DataCalculator.getIntance(db).getLastWeekDate());
                    }
                    sendBroadcast(intentChart);
                }
            }

            Log.d(TAG,"DB Runtime = "+String.valueOf(DBRunTime));
            /** notify user whether using the old PM2.5 density **/
            if((longitude == 0.0 && latitude == 0.0) || !isPMSearchSuccess){
                Intent intent = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State,1);
                sendBroadcast(intent);
            }else {
                Intent intent = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State,0);
                sendBroadcast(intent);
            }

            String isBackground = aCache.getAsString(Const.Cache_Is_Background);
            String userId = aCache.getAsString(Const.Cache_User_Id);
            if (isBackground == null) { //App first run
                isBackground = "false";
                aCache.put(Const.Cache_Is_Background, isBackground);
                if (userId == null) aCache.put(Const.Cache_User_Id, "0");
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
                if(state.getId() > State_Much_Index) DB_Chart_Loop = 24;
                else DB_Chart_Loop = 12;
                Bundle mBundle = new Bundle();
                switch (DBRunTime % DB_Chart_Loop) { //Send chart data to mainfragment
                    case 1:
                        checkPMDataForUpload();
                        break;
                    case 3:
                        UpdateService.run(getApplicationContext(),aCache,dbHelper);
                        break;
                    case 5:
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
                    case 7:
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
                }
                //every 5 second to check and to update the text in Mainfragment, even though there is no newly data calculated.
                int mul = 1;
                if(state != null && state.getId() > State_Much_Index) {
                    mul = 2;
                }
                    //to much data here, we need to slow it down, every 1 min to check it
                    intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                     if(DBRunTime % (2 * mul) == 0) {
                         intentText.putExtra(Const.Intent_DB_PM_Hour, calLastHourPM());
                     }
                     if(DBRunTime % (5 * mul) == 0) {
                        intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                     }if(DBRunTime % (10 * mul) == 0) {
                        intentText.putExtra(Const.Intent_DB_PM_Week, calLastWeekAvgPM());
                     }
                if (isBackground.equals("false")) {
                    sendBroadcast(intentText);
                }
                //change to a more soft way by using system.currentime
                String lastTime = aCache.getAsString(Const.Cache_DB_Lastime_searchDensity);
                if(! ShortcutUtil.isStringOK(lastTime))  {
                    aCache.put(Const.Cache_DB_Lastime_searchDensity,String.valueOf(System.currentTimeMillis()));
                    searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                }else {
                    Long curTime = System.currentTimeMillis();
                    //every 1 hour to search the PM density from server
                    if (curTime - Long.valueOf(lastTime) > Const.Min_Search_PM_Time) {
                        aCache.put(Const.Cache_DB_Lastime_searchDensity, String.valueOf(System.currentTimeMillis()));
                        searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                    }
                }
                //every 10 min to open the GPS and if get the last location, close it.
                if(DBRunTime % 120 == 0){
                    Location location = getLastLocation();
                    if(location != null){
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    }
                    mManager = null;
                }
                //every 1 min to calculate the pm result
                if (DBRunTime % 12 == 0) {
                    State last = state;
                    state = calculatePM25(longitude, latitude);
                    State now = state;
                    if (!isSurpass(last, now)) {
                        uploadPMData(state);
                    } else {
                        //TODO Check if Runtime logic success
                        reset(DBRunTime);
                    }
                }
                //every 1 hour to check if some data need to be uploaded
                String lastUploadTime = aCache.getAsString(Const.Cache_DB_Lastime_Upload);
                if(! ShortcutUtil.isStringOK(lastUploadTime))  aCache.put(Const.Cache_DB_Lastime_Upload,String.valueOf(System.currentTimeMillis()));
                else {
                    Long curTime = System.currentTimeMillis();
                    //every 1 hour to search the PM density from server
                    if (curTime - Long.valueOf(lastUploadTime) > Const.Min_Upload_Check_Time) {
                        aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                        if(ShortcutUtil.isStringOK(aCache.getAsString(Const.Cache_User_Id))){
                            //means currently user has login
                           // checkPMDataForUpload();
                        }
                    }
                }
                DBRunTime++;
                if(DBRunTime >= 500) DBRunTime = 1; //500 a cycle
                if (DBRunTime % 5 == 0) {
                    intentText = new Intent(Const.Action_DB_MAIN_Location);
                    intentText.putExtra(Const.Intent_DB_PM_Lati, String.valueOf(latitude));
                    intentText.putExtra(Const.Intent_DB_PM_Longi, String.valueOf(longitude));
                    if (isBackground.equals("false")) {
                        sendBroadcast(intentText);
                    }
                }
            } else {
                //using a more soft way to notify user that DB is not running
                Intent intent = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State,-1);
                sendBroadcast(intent);
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
        enough_lati = -0.1;
        enough_longi = -0.1;
        DBCanRun = false;
        DBRunTime = 0;
        isPMSearchRun = false;
        isUploadTaskRun = false;
        isLocationChanged = false;
        isUploadRun = false;
        isPMSearchSuccess = false;
        ChartTaskCanRun = true;
        aCache = ACache.get(getApplicationContext());
        //todo each time to run the data and
        if (aCache.getAsString(Const.Cache_PM_Density) != null) {
            PM25Density = Double.valueOf(aCache.getAsString(Const.Cache_PM_Density));
            Log.d(TAG,"PM25 Density "+String.valueOf(PM25Density));
        }
        GPSInitial();
        DBInitial();
        serviceStateInitial();
        sensorInitial();
        if (mLastLocation != null){
            Log.d(TAG,"Change the Location Updates speed to "+String.valueOf(GPS_Min_Frequency)+" "+String.valueOf(GPS_Min_Distance));
            mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, GPS_Min_Frequency,GPS_Min_Distance, locationListener);
        }
        if((longitude == 0.0 && latitude == 0.0) && PM25Density == 0.0){
            Log.e(TAG,"DBCanRun == False, longitude == 0.0 && latitude == 0.0 && PM25Density == 0.0");
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
            //Log.e("Today size", String.valueOf(states.size()));
            //Log.e("Today Last state", "begin");
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
                Log.d(TAG,"Time: "+ShortcutUtil.refFormatNowDate(timeNs)+" Step: "+String.valueOf(numSteps));
                numSteps++;
                numStepsTmp++;
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
                if (time2 - time1 > Motion_Detection_Interval) {
                    if (numSteps > Motion_Run_Thred)
                        mMotionStatus = Const.MotionStatus.RUN;
                    else if (numSteps <= Motion_Run_Thred && numSteps >= Motion_Walk_Thred)
                        mMotionStatus = Const.MotionStatus.WALK;
                    else
                        mMotionStatus = Const.MotionStatus.STATIC;
                    numSteps = 0;
                    time1 = time2;
                    Log.v(TAG, "Motion Status: " + String.valueOf(mMotionStatus));
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
                mLastLocation = mManager.getLastKnownLocation(provider);
                if(mLastLocation != null) break;
            }
        }
        if (mLastLocation == null) {
            Log.e(TAG, "provider: "+provider+" LastKnownLocation == null");
            for (int i = 0; i != providers.length; i++){
                if(mManager.isProviderEnabled(providers[i])) {
                    Log.e(TAG, "No lastimeLocation, Request: "+providers[i]+" Update");
                    mManager.requestLocationUpdates(providers[i], 0, 0, locationListener);
                }
            }
            Toast.makeText(getApplicationContext(), Const.Info_GPS_No_Cache, Toast.LENGTH_SHORT).show();
        } else {
            isGPSRun = true;
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();
            Log.d(TAG,"Location Service is running"+String.valueOf(latitude)+" "+String.valueOf(longitude));
            aCache.put(Const.Cache_Latitude, latitude);
            aCache.put(Const.Cache_Longitude,longitude);
            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
        }
        mManager.addGpsStatusListener(gpsStatusListener);
    }

    GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {

        public void onGpsStatusChanged(int event) {
            Log.d(TAG,"onGpsStatusChanged event == "+String.valueOf(event));
            GpsStatus status = mManager.getGpsStatus(null);
            if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                int time = status.getTimeToFirstFix();
                Log.d(TAG,"onGpsStatusChanged time "+String.valueOf(time));
            } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                Iterable<GpsSatellite> allgps = status.getSatellites();
                Iterator<GpsSatellite> items = allgps.iterator();
                int i = 0;
                int ii = 0;
                while (items.hasNext())
                {
                    GpsSatellite tmp = (GpsSatellite) items.next();
                    if (tmp.usedInFix())
                        ii++;
                    i++;
                }
                if(ii > 4){
                    Const.CURRENT_INDOOR = false;
                }else {
                    Const.CURRENT_INDOOR = true;
                }
               Log.d(TAG,"onGpsStatusChanged i "+String.valueOf(i)+" ii"+String.valueOf(ii));
            } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                Iterable<GpsSatellite> allgps = status.getSatellites();
                Iterator<GpsSatellite> items = allgps.iterator();
                int i = 0;
                int ii = 0;
                while (items.hasNext())
                {
                    GpsSatellite tmp = (GpsSatellite) items.next();
                    if (tmp.usedInFix())
                        ii++;
                    i++;
                }
                if(ii > 4){
                    Const.CURRENT_INDOOR = false;
                }else {
                    Const.CURRENT_INDOOR = true;
                }
                Log.d(TAG,"onGpsStatusChanged started i "+String.valueOf(i)+" ii"+String.valueOf(ii));
            } else if (event == GpsStatus.GPS_EVENT_STOPPED) {
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
                    Log.d(TAG,"onLocationChanged Current Location == Lastime Location");
                } else {
                    //location has been changed, check if changes big enough
                    if (ShortcutUtil.isLocationChangeEnough(enough_lati,latitude,enough_longi,longitude)) {
                        Log.d(TAG,"onLocationChanged Current Location Changed enough and get the density from server");
                        enough_longi = longitude;
                        enough_lati = latitude;
                        searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                    }
                    last_lati = latitude;
                    last_long = longitude;
                }
            }else {
                Log.d(TAG,"onLocationChanged Location == null");
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
        double static_breath = ShortcutUtil.calStaticBreath(aCache.getAsString(Const.Cache_User_Weight));
        if(static_breath == 0.0){
            Toast.makeText(getApplicationContext(),Const.Info_Weight_Null,Toast.LENGTH_SHORT).show();
            static_breath = 6.6; // using the default one
        }
        Log.d(TAG,"Static Breath "+String.valueOf(static_breath));
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
                Const.CURRENT_INDOOR ? "1" : "0",
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numStepsTmp), "12", String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), "1", 0, isConnected ? 1 : 0);
        numStepsTmp = 0;
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
            }else {
                tmp = 0.0;
            }
            Log.d(TAG,"calLastWeekAvgPM tmp = "+ String.valueOf(tmp));
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
        Log.d(TAG, "-------insert ------state --------- begin");
        state.print();
        Log.d(TAG, "-------insert ------state --------- finish");
        cupboard().withDatabase(db).put(state);
        IDToday++;
        aCache.put(Const.Cache_Lastime_Timepoint,state.getTime_point());
        //Log.e("State,Inserted upload", String.valueOf(state.getUpload()));
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
                    Log.d(TAG,"searchPMRequest PM2.5 Density "+String.valueOf(PM25Density));
                    aCache.put(Const.Cache_PM_Density, PM25Density);
                    sendBroadcast(intent);
                    DBCanRun = true;
                    isPMSearchSuccess = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.v(TAG, "searchPMRequest resp:" + response.toString());
                Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Success, Toast.LENGTH_SHORT).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isPMSearchRun = false;
                isPMSearchSuccess = false;
                Toast.makeText(getApplicationContext(), Const.Info_PMDATA_Failed, Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    public void uploadPMData(final State state) {
            Log.d(TAG,"uploadPMData State density: "+state.getDensity());
            isUploadRun = true;
            String url = HttpUtil.Upload_url;
            JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));
            Log.d("json", tmp.toString());
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, tmp, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    isUploadRun = false;
                    Log.v("response", response.toString());
                    insertState(state);
                    updateStateUpLoad(state, 1);
                    Toast.makeText(getApplicationContext(), Const.Info_Upload_Success, Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG,"uploadPMData onError Response");
                    isUploadRun = false;
                    String id = aCache.getAsString(Const.Cache_User_Id);
                    if(ShortcutUtil.isStringOK(id) && !id.equals("0"))
                        Toast.makeText(getApplicationContext(), Const.Info_Upload_Failed, Toast.LENGTH_SHORT).show();
                    insertState(state);
                }

            });
            VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    private void updateStateUpLoad(State state,int upload) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, upload);
        cupboard().withDatabase(db).update(State.class, values, "id = ?", state.getId() + "");
    }

    /**
     * Check DB if there are some data for uploading
     */
    public void checkPMDataForUpload() {
        String idStr = aCache.getAsString(Const.Cache_User_Id);
        if(ShortcutUtil.isStringOK(idStr) && !idStr.equals("0")){
            Log.d("upload", "upload batch start");
            final List<State> states = cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_HAS_UPLOAD + "=?", "0").list();
            Log.d("upload", "upload size " + states.size());
            isUploadRun = true;
            String url = HttpUtil.UploadBatch_url;
            JSONArray array = new JSONArray();
            for (State state : states) {
                JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));
                array.put(tmp);
            }
            JSONObject batchData = null;
            try {
                batchData = new JSONObject();
                batchData.put("data", array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.d("batchData", batchData.toString());
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, batchData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    isUploadRun = false;
                    Log.d("response", response.toString());
                    try {
                        String value = response.getString("succeed_count");
                        if (Integer.valueOf(value) == states.size()) {
                            for (State state : states) {
                                updateStateUpLoad(state, 1);
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
                    isUploadRun = false;
                    Toast.makeText(getApplicationContext(), Const.Info_Upload_Failed, Toast.LENGTH_SHORT).show();
                }

            });
            VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
    }

    /**
     * Get the last known location from providers
     * @return
     */
    private Location getLastLocation(){
        Location result = null;
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
            longitude = mLastLocation.getLongitude();
            latitude = mLastLocation.getLatitude();
        }
        return result;
    }

    class BluetoothReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }

    /**
     * Check if service running surpass a day
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
        //todo test it !
        runtime = -1;
        longitude = 0.0;
        latitude = 0.0;
        last_lati = -0.1;
        last_long = -0.1;
        IDToday = Long.valueOf(0);
        venVolToday = Long.valueOf(0);
        DBCanRun = false;
        DBRunTime = 0;
        isPMSearchRun = false;
        isUploadTaskRun = false;
        isLocationChanged = false;
        ChartTaskCanRun = true;
        DBInitial();
        sensorInitial();
        GPSInitial();
    }
}

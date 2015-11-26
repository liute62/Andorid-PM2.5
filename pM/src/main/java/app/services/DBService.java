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
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
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
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Iterator;
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
 *   Location Initial:
 *    1.Success, lati&longi are set to  getLastKnownLocation
 *    2.Failed,  lati&longi are set to  Shanghai location
 *   For location changed:
 *    1.last time lati&longi equals current lati&longi, means no change, don't need search result from internet.
 *    2.If location changed, download pm density from server.
 * 5.DB Runnable begin running.
 * -----DB Runnable-----
 * 1. searchPMRequest, two ways
 *    1.Initial time, search pm density
 *    2.After DB Runnable last a hour
 * 2. ChartTaskCanRun means whether to update the chart in mainfragment, default : true,
 *    ChartRunTime = -1,initialize data for chart, meanwhile set cache.
 *    ChartRunTime = 0,1,2,4,5, update chart4,5,8 -- Two Hour Data, set cache
 *    ChartRunTime = 3, update chart1,2,3,6,10 -- A Day data, set cache
 *    ChartRunTime = 6, update chart7,12 -- A Week Data, set cache
 * 3. DBCanRun, the default is false
 *    True:  Only after getting PM Density at first or in running.
 *    False: Search PM Density Failed
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
    private LocationListener locationListener;
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
    private int DBRunTime;
    private int ChartRunTime;
    ACache aCache;

    private Runnable DBRunnable = new Runnable() {

        @Override
        public void run() {
            int mul = Const.DB_PM_Search_INTERVAL / Const.DB_PM_Cal_INTERVAL;
            String isBackgound = aCache.getAsString(Const.Cache_Is_Background);
            if(isBackgound == null){
                isBackgound = "false";
                aCache.put(Const.Cache_Is_Background,isBackgound);
            }
            if(DBRunTime == mul || ChartRunTime == -1){
                //every a hour search pm density automatically
                searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
            }
            if (ChartTaskCanRun){
//                ChartTaskCanRun = false;
                Intent intent;
                Bundle mBundle = new Bundle();
                switch (ChartRunTime){
                    //The initial state
                    case -1:
                        Log.e("DB","initial");
                        intent = new Intent(Const.Action_Chart_Cache);
                        if(aCache.getAsObject(Const.Cache_Chart_1) == null)
                            aCache.put(Const.Cache_Chart_1, DataCalculator.getIntance(db).calChart1Data());
                        if(aCache.getAsObject(Const.Cache_Chart_2) == null)
                            aCache.put(Const.Cache_Chart_2, DataCalculator.getIntance(db).calChart2Data());
                        if(aCache.getAsObject(Const.Cache_Chart_3) == null)
                            aCache.put(Const.Cache_Chart_3, DataCalculator.getIntance(db).calChart3Data());
                        if(aCache.getAsObject(Const.Cache_Chart_4) == null)
                            aCache.put(Const.Cache_Chart_4, DataCalculator.getIntance(db).calChart4Data());
                        if(aCache.getAsObject(Const.Cache_Chart_5) == null)
                            aCache.put(Const.Cache_Chart_5, DataCalculator.getIntance(db).calChart5Data());
                        if(aCache.getAsObject(Const.Cache_Chart_6) == null)
                            aCache.put(Const.Cache_Chart_6, DataCalculator.getIntance(db).calChart6Data());
                        if(aCache.getAsObject(Const.Cache_Chart_7) == null) {
                            aCache.put(Const.Cache_Chart_7, DataCalculator.getIntance(db).calChart7Data());
                            aCache.put(Const.Cache_Chart_7_Date, DataCalculator.getIntance(db).getLastWeekDate());}
                        if(aCache.getAsObject(Const.Cache_Chart_8) == null)
                            aCache.put(Const.Cache_Chart_8, DataCalculator.getIntance(db).calChart8Data());
                        if(aCache.getAsObject(Const.Cache_Chart_10) == null)
                            aCache.put(Const.Cache_Chart_10, DataCalculator.getIntance(db).calChart10Data());
                        if(aCache.getAsObject(Const.Cache_Chart_12) == null){
                            aCache.put(Const.Cache_Chart_12, DataCalculator.getIntance(db).calChart12Data());
                            aCache.put(Const.Cache_Chart_12_Date, DataCalculator.getIntance(db).getLastWeekDate());}
                        break;
                    case 3:
                        intent = new Intent(Const.Action_Chart_Result_2);
                        DataCalculator.getIntance(db).updateLastDayState();
                        mBundle.putSerializable(Const.Intent_chart1_data,  DataCalculator.getIntance(db).calChart1Data());
                        mBundle.putSerializable(Const.Intent_chart2_data,  DataCalculator.getIntance(db).calChart2Data());
                        mBundle.putSerializable(Const.Intent_chart3_data,  DataCalculator.getIntance(db).calChart3Data());
                        mBundle.putSerializable(Const.Intent_chart6_data,  DataCalculator.getIntance(db).calChart6Data());
                        mBundle.putSerializable(Const.Intent_chart10_data, DataCalculator.getIntance(db).calChart10Data());
                        break;
                    case 6:
                        intent = new Intent(Const.Action_Chart_Result_3);
                        DataCalculator.getIntance(db).updateLastWeekState();
                        mBundle.putSerializable(Const.Intent_chart7_data, DataCalculator.getIntance(db).calChart7Data());
                        mBundle.putSerializable(Const.Intent_chart_7_data_date,  DataCalculator.getIntance(db).getLastWeekDate());
                        mBundle.putSerializable(Const.Intent_chart12_data, DataCalculator.getIntance(db).calChart12Data());
                        mBundle.putSerializable(Const.Intent_chart_12_data_date,  DataCalculator.getIntance(db).getLastWeekDate());
                        ChartRunTime = 0;
                        break;
                    default:
                        intent = new Intent(Const.Action_Chart_Result_1);
                        DataCalculator.getIntance(db).updateLastTwoHourState();
                        mBundle.putSerializable(Const.Intent_chart4_data, DataCalculator.getIntance(db).calChart4Data());
                        mBundle.putSerializable(Const.Intent_chart5_data, DataCalculator.getIntance(db).calChart5Data());
                        mBundle.putSerializable(Const.Intent_chart8_data, DataCalculator.getIntance(db).calChart8Data());
                        break;
                }
                ChartRunTime++;
                if(isBackgound.equals("false")) {
                    intent.putExtras(mBundle);
                    sendBroadcast(intent);
                }
            }
            if (DBCanRun) {
                State state = calculatePM25(longitude, latitude);
                insertState(state); //insert the information into database
                //state.print();
                Intent intent = new Intent(Const.Action_DB_MAIN_PMResult);
//                Log.e("hour",calLastHourPM());
                intent.putExtra(Const.Intent_DB_PM_Hour, calLastHourPM());
                intent.putExtra(Const.Intent_DB_PM_Week, calLastWeekAvgPM());
                intent.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                if(isBackgound.equals("false")) {
                    sendBroadcast(intent);
                }
                DBRunTime++;
//                if(DBRunTime == 1){
////                    ChartTaskCanRun = true;
//                    DBRunTime = 0;
//                }
            }
            //searchState();
            //upload(state);
            DBHandler.postDelayed(DBRunnable, Const.DB_PM_Cal_INTERVAL);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        longitude = 0.0;
        latitude = 0.0;
        last_lati = -0.1;
        last_long = -0.1;
        DBCanRun = false;
        DBRunTime = 0;
        ChartRunTime = -1;
        isPMSearchRun = false;
        isLocationChanged = false;
        ChartTaskCanRun = true;
        aCache = ACache.get(getApplicationContext());
        if(aCache.getAsString(Const.Cache_PM_Density) != null){
            PM25Density = Double.valueOf(aCache.getAsString(Const.Cache_PM_Density));
        }
        DBInitial();
        sensorInitial();
        GPSInitial();
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
            PM25Today = Double.parseDouble(state.getPm25());
            venVolToday = Double.parseDouble(state.getVentilation_volume());
            IDToday = Long.valueOf(state.getId());
        }
        //send chart result to initialize the chart

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("PM2.5")
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
        mManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location= mManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(location == null){
            longitude = Const.Default_LONGITUDE;
            latitude = Const.Default_LATITUDE;
            Toast.makeText(getApplicationContext(),Const.Info_GPS_No_Cache,Toast.LENGTH_SHORT).show();
        }else {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
        }
        GpsStatus.Listener gpsStatusListener=new GpsStatus.Listener() {
            public void onGpsStatusChanged(int event) {
                if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                } else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
                    GpsStatus gpsStatus = mManager.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    Iterator<GpsSatellite> it = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (it.hasNext() && count <= maxSatellites) {
                        count++;
                        GpsSatellite s = it.next();
                    }
                } else if (event == GpsStatus.GPS_EVENT_STARTED) {
                } else if (event == GpsStatus.GPS_EVENT_STOPPED) {

                }
            }
        };
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    Log.e(String.valueOf(latitude),String.valueOf(longitude));
                    isLocationChanged = true;
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                    if (last_long == longitude && last_lati == latitude) {
                        //means no changes
                    } else {
                        //location has been changed
                        last_lati = latitude;
                        last_long = longitude;
                        Const.Default_LATITUDE = latitude;
                        Const.Default_LONGITUDE = longitude;
                        if (isPMSearchRun == false) {
                            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                        }
                    }
                }
            }

            @Override
            public void onStatusChanged(String s, int status, Bundle bundle) {
                if(status==LocationProvider.AVAILABLE){
                    Toast.makeText(getApplicationContext(),Const.Info_GPS_Available,Toast.LENGTH_SHORT).show();
                }else if(status== LocationProvider.OUT_OF_SERVICE){
                    Toast.makeText(getApplicationContext(),Const.Info_GPS_OutOFService,Toast.LENGTH_SHORT).show();
                }else if(status==LocationProvider.TEMPORARILY_UNAVAILABLE){
                    Toast.makeText(getApplicationContext(),Const.Info_GPS_Pause,Toast.LENGTH_SHORT).show();
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
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        criteria.setAltitudeRequired(false);
        criteria.setBearingRequired(false);
        criteria.setCostAllowed(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        criteria.setSpeedRequired(false);

        String provider = mManager.getBestProvider(criteria, true);
        mManager.addGpsStatusListener(gpsStatusListener);
        mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                Const.DB_Location_INTERVAL, 0, locationListener);
    }

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
        if (Const.CURRENT_INDOOR) {
            density /= 3;
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

        State state = new State(IDToday, Const.CURRENT_USER_ID, Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                Const.CURRENT_INDOOR ? "1" : "0",
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numSteps), "12", String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), "1");
        return state;
    }

    private String calLastWeekAvgPM() {
        Double result = 0.0;
        Double tmp;
        int num = 0;
        List<List<State>> datas = DataCalculator.getIntance(db).getLastWeekStates();
        if (datas.isEmpty()){
            return String.valueOf(result);
        }
        for (int i = 0; i != datas.size(); i++){
            List<State> states = datas.get(i);
            if (states.isEmpty()){
                break;
            }else {
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
        if(lastHourH < 0) lastHourH = 0;
        calendar.set(year, month, day, lastHourH, currentMin,0);
        Long lastTime = calendar.getTime().getTime();
        calendar.set(year,month,day,0,0,0);
        Long originTime = calendar.getTime().getTime();
        List<State> test = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", originTime.toString(), lastTime.toString()).list();
        if(test.isEmpty()) firstHour = true;
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", lastTime.toString(), nowTime.toString()).list();
        if (states.isEmpty()) {
            return String.valueOf(result);
        } else if (states.size() == 1) {
            return states.get(states.size() - 1).getPm25();
        } else {
            State state1 = states.get(states.size() - 1); // the last one
            Log.e("state1",state1.getPm25());
            if(firstHour){
                result = Double.valueOf(state1.getPm25()) - 0;
            }else {
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).put(state);
        IDToday++;
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
                    aCache.put(Const.Cache_PM_Density,PM25Density);
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

        }

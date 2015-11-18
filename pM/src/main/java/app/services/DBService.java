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
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.model.PMModel;
import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.Const;
import app.utils.DBHelper;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class DBService extends Service {
    public static final String ACTION = "app.services.DBService";

    private DBHelper dbHelper;
    private SQLiteDatabase db;

    double longitude = 0.0;  //the newest longitude
    double latitude = 0.0;  // the newest latitude
    double last_long = -1.0; // the last time longitude
    double last_lati = -1.0; // the last time latitude
    double PM25Density;
    long idToday = 0;
    double PM25Today;
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
    boolean isPMSearchRun = false;

    private Handler DBHandler = new Handler();

    private boolean DBCanRun = false;
    private int DBRunTime = 0;
    private boolean ChartTaskCanRun = false;
    private Runnable DBRunnable = new Runnable() {

        @Override
        public void run() {
            //addPM25();
            if (DBCanRun) {
                State state = calculatePM25(longitude, latitude);
                insertState(state); //insert the information into database
                state.print();
                Intent intent = new Intent(Const.Action_DB_MAIN_PMResult);
                intent.putExtra(Const.Intent_PM_Id, idToday);
                intent.putExtra(Const.Intent_DB_PM_Hour, calLastHourPM("Han"));
                intent.putExtra(Const.Intent_DB_PM_Week, calLastWeekAvgPM());
                intent.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                intent.putExtra(Const.Intent_DB_PM_TIME, state.getTime_point());
                intent.putExtra(Const.Intent_DB_Ven_Volume, state.getVentilation_volume());
                sendBroadcast(intent);
                DBRunTime++;
                if(DBRunTime == 5){
                    DBRunTime = 0;
                    ChartTaskCanRun = true;
                }
            }
            if (ChartTaskCanRun){
                ChartTaskCanRun = false;
                Intent intent = new Intent(Const.Action_Chart_Result);
                Bundle mBundle = new Bundle();
                mBundle.putSerializable(Const.Intent_chart1_data,calChart1Data());
                intent.putExtras(mBundle);
                sendBroadcast(intent);
                Log.e("ChartTaskCanRun","ChartTaskCanRun");
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
        Log.e("DBService", "OnCreate");
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

        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        if (states.isEmpty()) {
            PM25Today = 0.0;
            venVolToday = 0.0;
            idToday = 0;
        } else {
            State state = states.get(states.size() - 1);
            PM25Today = Double.parseDouble(state.getPm25());
            venVolToday = Double.parseDouble(state.getVentilation_volume());
            idToday = state.getId();
        }


        /** data initial for main fragment**/
        //Intent intent = new Intent(Const.Action_DB_MAIN_PMResult);
        //intent.putExtra(Const.Intent_DB_PM_Hour,calLastHourPM("Ini"));
        //intent.putExtra(Const.Intent_DB_PM_Day, PM25Today);
        //intent.putExtra(Const.Intent_DB_PM_Week,calLastWeekAvgPM());
        //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("PM2.5")
                        .setContentText("服务运行中")
                        .setContentIntent(pendingIntent)
                        .setOngoing(true);

        startForeground(12450, mBuilder.build());
    }

    /**
     * density: (ug/m3)
     * breath:  (L/min)
     *
     * @param longi
     * @param lati
     * @return
     */
    private State calculatePM25(double longi, double lati) {
        Double breath = 0.0;
        Double density = PM25Density;
        //Double density = Double.valueOf(Const.CURRENT_PM_MODEL.getPm25());
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

        State state = new State(idToday, "0", Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                Const.CURRENT_INDOOR ? "1" : "0",
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numSteps), "12", String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), "1");
        return state;
    }

    private String calLastWeekAvgPM() {
        Double result = 0.0;
        return String.valueOf(result);
    }

    private String calLastHourPM(String tag) {
        Double result = 0.0;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);

        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();

        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        if (states.isEmpty()) {
            return String.valueOf(result);
        } else if (states.size() == 1) {
            return states.get(states.size() - 1).getPm25();
        } else {

            State state1 = states.get(states.size() - 1);
            State state2 = states.get(states.size() - 2);
            result = Double.valueOf(state1.getPm25()) - Double.valueOf(state2.getPm25());
        }
        Log.e("calLast" + tag, String.valueOf(result));
        return String.valueOf(result);
    }

    private HashMap<Integer,Float> calChart1Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        Double result = 0.0;
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);

        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();

        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        Map<Integer,Float> tmpMap = new HashMap<>();
        if (states.isEmpty()){
            return map;
        }
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPoint(Long.valueOf(state.getTime_point()));
            float pm25;
            if(i == 0){
                pm25 = Float.valueOf(state.getPm25());
            }else {
                pm25 = Float.valueOf(state.getPm25()) - Float.valueOf(states.get(i-1).getPm25());
            }
            //now we get the index of time and the pm25 of that point
            tmpMap.put(index, pm25);
        }
        //now calculate the avg value
        for (int i = 0; i != 48; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.avgOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
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
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location != null) {
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                    if (last_long == longitude && last_lati == latitude) {
                        //means no changes
                    } else {
                        //location has been changed
                        last_lati = latitude;
                        last_long = longitude;
                        if (isPMSearchRun == false) {
                            Log.e("onLocationChanged", "searchPMRequest");
                            searchPMRequest(String.valueOf(longitude), String.valueOf(latitude));
                        }
                    }
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.e("onStatusChanged", s);

            }

            @Override
            public void onProviderEnabled(String s) {
                Log.e("onProviderEnabled", s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.e("onProviderDisabled", s);
                Toast.makeText(getApplicationContext(), Const.ERROR_NO_GPS,
                        Toast.LENGTH_SHORT).show();
            }
        };
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);  //模糊模式
        criteria.setAltitudeRequired(false);             //不提供海拔信息
        criteria.setBearingRequired(false);              //不提供方向信息
        criteria.setCostAllowed(true);                   //允许运营商计费
        criteria.setPowerRequirement(Criteria.POWER_LOW);//低电池消耗
        criteria.setSpeedRequired(false);                //不提供位置信息

        String provider = mManager.getBestProvider(criteria, true);
        mManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                Const.DB_Location_INTERVAL, 0, locationListener);
    }

    /**
     * DB Operations
     *
     * @param state
     */
    private void insertState(State state) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).put(state);
        idToday++;
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
                    sendBroadcast(intent);
                    DBCanRun = true;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("searchPMRequest resp", response.toString());
                Toast.makeText(getApplicationContext(), "Data Get Success!", Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isPMSearchRun = false;
                Toast.makeText(getApplicationContext(), "Data Get Fail!", Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

}

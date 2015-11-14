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
import android.support.v4.content.LocalBroadcastManager;
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
import java.util.List;

import app.Entity.State;
import app.model.PMModel;
import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.Const;
import app.utils.DBHelper;
import app.utils.HttpUtil;
import app.utils.VolleyQueue;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 15/11/10.
 */
public class DBService extends Service
{
    private static final String TAG = "DBService" ;
    public static final String ACTION = "app.services.DBService";

    private DBHelper dbHelper;
    private SQLiteDatabase db;

    double longitude;
    double latitude;
    double PM25;
    double VENTILATION_VOLUME;

    private LocationManager mManager;
    private LocationListener locationListener;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleStepDetector simpleStepDetector;
    private int numSteps;
    private long time1;
    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;
    PMModel pmModel;
    boolean isPMSearchRun;

    private Handler DBHandler = new Handler();

    private Runnable DBRunnable = new Runnable() {

        @Override
        public void run() {
            //addPM25();
            State state = calculatePM25(longitude,latitude);
            //state.print();
            Intent intent = new Intent(Const.Action_DB_MAIN_PMResult);
            intent.putExtra(Const.Intent_DB_PM_Result,state.getPm25());
            intent.putExtra(Const.Intent_DB_PM_TIME,state.getTime_point());
            intent.putExtra(Const.Intent_DB_Ven_Volume,state.getVentilation_volume());
            sendBroadcast(intent);
            //searchState();
            //insertState(state);
            //upload(state);
            DBHandler.postDelayed(DBRunnable, Const.DB_PM_Search_INTERVAL);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
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

    private void DBInitial(){
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
            PM25 = 0.0;
            VENTILATION_VOLUME = 0.0;
        } else {
            State state = states.get(states.size() - 1);
            PM25 = Double.parseDouble(state.getPm25());
            VENTILATION_VOLUME = Double.parseDouble(state.getVentilation_volume());
        }


        Intent intent = new Intent(ACTION);
        intent.putExtra(Const.Intent_PM_Density, PM25);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

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

    private State calculatePM25(double longi,double lati) {
        Double breath = 0.0;
        Double density = 70.0;
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

        VENTILATION_VOLUME += breath;
        PM25 += density*breath;

        State state = new State("0", Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                Const.CURRENT_INDOOR? "1":"0",
                mMotionStatus == Const.MotionStatus.STATIC? "1" : mMotionStatus == Const.MotionStatus.WALK? "2" : "3",
                Integer.toString(numSteps), "12", String.valueOf(VENTILATION_VOLUME), density.toString(), String.valueOf(PM25), "1");
         return state;
    }

   private void sensorInitial(){
        numSteps = 0;
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
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
                if(time2 - time1 > 5000){
                    if (numSteps > 70)
                        mMotionStatus = Const.MotionStatus.RUN;
                    else if(numSteps <= 70 && numSteps >= 30)
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
        }, mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void GPSInitial(){
        mManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if(location != null) {
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                    if(Const.CURRENT_LONGITUDE == longitude && Const.CURRENT_LATITUDE == latitude){
                        //means no changes
                    }else {
                        //location has been changed
                        Const.CURRENT_LATITUDE = latitude;
                        Const.CURRENT_LONGITUDE = longitude;
                        if (isPMSearchRun == false){
                            Log.e("onLocationChanged","searchPMRequest");
                            searchPMRequest(String.valueOf(longitude),String.valueOf(latitude));
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
                Log.e("onProviderEnabled",s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.e("onProviderDisabled",s);
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
                Const.LOCATION_TIME_INTERVAL, 0, locationListener);
    }

    /**
     * DB Operations
     * @param state
     */
    private void insertState(State state) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).put(state);
    }

    private void searchState(){
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);

        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        if(!states.isEmpty()){
            for (int i = 0; i != states.size(); i++){
                states.get(i).print();
            }
        }
    }

    /**
     * Get and Update Current PM info.
     * @param longitude
     * @param latitude
     */
    private void searchPMRequest(String longitude,String latitude){
        String url = HttpUtil.Search_PM_url;
        url = url+"?longitude="+longitude+"&latitude="+latitude;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                isPMSearchRun = false;
                try {
                    pmModel = PMModel.parse(response);
                    Intent intent = new Intent(Const.Action_DB_MAIN_PMDensity);
                    intent.putExtra(Const.Intent_PM_Density,pmModel.getPm25());
                    sendBroadcast(intent);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("searchPMRequest resp", response.toString());
                Toast.makeText(getApplicationContext(), "Data Get Success!", Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isPMSearchRun  = false;
                Toast.makeText(getApplicationContext(), "Data Get Fail!", Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

}

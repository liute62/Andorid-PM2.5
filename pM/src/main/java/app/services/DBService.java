package app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.example.pm.MainActivity;
import com.example.pm.R;
import java.util.Calendar;
import java.util.List;
import app.Entity.State;
import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.Const;
import app.utils.DBHelper;

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

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private SimpleStepDetector simpleStepDetector;
    private int numSteps;
    private long time1;
    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;


    private Handler DBHandler = new Handler();
    private Runnable DBRunnable = new Runnable() {

        @Override
        public void run() {
            //addPM25();
            calculatePM25(longitude,latitude);
            //searchState();
            //insertState(state);
            //upload(state);
            DBHandler.postDelayed(DBRunnable, Const.DB_TIME_INTERVAL);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Initial();
        DBRunnable.run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        DBRunnable = null;

    }

    private void Initial(){
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
        sensorInitial();
    }

    private void calculatePM25(double longi,double lati) {
        Double breath = 0.0;
        Double density = Double.valueOf(Const.CURRENT_PM_MODEL.getPm25());
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
        state.print();
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
            Log.e("s","s");
            for (int i = 0; i != states.size(); i++){
                states.get(i).print();
            }
        }
    }
}

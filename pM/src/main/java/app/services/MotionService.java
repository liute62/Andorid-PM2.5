package app.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.Const;

/**
 * Created by liuhaodong1 on 16/6/9.
 */
public class MotionService implements SensorEventListener{

    private static MotionService instance = null;

    public final int Motion_Detection_Interval = 60 * 1000; //1min

    public final int Motion_Run_Thred = 100; //100 step / min

    public final int Motion_Walk_Thred = 20; // > 10 step / min -- walk

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private SimpleStepDetector simpleStepDetector;

    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;

    private int numSteps;

    private int numStepsForRecord;

    private long time1;

    private Context mContext;

    private MotionService(Context context){
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new SimpleStepDetector();
    }

    public static MotionService getInstance(Context context){
        if(instance == null)
            instance = new MotionService(context);
        return instance;
    }

    public void start(){
        sensorStart();
    }

    public void stop(){
        if (mSensorManager != null) mSensorManager.unregisterListener(this);
    }

    public void reset(){

    }

    private void sensorStart() {
        numSteps = 0;
        simpleStepDetector.registerListener(new StepListener() {
            @Override
            public void step(long timeNs) {
                numSteps++;
            }
        });
        time1 = System.currentTimeMillis();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(event.values[0], event.values[1], event.values[2]);
        }
        long time2 = System.currentTimeMillis();
        if (time2 - time1 > Motion_Detection_Interval) {
            if (numSteps > Motion_Run_Thred)
                mMotionStatus = Const.MotionStatus.RUN;
            else if (numSteps <= Motion_Run_Thred && numSteps >= Motion_Walk_Thred)
                mMotionStatus = Const.MotionStatus.WALK;
            else
                mMotionStatus = Const.MotionStatus.STATIC;
            numStepsForRecord = numSteps;
            numSteps = 0;
            time1 = time2;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public Const.MotionStatus getMotionStatus() {
        return mMotionStatus;
    }

    public int getNumStepsForRecord(){
        return numStepsForRecord;
    }
}

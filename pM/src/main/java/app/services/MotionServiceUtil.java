package app.services;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.Const;
import app.utils.FileUtil;

/**
 * Created by liuhaodong1 on 16/6/9.
 */
public class MotionServiceUtil implements SensorEventListener{

    public static final String TAG = "MotionServiceUtil";

    private static MotionServiceUtil instance = null;

    public static final int Motion_Detection_Interval = 60 * 1000; //1min

    public static final int Motion_Run_Thred = 100; //100 step / min

    public static final int Motion_Walk_Thred = 20; // > 10 step / min -- walk

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Sensor mStepCounter;

    private SimpleStepDetector simpleStepDetector;

    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;

    private int numSteps;

    private int numStepsForRecord;

    private long time1;

    private Context mContext;

    private MotionServiceUtil(Context context){
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if(mStepCounter == null){
            Log.e(TAG,"mStepCounter == NULL");
        }
        simpleStepDetector = new SimpleStepDetector();
    }

    public static MotionServiceUtil getInstance(Context context){
        if(instance == null)
            instance = new MotionServiceUtil(context);
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
        mSensorManager.registerListener(this,mStepCounter,SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(event.values[0], event.values[1], event.values[2]);
            Log.e(TAG,event.values[0]+" "+event.values[1]);
        }else if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            Log.e(TAG,"step counter "+(int)(event.values[0]));
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
            FileUtil.appendStrToFile("number of steps: " + numStepsForRecord);
        }
    }

    public static Const.MotionStatus getMotionStatus(int steps){
        Const.MotionStatus motionStatus;
        if (steps > Motion_Run_Thred)
            motionStatus = Const.MotionStatus.RUN;
        else if (steps <= Motion_Run_Thred && steps >= Motion_Walk_Thred)
            motionStatus = Const.MotionStatus.WALK;
        else
            motionStatus = Const.MotionStatus.STATIC;
        return motionStatus;
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

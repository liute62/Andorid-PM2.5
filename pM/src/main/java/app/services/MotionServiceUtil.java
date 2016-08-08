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

    public static final int Motion_Detection_Interval = 10 * 1000; //10 seconds

    public static final int Motion_Run_Thred = 20; // step / 10s

    public static final int Motion_Walk_Thred = 4; // > 10 step / seconds -- walk

    public static final int TYPE_ALGORITHM = 0;

    public static final int TYPE_STEP_COUNTER = 1;

    private SensorManager mSensorManager;

    private Sensor mAccelerometer;

    private Sensor mStepCounter;

    private SimpleStepDetector simpleStepDetector;

    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;

    private int numSteps;

    private int numStepsForRecord;

    private long time1;

    private Context mContext;

    private int valueFromStepCounter = 0;

    private onGetStepListener listener = null;

    private boolean isStartOnce = false;

    private long start_time_point = 0;

    private MotionServiceUtil(Context context){
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        simpleStepDetector = new SimpleStepDetector();
        listener = new onGetStepListener() {
            @Override
            public void onGetStep(int type, int num) {

            }
        };
    }

    public static MotionServiceUtil getInstance(Context context){
        if(instance == null)
            instance = new MotionServiceUtil(context);
        return instance;
    }

    public void start(boolean isOnce){
        isStartOnce = isOnce;
        start_time_point = System.currentTimeMillis();
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
            long time2 = System.currentTimeMillis();
            if (time2 - time1 > Motion_Detection_Interval) {
                if (numSteps > Motion_Run_Thred)
                    mMotionStatus = Const.MotionStatus.RUN;
                else if (numSteps <= Motion_Run_Thred && numSteps >= Motion_Walk_Thred)
                    mMotionStatus = Const.MotionStatus.WALK;
                else
                    mMotionStatus = Const.MotionStatus.STATIC;
                numStepsForRecord = numSteps;
                listener.onGetStep(TYPE_ALGORITHM,numStepsForRecord*6);
                if (isStartOnce) stop();
                numSteps = 0;
                time1 = time2;
            }
        }else if(event.sensor.getType() == Sensor.TYPE_STEP_COUNTER){
            valueFromStepCounter = (int)event.values[0];
            listener.onGetStep(TYPE_ALGORITHM,valueFromStepCounter);
            if (isStartOnce) stop();
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

    public int getValueFromStepCounter(){
        return valueFromStepCounter;
    }

    public interface onGetStepListener{
        void onGetStep(int type,int num);
    }

    public void setOnGetStepListener(onGetStepListener l){
        this.listener = l;
    }

    @Override
    protected void finalize() throws Throwable {
        int minus = (int)(System.currentTimeMillis() - start_time_point);
        if(minus < Motion_Detection_Interval){
            listener.onGetStep(TYPE_ALGORITHM,numStepsForRecord);
            int step_minute = numSteps * (60*1000) / minus;
            listener.onGetStep(TYPE_ALGORITHM,step_minute);
            FileUtil.appendErrorToFile(TAG,"finalize before detection interval, "
            +minus+" step number in a minute "+step_minute);
        }
        super.finalize();
    }
}

package com.example.pm;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import app.movement.SimpleStepDetector;
import app.movement.StepListener;
import app.utils.Const;

/**
 * Created by liuhaodong1 on 15/11/11.
 */
public class SensorActivity extends Activity implements SensorEventListener{

    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private SimpleStepDetector simpleStepDetector;
    private int numSteps;
    private long time1;
    private static Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC;


    public SensorActivity(){
        numSteps = 0;
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        simpleStepDetector = new SimpleStepDetector();
        simpleStepDetector.registerListener(new StepListener() {
            @Override
            public void step(long timeNs) {
                numSteps++;
            }
        });
        time1 = System.currentTimeMillis();
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer,SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

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
}

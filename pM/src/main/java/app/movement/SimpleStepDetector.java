/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package app.movement;

import android.hardware.SensorManager;
import android.util.Log;

/**
 * Receives sensor updates and alerts a StepListener when a step has been detected.
 */
public class SimpleStepDetector {

    public static String TAG = "SimpleStepDetector";

    public static final float thred = 1.25f;

    public static int stepNum;

    private StepListener listener;

    public void registerListener(StepListener listener) {
        this.listener = listener;
    }

    /**
     * Accepts updates from the accelerometer.
     */
    public void updateAccel(float x, float y, float z) {
        float[] currentAccel = new float[3];
        currentAccel[0] = x;
        currentAccel[1] = y;
        currentAccel[2] = z;

        currentAccel[0] = -currentAccel[0]/SensorManager.GRAVITY_EARTH;
        currentAccel[1] = -currentAccel[1]/ SensorManager.GRAVITY_EARTH;
        currentAccel[2] = currentAccel[2]/SensorManager.GRAVITY_EARTH;
        float result = (float) Math.sqrt(currentAccel[0]*currentAccel[0] + currentAccel[1]*currentAccel[1] + currentAccel[2]*currentAccel[2]);
        // First step is to update our guess of where the global z vector is.
       // Log.d(TAG,String.valueOf(result));
        if(result > thred){
            listener.step(System.currentTimeMillis());
        }
    }
}
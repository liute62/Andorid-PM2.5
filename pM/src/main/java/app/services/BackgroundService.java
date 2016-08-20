package app.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.model.PMModel;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

/**
 * Created by liuhaodong1 on 16/6/2.
 * This service (receiver) is intended to running on background and do some discrete tasks.
 * Every time there is only one background service running
 * The sequence series are shown as below
 * while onReceive (every 1 minute){
 * 1.check if not running a inner task
 * 2.keep the process wake
 * 3.start inner doing some tasks
 * 4.release the process and reset some parameters
 * for start:
 * 1.
 * }
 */
public class BackgroundService extends BroadcastReceiver {

    public static final String TAG = "BackgroundService";

    private int repeatingCycle = 0; //

    private boolean isGoingToSearchPM = false;

    private boolean isGoingToGetLocation = false;

    private boolean isLocationFinished = true;

    private boolean isSearchDensityFinished = true;

    private boolean isUploadFinished = true;

    private boolean isGetStepFinished = true;

    private PowerManager.WakeLock wakeLock = null;

    private Context mContext = null;

    private DataServiceUtil dataServiceUtil = null;

    private MotionServiceUtil motionServiceUtil = null;

    private Location mLocation = null; //current location

    private State state = null;

    private int stepNum = 0;

    private boolean isReset = false; //if surpass a day, then reset the value.


    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backgroundWake");
        wakeLock.acquire();
        FileUtil.appendStrToFile(TAG, "wakeLock acquire");
        initInner();
        startInner();
    }

    /**
     * set default values
     */
    private void initInner() {
        mLocation = new Location("default");
        mLocation.setLatitude(0.0);
        mLocation.setLongitude(0.0);
        dataServiceUtil = DataServiceUtil.getInstance(mContext);
        motionServiceUtil = MotionServiceUtil.getInstance(mContext);
    }

    /**
     * getting the last cycle params
     */
    private void getLastParams() {

        isLocationFinished = true;
        isSearchDensityFinished = true;
        isUploadFinished = true;
        isGetStepFinished = true;
        repeatingCycle = (int) dataServiceUtil.getIDToday();
        double lati = dataServiceUtil.getLatitudeFromCache();
        double longi = dataServiceUtil.getLongitudeFromCache();
        stepNum = dataServiceUtil.getStepNumFromCache();
        state = dataServiceUtil.getCurrentState();
        isReset = dataServiceUtil.isSurpassFromCache();

        if (lati != 0.0 && longi != 0.0) {
            mLocation = new Location("cache");
            mLocation.setLatitude(lati);
            mLocation.setLongitude(longi);
            isGoingToGetLocation = false;
        } else {
            getLocations(1000 * 10);
            isGoingToGetLocation = false;
        }
        dataServiceUtil.initDefaultData();
    }

    /**
     * 1. init last time data
     * 2. get current location ( time limited : 15s )
     * 3. search pm result from sever (time limited : 15s)
     * 4. save database value
     */
    private void startInner() {

        getLastParams();
        Log.e(TAG, repeatingCycle + " ");
        if (isGoingToGetLocation || repeatingCycle % 23 == 0) {
            isLocationFinished = false;
            getLocations(1000 * 10);
        }
        if (isGoingToSearchPM || repeatingCycle % 101 == 0) {
            isSearchDensityFinished = false;
            searchPMResult(String.valueOf(mLocation.getLongitude()), String.valueOf(mLocation.getLatitude()));
        }
        //every 1 hour to check if some data need to be uploaded
        if (repeatingCycle % 119 == 0) {
            FileUtil.appendStrToFile(repeatingCycle, "every 1 hour to check pm data for upload");
            isUploadFinished = false;
            checkPMDataForUpload();
        }
        onGetSteps();
        saveValues();
        onFinished("saveValues");
    }

    /**
     *
     * @param runningTime
     */
    private void getLocations(long runningTime) {

        LocationServiceUtil locationServiceUtil = LocationServiceUtil.getInstance(mContext);

        if (locationServiceUtil.getLastKnownLocation() != null) {

            mLocation = locationServiceUtil.getLastKnownLocation();
            double last_lati = dataServiceUtil.getMaxLatitudeFromCache();
            double last_longi = dataServiceUtil.getMaxLongitudeFromCache();
            boolean isEnough = ShortcutUtil.isLocationChangeEnough(last_lati, mLocation.getLatitude(),
                    last_longi, mLocation.getLongitude());
            if(isEnough){
                FileUtil.appendStrToFile(TAG, "max latitude " + last_lati + " max longitude" +
                        last_longi + " latitude " + mLocation.getLatitude() + " " +
                        "longitude " + mLocation.getLongitude());
                dataServiceUtil.cacheMaxLocation(mLocation);
                dataServiceUtil.cacheIsSearchDensity(true);
            }
            dataServiceUtil.cacheLocation(mLocation.getLatitude(), mLocation.getLongitude());
            FileUtil.appendStrToFile(repeatingCycle, "locationInitial getLastKnownLocation " +
                    String.valueOf(mLocation.getLatitude()) + " " +
                    String.valueOf(mLocation.getLongitude()));
            isLocationFinished = true;
            onFinished("getLocations getLastKnownLocation");
        }

        locationServiceUtil.setGetTheLocationListener(new LocationServiceUtil.GetTheLocation() {
            @Override
            public void onGetLocation(Location location) {
            }

            @Override
            public void onSearchStop(Location location) {

                if (location != null) {
                    Log.e(TAG, "onSearchStop location " + location.getLongitude());
                    mLocation = location;
                    double last_lati = dataServiceUtil.getMaxLatitudeFromCache();
                    double last_longi = dataServiceUtil.getMaxLongitudeFromCache();
                    boolean isEnough = false;
                    isEnough = ShortcutUtil.isLocationChangeEnough(last_lati, mLocation.getLatitude(),
                            last_longi, mLocation.getLongitude());
                    if (isEnough) {
                        FileUtil.appendStrToFile(TAG, "max latitude " + last_lati + " max longitude" +
                                last_longi + " latitude " + mLocation.getLatitude() + " " +
                                "longitude " + mLocation.getLongitude());
                        dataServiceUtil.cacheMaxLocation(mLocation);
                        dataServiceUtil.cacheIsSearchDensity(true);
                    }
                    dataServiceUtil.cacheLocation(mLocation);
                }
                isLocationFinished = true;
                onFinished("getLocations onSearchStop");
            }
        });
        locationServiceUtil.run(LocationServiceUtil.TYPE_BAIDU);
        locationServiceUtil.setTimeIntervalBeforeStop(runningTime);
    }

    private void onGetSteps(){
        isGetStepFinished = false;
        motionServiceUtil.setOnGetStepListener(new MotionServiceUtil.onGetStepListener() {
            @Override
            public void onGetStep(int type, int num) {
                dataServiceUtil.cacheStepNum(num);
                isGetStepFinished = true;
                onFinished("getSteps Finished");
            }
        });
        motionServiceUtil.start(true);
    }

    /**
     * Get and Update Current PM info.
     *
     * @param longitude the current zone longitude
     * @param latitude  the current zone latitude
     */
    private void searchPMResult(String longitude, String latitude) {

        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        FileUtil.appendStrToFile(repeatingCycle, "searchPMResult " + url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");
                    if (status == 1) {

                        PMModel pmModel = PMModel.parse(response.getJSONObject("data"));
                        NotifyServiceUtil.notifyDensityChanged(mContext, pmModel.getPm25());
                        double PM25Density = Double.valueOf(pmModel.getPm25());
                        int PM25Source = pmModel.getSource();

                        dataServiceUtil.cacheIsSearchDensity(false);
                        dataServiceUtil.cachePMResult(PM25Density, PM25Source);
                        dataServiceUtil.cacheSearchPMFailed(0);

                        FileUtil.appendStrToFile(TAG,"searchPMResult success, density: " + PM25Density);
                    } else {
                        FileUtil.appendErrorToFile(TAG,"searchPMResult failed, status != 1");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    FileUtil.appendErrorToFile(TAG, "searchPMResult JSON error");
                }
                isSearchDensityFinished = true;
                onFinished("searchPMResult success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isSearchDensityFinished = true;
                onFinished("searchPMResult error");
                dataServiceUtil.cacheSearchPMFailed(dataServiceUtil.getSearchFailedCountFromCache()+1);
                FileUtil.appendErrorToFile(TAG,"searchPMResult failed msg: " + error.getMessage());
            }

        });
        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     *
     */
    private void saveValues() {

        State last = state;
        state = dataServiceUtil.calculatePM25(mLocation.getLatitude(), mLocation.getLongitude(),stepNum);
        state.print();
        State now = state;

        //most of cases, isReset would be false, so when isSurpass a day happened,
        //then jump to "else code block", since a new state not inserted, isSurpass always return true
        // only using isReset to jump to "if block" again.
        if (!isSurpass(last, now) || isReset) {
            dataServiceUtil.insertState(state);
            dataServiceUtil.cacheSurpass(false);
        } else {
            reset();
            dataServiceUtil.cacheSurpass(true);
        }
        Log.e(TAG, "repeating times: " + repeatingCycle);
    }

    private void checkPMDataForUpload() {
        dataServiceUtil.cacheLastUploadTime(System.currentTimeMillis());
        FileUtil.appendStrToFile(repeatingCycle, "every 1 hour to check pm data for upload");
        int idStr = dataServiceUtil.getUserIdFromCache();

        if (idStr != 0) {
            final List<State> states = dataServiceUtil.getPMDataForUpload();
            //FileUtil.appendStrToFile(DBRunTime, "1.checkPMDataForUpload upload batch start size = " + states.size());
            String url = HttpUtil.UploadBatch_url;
            JSONArray array = new JSONArray();
            final int size = states.size() < 1000 ? states.size() : 1000;
            for (int i = 0; i < size; i++) {
                JSONObject tmp = State.toJsonobject(states.get(i), String.valueOf(idStr));
                array.put(tmp);
            }
            JSONObject batchData = null;
            try {
                batchData = new JSONObject();
                batchData.put("data", array);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, batchData, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String value = response.getString("succeed_count");
                        FileUtil.appendStrToFile(repeatingCycle, "1.checkPMDataForUpload upload success value = " + value);
                        if (Integer.valueOf(value) == size) {
                            for (int i = 0; i < size; i++) {
                                dataServiceUtil.updateStateUpLoad(states.get(i), 1);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    isUploadFinished = true;
                    onFinished("checkPMDataForUpload success");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    isUploadFinished = true;
                    onFinished("checkPMDataForUpload error");
                    if (error.getMessage() != null)
                        FileUtil.appendErrorToFile(repeatingCycle, "1.checkPMDataForUpload error getMessage" + error.getMessage());
                    if (error.networkResponse != null)
                        FileUtil.appendErrorToFile(repeatingCycle, "1.checkPMDataForUpload networkResponse statusCode " + error.networkResponse.statusCode);
                    FileUtil.appendErrorToFile(repeatingCycle, "1.checkPMDataForUpload error " + error.toString());
                }
            }) {
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json; charset=utf-8");
                    return headers;
                }
            };
            VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
    }

    /**
     * Check if service running surpass a day
     * @param lastTime last time state info
     * @param nowTime  current state info
     * @return result true means to reset, false means keep going
     */
    private boolean isSurpass(State lastTime, State nowTime) {
        boolean result;
        String last;
        String now;
        try {
            last = ShortcutUtil.refFormatOnlyDate(Long.valueOf(lastTime.getTime_point()));
            now = ShortcutUtil.refFormatOnlyDate(Long.valueOf(nowTime.getTime_point()));
        } catch (Exception e) {
            FileUtil.appendErrorToFile(TAG,"isSurpass parsing error or state is null");
            return false;
        }
        result = !last.equals(now);
        return result;
    }

    /**
     * if Service running surpass a day, then reset data params
     */
    private void reset() {
        mLocation.setLongitude(0.0);
        mLocation.setLatitude(0.0);
        repeatingCycle = 0;
        dataServiceUtil.reset();
    }

    private void onFinished(String tag) {
        FileUtil.appendStrToFile(TAG,tag+" onFinished");
        if(isLocationFinished == true &&
                isSearchDensityFinished == true &&
                isUploadFinished == true
                &&isGetStepFinished == true) {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                FileUtil.appendStrToFile(TAG, "all tasks finished,release wakelock");
            }
        }
    }


    public static void runBackgroundService(Context context) {

        context = context.getApplicationContext();
        //cancelBackgroundService(context);
        FileUtil.appendStrToFile(TAG, "------begin background service------");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, BackgroundService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                , 60 * 1000, pi);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi); // Millisec * Second * Minute
    }

    public static void cancelBackgroundService(Context context) {

        FileUtil.appendStrToFile(TAG, "------begin cancel background service------");
        Intent intent = new Intent(context, BackgroundService.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}

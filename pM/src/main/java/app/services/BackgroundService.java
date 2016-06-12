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
import com.android.volley.DefaultRetryPolicy;
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
import app.utils.Const;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.StableCache;
import app.utils.VolleyQueue;

/**
 * Created by liuhaodong1 on 16/6/2.
 * <p/>
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

    long startTime = 0; // the nano time for start the process of a cycle.

    int repeatingCycle = 0; //

    private boolean isGoingToSearchPM = false;

    private boolean isGoingToGetLocation = false;

    private boolean isGoingToSaveData = false;

    private boolean isPMSearchSuccess = false;

    private StableCache cache; // a cache that

    private PowerManager.WakeLock wakeLock = null;

    private Context mContext = null;

    private boolean isFinished = true; //if the background process has finished his job

    private LocationServiceUtil locationServiceUtil = null; // a service for retrieving location information

    private InOutdoorServiceUtil inOutdoorServiceUtil = null;

    private MotionServiceUtil motionServiceUtil = null; // a service for retrieving motion information

    private DataServiceUtil dataServiceUtil = null; // a service for retrieving database and cache result

    private Location mLocation = null; //current location

    State state = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "backgroundWake");
        wakeLock.acquire();
        initInner();
        startInner();
    }

    private void initInner() {
        mLocation = new Location("default");
        mLocation.setLatitude(0.0);
        mLocation.setLongitude(0.0);
        isFinished = false;
        dataServiceUtil = DataServiceUtil.getInstance(mContext);
        cache = StableCache.getInstance(mContext);
    }

    /**
     * 1. init last time data
     * 2. get current location ( time limited : 15s )
     * 3. search pm result from sever (time limited : 15s)
     * 4. save database value
     */
    private void startInner() {

        startTime = System.currentTimeMillis();
        getLastParams();
        Log.e(TAG, repeatingCycle + " ");
        saveValues();
        if (isGoingToGetLocation || repeatingCycle % 20 == 0) {
            getLocations(1000 * 10);
        }
        if (isGoingToSearchPM || repeatingCycle % 120 == 0) {
            searchPMResult(String.valueOf(mLocation.getLatitude()), String.valueOf(mLocation.getLongitude()));
        }
    }

    private void getLastParams() {

        String repeating = cache.getAsString(Const.Cache_Repeating_Time);
        double lati = dataServiceUtil.getLatitude();
        double longi = dataServiceUtil.getLongitude();

        if (ShortcutUtil.isStringOK(repeating)) {
            repeatingCycle = Integer.valueOf(repeating);
        } else {
            repeatingCycle = 1;
            cache.put(Const.Cache_Repeating_Time, repeatingCycle);
        }

        if (lati != 0.0 && longi !=0.0) {
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

    private void getLocations(long runningTime) {

        Log.e(TAG, "getLocations " + System.currentTimeMillis());
        LocationServiceUtil locationServiceUtil = LocationServiceUtil.getInstance(mContext);
        if (locationServiceUtil.getLastKnownLocation() != null) {

            mLocation = locationServiceUtil.getLastKnownLocation();
            dataServiceUtil.cacheLocation(mLocation.getLatitude(), mLocation.getLongitude());
            FileUtil.appendStrToFile(repeatingCycle, "locationInitial getLastKnownLocation " +
                    String.valueOf(mLocation.getLatitude()) + " " + String.valueOf(mLocation.getLongitude()));
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
                    double last_lati = dataServiceUtil.getMaxLatitude();
                    double last_longi = dataServiceUtil.getMaxLongitude();
                    boolean isEnough = false;
                    if (last_lati == 0.0 || last_longi == 0.0) {
                        dataServiceUtil.cacheLocation(mLocation.getLatitude(), mLocation.getLongitude());
                    } else {
                        isEnough = ShortcutUtil.isLocationChangeEnough(last_lati, mLocation.getLatitude(),
                                last_longi, mLocation.getLongitude());
                    }
                    if (isEnough) {
                        FileUtil.appendStrToFile(0, "max latitude " + last_lati + " max longitude" +
                                last_longi + " latitude " + mLocation.getLatitude() + " " +
                                "longitude " + mLocation.getLongitude());
                        isGoingToSearchPM = true;
                        dataServiceUtil.cacheMaxLocation(mLocation);
                    }
                    dataServiceUtil.cacheLocation(mLocation);
                }
            }
        });
        locationServiceUtil.run();
        locationServiceUtil.setTimeIntervalBeforeStop(runningTime);
    }

    PMModel pmModel;
    double PM25Density;
    int PM25Source;

    /**
     * Get and Update Current PM info.
     *
     * @param longitude the current zone longitude
     * @param latitude  the current zone latitude
     */
    private void searchPMResult(String longitude, String latitude) {

        Log.e(TAG, "searchPMResult " + System.currentTimeMillis());
        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");
                    if (status == 1) {
                        isPMSearchSuccess = true;
                        pmModel = PMModel.parse(response.getJSONObject("data"));
                        NotifyServiceUtil.notifyDensityChanged(pmModel.getPm25());
                        PM25Density = Double.valueOf(pmModel.getPm25());
                        PM25Source = pmModel.getSource();
                        dataServiceUtil.cachePMResult(PM25Density, PM25Source);
                        FileUtil.appendStrToFile(repeatingCycle, "3.search pm density success, density: " + PM25Density);
                    } else {
                        isPMSearchSuccess = false;
                        FileUtil.appendErrorToFile(repeatingCycle, "search pm density failed, status != 1");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                isPMSearchSuccess = false;
                FileUtil.appendErrorToFile(repeatingCycle, "search pm density failed " + error.getMessage() + " " + error);
            }

        });
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                Const.Default_Timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Check DB if there are some data for uploading
     */
    public void checkPMDataForUpload2() {

        String idStr = cache.getAsString(Const.Cache_User_Id);
        if (ShortcutUtil.isStringOK(idStr) && !idStr.equals("0")) {
            final List<State> states = dataServiceUtil.getPMDataForUpload();
            //FileUtil.appendStrToFile(DBRunTime, "1.checkPMDataForUpload upload batch start size = " + states.size());
            String url = HttpUtil.UploadBatch_url;
            JSONArray array = new JSONArray();
            final int size = states.size() < 1000 ? states.size() : 1000;
            for (int i = 0; i < size; i++) {
                JSONObject tmp = State.toJsonobject(states.get(i), cache.getAsString(Const.Cache_User_Id));
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
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
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
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    Const.Default_Timeout_Long,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
    }

    private void saveValues() {
        repeatingCycle++;
        cache.put(Const.Cache_Repeating_Time, repeatingCycle);
        State last = state;
        state = dataServiceUtil.calculatePM25(mLocation.getLatitude(), mLocation.getLongitude());
        state.print();
        State now = state;
        if (!isSurpass(last, now)) {
            dataServiceUtil.insertState(state);
        } else {
            reset();
        }
        Log.e(TAG, "repeating times: " + repeatingCycle);
        FileUtil.appendStrToFile("cycle " + repeatingCycle);
        if(wakeLock != null) wakeLock.release();
        else {
            FileUtil.appendStrToFile("wakelock == null");
        }
    }

    private void checkPMDataForUpload() {
        dataServiceUtil.cacheLastUploadTime(System.currentTimeMillis());
        FileUtil.appendStrToFile(repeatingCycle, "every 1 hour to check pm data for upload");

    }

    /**
     * Check if service running surpass a day
     *
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
            e.printStackTrace();
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
        //refreshAll();
        dataServiceUtil.reset();
        motionServiceUtil.reset();
    }

    private void onFinished() {
        isFinished = true;
        wakeLock.release();
    }


    public static void SetAlarm(Context context) {
        context = context.getApplicationContext();
        FileUtil.appendStrToFile("setAlarm");
        Log.e(TAG, "SetAlarm");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, BackgroundService.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                ,60*1000, pi);
        //am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60, pi); // Millisec * Second * Minute
    }

    public void CancelAlarm(Context context) {
        Intent intent = new Intent(context, BackgroundService.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}

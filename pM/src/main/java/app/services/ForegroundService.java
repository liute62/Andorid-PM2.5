package app.services;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.pm.MainActivity;
import com.example.pm.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.model.PMModel;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DataCalculator;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;


/**
 * Created by liuhaodong1 on 15/11/10.
 * This is a service running in the foreground intended to update chart result and other GUI part
 */
public class ForegroundService extends Service {

    public static final String TAG = "app.services.ForegroundService";

    /**
     * for data operations
     */
    private ACache aCache; //for temporary chart data
    private DataServiceUtil dataServiceUtil = null;
    /**
     * for cycling
     */
    private int DBRunTime = 0;
    private final int State_TooMuch = 1000;
    private int DB_Chart_Loop = 12;
    private boolean isRefreshRunning;
    private String isBackground = null;

    private volatile HandlerThread mHandlerThread;
    private Handler refreshHandler;

    private Handler DBHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            DBRunnable.run();
        }
    };

    private Runnable DBRunnable = new Runnable() {

        Intent intentText;
        Intent intentChart;

        @Override
        public void run() {

            int runTimeInterval = Const.DB_Run_Time_INTERVAL;

            /***** DB Run First time *****/
            if (DBRunTime == 0) {   //The initial state, set cache for chart

                intentChart = new Intent(Const.Action_Chart_Cache);
                SQLiteDatabase db = dataServiceUtil.getDBHelper().getReadableDatabase();
                DataCalculator.getIntance(db).updateLastTwoHourState();
                DataCalculator.getIntance(db).updateLastDayState();
                DataCalculator.getIntance(db).updateLastWeekState();
                aCache.put(Const.Cache_Chart_1, DataCalculator.getIntance(db).calChart1Data());
                aCache.put(Const.Cache_Chart_2, DataCalculator.getIntance(db).calChart2Data());
                aCache.put(Const.Cache_Chart_3, DataCalculator.getIntance(db).calChart3Data());
                aCache.put(Const.Cache_Chart_4, DataCalculator.getIntance(db).calChart4Data());
                aCache.put(Const.Cache_Chart_5, DataCalculator.getIntance(db).calChart5Data());
                aCache.put(Const.Cache_Chart_6, DataCalculator.getIntance(db).calChart6Data());
                if (aCache.getAsObject(Const.Cache_Chart_7) == null) {
                    aCache.put(Const.Cache_Chart_7, DataCalculator.getIntance(db).calChart7Data());
                    aCache.put(Const.Cache_Chart_7_Date, DataCalculator.getIntance(db).getLastWeekDate());
                }
                aCache.put(Const.Cache_Chart_8, DataCalculator.getIntance(db).calChart8Data());
                aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                aCache.put(Const.Cache_Chart_10, DataCalculator.getIntance(db).calChart10Data());
                if (aCache.getAsObject(Const.Cache_Chart_12) == null) {
                    aCache.put(Const.Cache_Chart_12, DataCalculator.getIntance(db).calChart12Data());
                    aCache.put(Const.Cache_Chart_12_Date, DataCalculator.getIntance(db).getLastWeekDate());
                }
                sendBroadcast(intentChart);
                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));
                searchPMResult(String.valueOf(dataServiceUtil.getLongitudeFromCache()),
                        String.valueOf(dataServiceUtil.getLatitudeFromCache()));
                aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(System.currentTimeMillis()));

            }

            isBackground = aCache.getAsString(Const.Cache_Is_Background);
            if (isBackground == null) { //App first run
                isBackground = "false";
                aCache.put(Const.Cache_Is_Background, isBackground);
                String userId = aCache.getAsString(Const.Cache_User_Id);
                if (userId == null) aCache.put(Const.Cache_User_Id, "0");
            }

            if (isBackground.equals("false")) {

                double latitude = dataServiceUtil.getLatitudeFromCache();
                double longitude = dataServiceUtil.getLongitudeFromCache();
                boolean isPMSearchSuccess =
                        dataServiceUtil.getSearchFailedCountFromCache() > 2? false:true;

                runTimeInterval = Const.DB_Run_Time_INTERVAL;

                /** notify user whether using the old PM2.5 density **/
                if ((longitude == 0.0 && latitude == 0.0) || !isPMSearchSuccess) {
                    Intent intent = new Intent(Const.Action_DB_Running_State);
                    intent.putExtra(Const.Intent_DB_Run_State, 1);
                    sendBroadcast(intent);
                } else {
                    Intent intent = new Intent(Const.Action_DB_Running_State);
                    intent.putExtra(Const.Intent_DB_Run_State, 0);
                    sendBroadcast(intent);
                }
                if (DBRunTime % 5 == 0) {
                    NotifyServiceUtil.notifyLocationChanged(
                            ForegroundService.this, latitude, longitude);
                }
                /***** DB Running Normally *****/
                dataServiceUtil.refresh();
                State state = dataServiceUtil.getCurrentState();
                if(state == null) return;
                if (DBRunTime == 0) { //Initialize the state when DB start
                    DBRunTime = 1;
                }
                /**check if there is some data for uploading after aimed interval**/
                if(dataServiceUtil.isToUpload()) {
                    checkPMDataForUpload();
                    dataServiceUtil.cacheLastUploadTime(System.currentTimeMillis());
                }
                if(dataServiceUtil.isToSearchCity()){
                    NotifyServiceUtil.notifyLocationChanged(ForegroundService.this,dataServiceUtil.getLatitudeFromCache(),
                            dataServiceUtil.getLongitudeFromCache());
                    dataServiceUtil.cacheLastSearchCityTime(System.currentTimeMillis());
                }
                if (state.getId() > State_TooMuch) DB_Chart_Loop = 10;
                else DB_Chart_Loop = 5;
                Bundle mBundle = new Bundle();
                SQLiteDatabase db = dataServiceUtil.getDBHelper().getReadableDatabase();
                switch (DBRunTime % DB_Chart_Loop) { //Send chart data to mainfragment
                    case 1:
                        //UpdateServiceUtil.run(getApplicationContext(), aCache, dataServiceUtil.getDBHelper());
                        break;
                    case 2:
                        intentChart = new Intent(Const.Action_Chart_Result_1);
                        DataCalculator.getIntance(db).updateLastTwoHourState();
                        mBundle.putSerializable(Const.Intent_chart4_data, DataCalculator.getIntance(db).calChart4Data());
                        mBundle.putSerializable(Const.Intent_chart5_data, DataCalculator.getIntance(db).calChart5Data());
                        mBundle.putSerializable(Const.Intent_chart8_data, DataCalculator.getIntance(db).calChart8Data());
                        aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                        intentChart.putExtras(mBundle);
                        sendBroadcast(intentChart);
                        break;
                    case 3:
                        intentChart = new Intent(Const.Action_Chart_Result_2);
                        DataCalculator.getIntance(db).updateLastDayState();
                        Bundle mBundle2 = new Bundle();
                        mBundle2.putSerializable(Const.Intent_chart1_data, DataCalculator.getIntance(db).calChart1Data());
                        mBundle2.putSerializable(Const.Intent_chart2_data, DataCalculator.getIntance(db).calChart2Data());
                        mBundle2.putSerializable(Const.Intent_chart3_data, DataCalculator.getIntance(db).calChart3Data());
                        mBundle2.putSerializable(Const.Intent_chart6_data, DataCalculator.getIntance(db).calChart6Data());
                        mBundle2.putSerializable(Const.Intent_chart10_data, DataCalculator.getIntance(db).calChart10Data());
                        intentChart.putExtras(mBundle2);
                        sendBroadcast(intentChart);
                        break;
                    case 4:
                        intentChart = new Intent(Const.Action_Chart_Result_3);
                        DataCalculator.getIntance(db).updateLastWeekState();
                        Bundle mBundle3 = new Bundle();
                        mBundle3.putSerializable(Const.Intent_chart7_data, DataCalculator.getIntance(db).calChart7Data());
                        mBundle3.putSerializable(Const.Intent_chart_7_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                        mBundle3.putSerializable(Const.Intent_chart12_data, DataCalculator.getIntance(db).calChart12Data());
                        mBundle3.putSerializable(Const.Intent_chart_12_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                        intentChart.putExtras(mBundle3);
                        sendBroadcast(intentChart);
                        break;
                }
                intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                intentText.putExtra(Const.Intent_DB_PM_Hour, DataCalculator.getIntance(db).calLastHourPM());
                intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                intentText.putExtra(Const.Intent_DB_PM_Week, DataCalculator.getIntance(db).calLastWeekAvgPM());
                sendBroadcast(intentText);
            }
//            else {
//                //using a more soft way to notify user that DB is not running
//                Intent intent = new Intent(Const.Action_DB_Running_State);
//                intent.putExtra(Const.Intent_DB_Run_State, -1);
//                sendBroadcast(intent);
//            }

            DBRunTime++;
            if (DBRunTime >= 721) DBRunTime = 1; //1/5s, 12/min 720/h 721` a cycle

            DBHandler.postDelayed(DBRunnable, runTimeInterval);
        }
    };

    private void initialThread() {

        mHandlerThread = new HandlerThread("DBHandlerThread");
        mHandlerThread.start();
        refreshHandler = new Handler(mHandlerThread.getLooper()) {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Intent intentChart;
                Bundle mBundle = new Bundle();
                SQLiteDatabase db = dataServiceUtil.getDBHelper().getReadableDatabase();
                State state = dataServiceUtil.getCurrentState();
                if (msg.what == Const.Handler_Refresh_Text) {
                    if (state == null) return;
                    DataCalculator.getIntance(db).updateLastTwoHourState();
                    DataCalculator.getIntance(db).updateLastWeekState();
                    Intent intentText = new Intent(Const.Action_DB_MAIN_PMResult);
                    intentText.putExtra(Const.Intent_DB_PM_Hour, DataCalculator.getIntance(db).calLastHourPM());
                    intentText.putExtra(Const.Intent_DB_PM_Day, state.getPm25());
                    intentText.putExtra(Const.Intent_DB_PM_Week, DataCalculator.getIntance(db).calLastWeekAvgPM());
                    sendBroadcast(intentText);
                } else if (msg.what == Const.Handler_Refresh_Chart1) {
                    intentChart = new Intent(Const.Action_Chart_Result_1);
                    DataCalculator.getIntance(db).updateLastTwoHourState();
                    mBundle.putSerializable(Const.Intent_chart4_data, DataCalculator.getIntance(db).calChart4Data());
                    mBundle.putSerializable(Const.Intent_chart5_data, DataCalculator.getIntance(db).calChart5Data());
                    mBundle.putSerializable(Const.Intent_chart8_data, DataCalculator.getIntance(db).calChart8Data());
                    aCache.put(Const.Cache_Chart_8_Time, DataCalculator.getIntance(db).getLastTwoHourTime());
                    intentChart.putExtras(mBundle);
                    sendBroadcast(intentChart);
                } else if (msg.what == Const.Handler_Refresh_Chart2) {
                    intentChart = new Intent(Const.Action_Chart_Result_2);
                    DataCalculator.getIntance(db).updateLastDayState();
                    mBundle.putSerializable(Const.Intent_chart1_data, DataCalculator.getIntance(db).calChart1Data());
                    mBundle.putSerializable(Const.Intent_chart2_data, DataCalculator.getIntance(db).calChart2Data());
                    mBundle.putSerializable(Const.Intent_chart3_data, DataCalculator.getIntance(db).calChart3Data());
                    mBundle.putSerializable(Const.Intent_chart6_data, DataCalculator.getIntance(db).calChart6Data());
                    mBundle.putSerializable(Const.Intent_chart10_data, DataCalculator.getIntance(db).calChart10Data());
                    intentChart.putExtras(mBundle);
                    sendBroadcast(intentChart);
                } else if (msg.what == Const.Handler_Refresh_Chart3) {
                    intentChart = new Intent(Const.Action_Chart_Result_3);
                    DataCalculator.getIntance(db).updateLastWeekState();
                    mBundle.putSerializable(Const.Intent_chart7_data, DataCalculator.getIntance(db).calChart7Data());
                    mBundle.putSerializable(Const.Intent_chart_7_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                    mBundle.putSerializable(Const.Intent_chart12_data, DataCalculator.getIntance(db).calChart12Data());
                    mBundle.putSerializable(Const.Intent_chart_12_data_date, DataCalculator.getIntance(db).getLastWeekDate());
                    intentChart.putExtras(mBundle);
                    sendBroadcast(intentChart);
                    isRefreshRunning = false;
                    Toast.makeText(ForegroundService.this.getApplicationContext(),
                            Const.Info_Refresh_Chart_Success, Toast.LENGTH_SHORT).show();
                }
            }
        };

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FileUtil.appendStrToFile(TAG + "OnCreate");
        initialThread();
        dataServiceUtil = DataServiceUtil.getInstance(this);
        DBRunTime = 0;
        isRefreshRunning = false;
        aCache = ACache.get(getApplicationContext());
        registerAReceiver();
        serviceStateInitial();
        DBHandler.sendEmptyMessageDelayed(0, 1000);//1s
        dataServiceUtil.cacheHasStepCounter(ShortcutUtil.hasStepCounter(this));
        if(dataServiceUtil.isHasStepCounter()){
            FileUtil.appendStrToFile(TAG,"the device has the step counter sensor");
        }else {
            FileUtil.appendStrToFile(TAG,"the device is using algorithm to calculate steps");
        }
        BackgroundService.runBackgroundService(this);
    }

    @Override
    public void onDestroy() {
        FileUtil.appendStrToFile(TAG + " onDestory");
        mHandlerThread.quit();
        super.onDestroy();
        DBRunnable = null;
    }

    private void registerAReceiver() {
        Receiver receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.Action_Search_Density_ToService);
        filter.addAction(Const.Action_Bluetooth_Hearth);
        filter.addAction(Const.Action_Get_Location_ToService);
        filter.addAction(Const.Action_Refresh_Chart_ToService);
        this.registerReceiver(receiver, filter);
    }

    private void serviceStateInitial() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(getResources().getString(R.string.app_name))
                        .setContentText(getResources().getString(R.string.app_service_running))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true);
        startForeground(12450, mBuilder.build());
    }

    /**
     *
     */
    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Const.Action_Bluetooth_Hearth)) {

                String hearthStr = intent.getStringExtra(Const.Intent_Bluetooth_HearthRate);

                if (ShortcutUtil.isStringOK(hearthStr)) {

                    // TODO: 16/8/5 change treating as cache to a more proper one
                    int rate;
                    FileUtil.appendStrToFile(TAG,"using hearth rate from bluetooth " + hearthStr);
                    try {
                        rate = Integer.valueOf(hearthStr);
                        dataServiceUtil.cacheHearthRate(rate);
                    } catch (Exception e) {
                        FileUtil.appendStrToFile(TAG,"onReceive parsing hearth rate error "+
                        hearthStr);
                    }
                }
            } else if (intent.getAction().equals(Const.Action_Search_Density_ToService)) {

                Intent intentTmp = new Intent(Const.Action_DB_Running_State);
                intent.putExtra(Const.Intent_DB_Run_State, 0);
                sendBroadcast(intentTmp);

            } else if (intent.getAction().equals(Const.Action_Get_Location_ToService)) {

                double lati = intent.getDoubleExtra(Const.Intent_DB_PM_Lati, 0.0);
                double longi = intent.getDoubleExtra(Const.Intent_DB_PM_Longi, 0.0);
                if (lati != 0.0 && longi != 0.0) {
                    dataServiceUtil.cacheLocation(lati,longi);
                }else {
                    FileUtil.appendErrorToFile(TAG,"onReceive passing location, latitude == "+
                    lati+" longitude == "+longi);
                }
            } else if (intent.getAction().equals(Const.Action_Refresh_Chart_ToService)) {
                //when open the phone, check if it need to refresh.
                if (!isRefreshRunning) {
                    isRefreshRunning = true;
                    refreshAll();
                }
            }
        }
    }


    /**
     * Check DB if there are some data for uploading
     * if there is, upload  < 1000 state items at once
     */
    public void checkPMDataForUpload() {

        int idStr = dataServiceUtil.getUserIdFromCache();

        if (idStr != 0) {
            final List<State> states = dataServiceUtil.getPMDataForUpload();

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
                FileUtil.appendErrorToFile(TAG,"checkPMDataForUpload JSON Error at batchData");
            }
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST, url, batchData, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    try {
                        String value = response.getString("succeed_count");
                        FileUtil.appendStrToFile(TAG,"checkPMDataForUpload" +
                                " upload success value = " + value);
                        if (Integer.valueOf(value) == size) {
                            for (int i = 0; i < size; i++) {
                                dataServiceUtil.updateStateUpLoad(states.get(i), 1);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        FileUtil.appendErrorToFile(TAG,
                                "checkPMDataForUpload JSON Error at onResponse");
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.getMessage() != null)
                        FileUtil.appendErrorToFile(TAG,"checkPMDataForUpload error msg" +
                                error.getMessage());
                    if (error.networkResponse != null)
                        FileUtil.appendErrorToFile(TAG,"checkPMDataForUpload error statusCode "
                                + error.networkResponse.statusCode);
                    FileUtil.appendErrorToFile(TAG,"1.checkPMDataForUpload error all "+
                            error.toString());
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
            VolleyQueue.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
    }

    /**
     * Get and Update Current PM info.
     * @param longitude the current zone longitude
     * @param latitude  the current zone latitude
     */
    private void searchPMResult(String longitude, String latitude) {

        String url = HttpUtil.Search_PM_url;
        url = url + "?longitude=" + longitude + "&latitude=" + latitude;
        FileUtil.appendStrToFile(TAG,"searchPMResult " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.getInt("status");

                    if (status == 1) {
                        PMModel pmModel = PMModel.parse(response.getJSONObject("data"));
                        NotifyServiceUtil.notifyDensityChanged(ForegroundService.this, pmModel.getPm25());
                        double PM25Density = Double.valueOf(pmModel.getPm25());
                        int PM25Source = pmModel.getSource();
                        dataServiceUtil.cachePMResult(PM25Density, PM25Source);
                        dataServiceUtil.cacheSearchPMFailed(0);
                        FileUtil.appendStrToFile(TAG, "searchPMResult success, density == " +
                                PM25Density);
                    } else {
                        dataServiceUtil.cacheSearchPMFailed(
                                dataServiceUtil.getSearchFailedCountFromCache()+1);
                        FileUtil.appendErrorToFile(TAG, "searchPMResult failed, status != 1");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    FileUtil.appendErrorToFile(TAG, "searchPMResult failed, JSON parsing error");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                dataServiceUtil.cacheSearchPMFailed(dataServiceUtil.getSearchFailedCountFromCache()+1);
                FileUtil.appendErrorToFile(TAG, "searchPMResult failed error msg == " +
                        error.getMessage() + " " + error);
            }

        });

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                Const.Default_Timeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        VolleyQueue.getInstance(
               ForegroundService.this.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /*
    * after the user press the fresh button.
     */
    private void refreshAll() {
        refreshHandler.sendEmptyMessage(Const.Handler_Refresh_Text);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart1, 300);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart2, 600);
        refreshHandler.sendEmptyMessageDelayed(Const.Handler_Refresh_Chart3, 1200);
        NotifyServiceUtil.notifyCityChanged(this, dataServiceUtil.getLatitudeFromCache(),
                dataServiceUtil.getLongitudeFromCache());
    }
}

package com.example.pm;


import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONException;
import org.json.JSONObject;
import app.model.PMModel;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DataFaker;
import app.utils.HttpUtil;
import app.utils.VolleyQueue;
import app.view.widget.LoadingDialog;
import lecho.lib.hellocharts.view.LineChartView;

public class MainFragment extends Fragment implements OnClickListener {

    Activity mActivity;
    ImageView mProfile;
    ImageView mHotMap;
    LineChartView mChart1;
    LineChartView mChart2;
    TextView mTime;
    TextView mAirQuality;
    TextView mCity;
    TextView mHint;
    TextView mHourPM;
    TextView mDayPM;
    TextView mWeekPM;
    TextView mChangeChart1;
    TextView mChangeChart2;
    int PMDensity;
    int currentHour;
    int currentMin;
    ClockTask clockTask;
    boolean isClockTaskRun = false;
    boolean isDataTaskRun = false;
    LoadingDialog loadingDialog;
    PMModel pmModel;
    ACache aCache;
    LocationManager mManager;
    LocationListener locationListener;
    private Double latitude = null;
    private Double longitude = null;

    Handler mClockHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mTime != null) {
                if (currentMin < 10) {
                    mTime.setText(String.valueOf(currentHour) + ": " + "0" + String.valueOf(currentMin));
                } else {
                    mTime.setText(String.valueOf(currentHour) + ": " + String.valueOf(currentMin));
                }
            }

        }
    };

    Handler mDataHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Const.Handler_PM_Data){
                PMModel data = (PMModel)msg.obj;
                PMDensity = Integer.valueOf(data.getPm25());
                Log.e("mDataHandler",String.valueOf(PMDensity));
                dataInitial();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        PMDensity = 0;
        isClockTaskRun = false;
        pmModel = new PMModel();
        mActivity = getActivity();
        loadingDialog = new LoadingDialog(getActivity());
        aCache = ACache.get(mActivity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.e("MainFragment","OncreateView");
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mProfile = (ImageView) view.findViewById(R.id.main_profile);
        mHotMap = (ImageView) view.findViewById(R.id.main_hot_map);
        mTime = (TextView) view.findViewById(R.id.main_current_time);
        mAirQuality = (TextView) view.findViewById(R.id.main_air_quality);
        mCity = (TextView) view.findViewById(R.id.main_current_city);
        mHint = (TextView) view.findViewById(R.id.main_hint);
        mChart1 = (LineChartView) view.findViewById(R.id.main_chart_1);
        mChart2 = (LineChartView) view.findViewById(R.id.main_chart_2);
        mHourPM = (TextView) view.findViewById(R.id.main_hour_pm);
        mDayPM = (TextView) view.findViewById(R.id.main_day_pm);
        mWeekPM = (TextView) view.findViewById(R.id.main_week_pm);
        mChangeChart1 = (TextView) view.findViewById(R.id.main_chart_1_change);
        mChangeChart2 = (TextView) view.findViewById(R.id.main_chart_2_change);
        cacheInitial();
        setListener();
        dataInitial();
        taskInitial();
        return view;
    }

    private void cacheInitial(){
        JSONObject object = aCache.getAsJSONObject(Const.Cache_PM_State);
        if (object == null){
                Toast.makeText(mActivity.getApplicationContext(),Const.ERROR_NO_PM_DATA,Toast.LENGTH_SHORT).show();
        }else{
            //Set current PM state by Cache.
            try {
                Const.CURRENT_PM_MODEL = PMModel.parse(object);
                Message data = new Message();
                data.what = Const.Handler_PM_Data;
                data.obj = Const.CURRENT_PM_MODEL;
                mDataHandler.sendMessage(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void setListener() {
        mProfile.setOnClickListener(this);
        mHotMap.setOnClickListener(this);
        mChangeChart1.setOnClickListener(this);
        mChangeChart2.setOnClickListener(this);
    }

    private void dataInitial() {
        Time t = new Time();
        t.setToNow();
        currentHour = t.hour;
        currentMin = t.minute;
        if (currentMin < 10) {
            mTime.setText(String.valueOf(currentHour) + ": " + "0" + String.valueOf(currentMin));
        } else {
            mTime.setText(String.valueOf(currentHour) + ": " + String.valueOf(currentMin));
        }
        mCity.setText(Const.cityName[(int) (Math.random() * Const.cityName.length)]);
        mAirQuality.setText(DataFaker.setAirQualityText(PMDensity));
        mAirQuality.setTextColor(DataFaker.setAirQualityColor(PMDensity));
        mHint.setText(DataFaker.setHeathHintText(PMDensity));
        mHint.setTextColor(DataFaker.setHeathHintColor(PMDensity));
        mHourPM.setText(String.valueOf(PMDensity));
        mDayPM.setText(String.valueOf(PMDensity * 2));
        mWeekPM.setText(String.valueOf(PMDensity * 7));
        mChart1.setLineChartData(DataFaker.setDataForChart1());
        mChart2.setLineChartData(DataFaker.setDataForChart1());
    }

    private void taskInitial() {
        //GPS Task
        GPSInitial();
        //clock task
        if (isClockTaskRun == false) {
            isClockTaskRun = true;
            clockTask = new ClockTask();
            clockTask.execute(1);
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.main_profile:
                mProfile.setSelected(true);
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.toggle();
                break;
            case R.id.main_hot_map:
                Intent intent = new Intent(getActivity(), HotMapActivity.class);
                startActivity(intent);
                break;
            case R.id.main_chart_1_change:
                break;
            case R.id.main_chart_2_change:
                break;
            default:
                break;
        }
    }

    private class ClockTask extends AsyncTask<Integer, Integer, Integer> {

        @Override
        protected Integer doInBackground(Integer... integers) {
            while (true) {
                Time t = new Time();
                t.setToNow();
                //if current time matches the download time array.
                if (t.minute == currentMin + 1 || t.hour == currentHour + 1 || (t.hour == 0 && currentMin == 59)) {
                    currentMin = t.minute;
                    currentHour = t.hour;
                    mClockHandler.sendEmptyMessage(1);
                }
            }
        }
    }

    private void GPSInitial(){
        mManager = (LocationManager)mActivity.getSystemService(Context.LOCATION_SERVICE);
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
                        if (isDataTaskRun == false){
                            Log.e("onLocationChanged","searchPMRequest");
                            searchPMRequest(String.valueOf(longitude),String.valueOf(latitude));
                        }
                    }
                }
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {
                Log.e("onProviderEnabled",s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.e("onProviderDisabled",s);
                Toast.makeText(mActivity.getApplicationContext(), Const.ERROR_NO_GPS,
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
                loadingDialog.dismiss();
                isDataTaskRun = false;
                try {
                    aCache.put(Const.Cache_PM_State,response);
                    pmModel = PMModel.parse(response);
                    Message data = new Message();
                    data.what = Const.Handler_PM_Data;
                    data.obj = pmModel;
                    mDataHandler.sendMessage(data);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.e("searchPMRequest resp", response.toString());
                Toast.makeText(mActivity.getApplicationContext(), "Data Get Success!", Toast.LENGTH_LONG).show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loadingDialog.dismiss();
                isDataTaskRun  = false;
                Toast.makeText(mActivity.getApplicationContext(), "Data Get Fail!", Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * Get and Update Current City Name.
     * @param lati
     * @param Longi
     */
    private void searchCityRequest(String lati, String Longi){
        String url = HttpUtil.SearchCity_url;
        url = url+"&location="+lati+","+Longi+"&key="+Const.APP_MAP_KEY;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(mActivity.getApplicationContext(), Const.ERROR_NO_CITY_RESULT, Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("MainFragment","onDestrory");
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("MainFragment","onPause");
    }
}
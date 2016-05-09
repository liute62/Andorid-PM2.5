package com.example.pm;


import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.model.PMModel;
import app.services.DBService;
import app.utils.ACache;
import app.utils.StableCache;
import app.utils.ChartsConst;
import app.utils.Const;
import app.utils.DataGenerator;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;
import app.view.widget.DialogConfirm;
import app.view.widget.DialogGetCity;
import app.view.widget.DialogGetDensity;
import app.view.widget.DialogGetLocation;
import app.view.widget.LoadingDialog;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.AbstractChartView;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Running Sequence
 * Oncreate:
 * 1.params initial
 * 2.if DB service is not work, run it.
 * OncreateView:
 * 1.view initial
 * 2.listener initial
 * 3.cache initial, set cache value to data value
 * 4.data initial, bind the view with the data value
 * 5.chart initial, set the value of the chart when chart initial and changed.
 * 6.task initial, execute a threat to update the current time
 */
public class MainFragment extends Fragment implements OnClickListener {

    public static final String TAG = "MainFragment";
    //Todo, a icon to alert that user 60km away from station
    Activity mActivity;
    ImageView mProfile;
    ImageView mHotMap;
    ImageView mRefreshChart;
    TextView mTime;
    TextView mAirQuality;
    TextView mCity;
    TextView mHint;
    TextView mHourPM;
    TextView mDayPM;
    TextView mWeekPM;
    TextView mChangeChart1;
    TextView mChangeChart2;
    TextView mChart1Title;
    TextView mChart2Title;
    ImageView mChart2Hint;
    ImageView mDensityError;
    ImageView mRunError;
    ImageView mChart1Alert;
    ImageView mChart2Alert;
    ImageView mAddCity;
    TextView mViewMore2;

    Double PMDensity;
    Double PMBreatheHour;
    Double PMBreatheDay;
    Double PMBreatheWeekAvg;
    int currentHour;
    int currentMin;
    String currentLatitude;
    String currentLongitude;
    String currentCity;
    ClockTask clockTask;
    boolean isClockTaskRun = false;

    LoadingDialog loadingDialog;
    PMModel pmModel;
    ACache aCache;
    StableCache stableCache;
    private IntentFilter intentFilter;
    /**
     * Charts*
     */
    int current_chart1_index;
    int current_chart2_index;
    ColumnChartView mChart1column;
    LineChartView mChart1line;
    ColumnChartView mChart2column;
    LineChartView mChart2line;
    /**
     * Charts data
     **/
    HashMap<Integer, Float> chartData1; //data for chart 1
    HashMap<Integer, Float> chartData2; //data for chart 2
    HashMap<Integer, Float> chartData3; //data for chart 3
    HashMap<Integer, Float> chartData4; //data for chart 4
    HashMap<Integer, Float> chartData5; //data for chart 5
    HashMap<Integer, Float> chartData6; //data for chart 6
    HashMap<Integer, Float> chartData7; //data for chart 7
    List<String> chart7Date;           //date(mm.dd) for chart 7
    HashMap<Integer, Float> chartData8; //data for chart 8
    ArrayList<String> chart8Time;
    HashMap<Integer, Float> chartData10; //data for chart 10
    HashMap<Integer, Float> chartData12; //data for chart 12
    List<String> chart12Date;           //date(mm.dd) for chart 12

    private DBServiceReceiver dbReceiver;
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

    Handler mDataHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case Const.Handler_PM_Density:
                    PMModel data1 = (PMModel) msg.obj;
                    PMDensity = Double.valueOf(data1.getPm25());
                    //dataInitial();
                    text1Initial();
                    break;
                case Const.Handler_PM_Data:
                    PMModel data = (PMModel) msg.obj;
                    if (ShortcutUtil.isStringOK(data.getPm_breath_hour())) {
                        PMBreatheHour = Double.valueOf(data.getPm_breath_hour());
                        aCache.put(Const.Cache_PM_LastHour, PMBreatheHour);
                    }
                    if (ShortcutUtil.isStringOK(data.getPm_breath_today())) {
                        PMBreatheDay = Double.valueOf(data.getPm_breath_today());
                        aCache.put(Const.Cache_PM_LastDay, PMBreatheDay);
                    }
                    if (ShortcutUtil.isStringOK(data.getPm_breath_week())) {
                        PMBreatheWeekAvg = Double.valueOf(data.getPm_breath_week());
                        aCache.put(Const.Cache_PM_LastWeek, PMBreatheWeekAvg);
                    }
                    text2Initial();
                    break;
                case Const.Handler_City_Name:
                    String name = (String)msg.obj;
                    currentCity = name;
                    mAddCity.setVisibility(View.GONE);
                    mCity.setText(currentCity);
                    aCache.put(Const.Cache_City,currentCity);
                    break;
                case Const.Handler_Add_City:
                    String name2 = (String)msg.obj;
                    currentCity = name2;
                    mAddCity.setVisibility(View.GONE);
                    mCity.setText(currentCity);
                    break;
                case Const.Handler_Refresh_All:
                    break;
            }

        }
    };

    @Override
    public void onPause() {
        Log.d(TAG,"onPause");
        mActivity.unregisterReceiver(dbReceiver);
        aCache.put(Const.Cache_Is_Background, "true");
        aCache.put(Const.Cache_Pause_Time, String.valueOf(System.currentTimeMillis()));
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mActivity != null) {
            dbReceiver = new DBServiceReceiver();
            intentFilter = new IntentFilter();
            intentFilter.addAction(Const.Action_DB_Running_State);
            intentFilter.addAction(Const.Action_DB_MAIN_PMDensity);
            intentFilter.addAction(Const.Action_DB_MAIN_PMResult);
            intentFilter.addAction(Const.Action_Chart_Cache);
            intentFilter.addAction(Const.Action_Chart_Result_1);
            intentFilter.addAction(Const.Action_Chart_Result_2);
            intentFilter.addAction(Const.Action_Chart_Result_3);
            intentFilter.addAction(Const.Action_DB_MAIN_Location);
            aCache.put(Const.Cache_Is_Background, "false");
            String tmpTime = aCache.getAsString(Const.Cache_Pause_Time);
            //Todo if longer than 30 min
            long curTime = System.currentTimeMillis();
            if(tmpTime != null && Long.valueOf(tmpTime) - curTime > 1){
                Log.d(TAG,"CurrentTime "+ShortcutUtil.refFormatNowDate(curTime));
                Const.CURRENT_NEED_REFRESH = true;
            }
            mActivity.registerReceiver(dbReceiver, intentFilter);
        }
        Time t = new Time();
        t.setToNow();
        currentMin = t.minute;
        currentHour = t.hour;
        mClockHandler.sendEmptyMessage(1);
        checkForRefresh();
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        ShortcutUtil.removeNormalScreenShots();
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        PMDensity = 0.0;
        PMBreatheDay = 0.0;
        PMBreatheHour = 0.0;
        PMBreatheWeekAvg = 0.0;
        currentLongitude = null;
        currentLatitude = null;
        current_chart1_index = 1;
        current_chart2_index = 2;
        isClockTaskRun = false;
        chart7Date = new ArrayList<>();
        chart12Date = new ArrayList<>();
        chartData1 = new HashMap<>();
        chartData2 = new HashMap<>();
        chartData3 = new HashMap<>();
        chartData4 = new HashMap<>();
        chartData5 = new HashMap<>();
        chartData6 = new HashMap<>();
        chartData7 = new HashMap<>();
        chartData8 = new HashMap<>();
        chart8Time = new ArrayList<>();
        chartData10 = new HashMap<>();
        chartData12 = new HashMap<>();
        pmModel = new PMModel();
        loadingDialog = new LoadingDialog(getActivity());
        aCache = ACache.get(mActivity.getApplicationContext());
        stableCache = StableCache.getInstance(mActivity);
        ShortcutUtil.calStaticBreath(stableCache.getAsString(Const.Cache_User_Weight));
        //GPS Task
        if (!ShortcutUtil.isServiceWork(mActivity, Const.Name_DB_Service)) {
            dbReceiver = new DBServiceReceiver();
            intentFilter = new IntentFilter();
            intentFilter.addAction(Const.Action_DB_Running_State);
            intentFilter.addAction(Const.Action_DB_MAIN_PMDensity);
            intentFilter.addAction(Const.Action_DB_MAIN_PMResult);
            intentFilter.addAction(Const.Action_Chart_Cache);
            intentFilter.addAction(Const.Action_Chart_Result_1);
            intentFilter.addAction(Const.Action_Chart_Result_2);
            intentFilter.addAction(Const.Action_Chart_Result_3);
            intentFilter.addAction(Const.Action_DB_MAIN_Location);
            if(mActivity != null) {
                mActivity.registerReceiver(dbReceiver, intentFilter);
                intentFilter = new IntentFilter();
                Intent mIntent = new Intent(mActivity, DBService.class);
                mActivity.startService(mIntent);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mProfile = (ImageView) view.findViewById(R.id.main_profile);
        mHotMap = (ImageView) view.findViewById(R.id.main_hot_map);
        mRefreshChart = (ImageView)view.findViewById(R.id.main_refresh_chart);
        mTime = (TextView) view.findViewById(R.id.main_current_time);
        mAirQuality = (TextView) view.findViewById(R.id.main_air_quality);
        mCity = (TextView) view.findViewById(R.id.main_current_city);
        mHint = (TextView) view.findViewById(R.id.main_hint);
        mHourPM = (TextView) view.findViewById(R.id.main_hour_pm);
        mDayPM = (TextView) view.findViewById(R.id.main_day_pm);
        mWeekPM = (TextView) view.findViewById(R.id.main_week_pm);
        mDensityError = (ImageView)view.findViewById(R.id.main_density_error);
        mRunError = (ImageView)view.findViewById(R.id.main_run_error);
        mChart2Hint = (ImageView)view.findViewById(R.id.main_chart_hint_2);
        mChart1column = (ColumnChartView) view.findViewById(R.id.main_chart_1_column);
        mChart1line = (LineChartView) view.findViewById(R.id.main_chart_1_line);
        mChart2column = (ColumnChartView) view.findViewById(R.id.main_chart_2_column);
        mChart2line = (LineChartView) view.findViewById(R.id.main_chart_2_line);
        mChangeChart1 = (TextView) view.findViewById(R.id.main_chart_1_change);
        mChangeChart2 = (TextView) view.findViewById(R.id.main_chart_2_change);
        mChart1Title = (TextView) view.findViewById(R.id.main_chart1_title);
        mChart2Title = (TextView) view.findViewById(R.id.main_chart2_title);
        mChart1Alert = (ImageView) view.findViewById(R.id.main_chart_1_alert);
        mChart2Alert = (ImageView) view.findViewById(R.id.main_chart_2_alert);
        mAddCity = (ImageView)view.findViewById(R.id.main_add_city);
        mViewMore2 = (TextView)view.findViewById(R.id.main_view_more_2);
        setFonts(view);
        setTextSizeByWidth();
        setListener();
        cacheInitial();
        dataInitial();
        chartInitial(current_chart1_index, current_chart2_index);
        taskInitial();
        return view;
    }

    private void checkForRefresh(){
        String refresh = aCache.getAsString(Const.Cache_DB_Lastime_Refresh);
        if(!ShortcutUtil.isStringOK(refresh)){
            aCache.put(Const.Cache_DB_Lastime_Refresh,String.valueOf(System.currentTimeMillis()));
        }else {
            long refTime = 0;
            long curTime = 0;
            try {
                refTime = Long.valueOf(refresh);
                curTime = System.currentTimeMillis();
            }catch (Exception e){
                FileUtil.appendErrorToFile(0,"checkForRefresh error "+refresh);
            }
            Log.e(TAG,"checkForRefresh ref: "+ShortcutUtil.refFormatDateAndTime(refTime)+" cur: "+ShortcutUtil.refFormatDateAndTime(curTime));
            if(curTime - refTime > Const.Refresh_Chart_Interval){
                //notify the service to refresh the chart
                Intent intent = new Intent(Const.Action_Refresh_Chart_ToService);
                mActivity.sendBroadcast(intent);
            }
            aCache.put(Const.Cache_DB_Lastime_Refresh,String.valueOf(curTime));
        }
    }

    private void setListener() {
        mProfile.setOnClickListener(this);
        mHotMap.setOnClickListener(this);
        mRefreshChart.setOnClickListener(this);
        mChangeChart1.setOnClickListener(this);
        mChangeChart2.setOnClickListener(this);
        mAddCity.setOnClickListener(this);
        mViewMore2.setOnClickListener(this);
        mCity.setOnClickListener(this);
        mAirQuality.setOnClickListener(this);
    }

    private void setFonts(View view) {
//        Typeface typeFace = Typeface.createFromAsset(mActivity.getAssets(), "SourceHanSansCNLight.ttf");
        Typeface typeFace = Typeface.createFromAsset(mActivity.getAssets(), "kaiti.TTF");
        TextView textView1 = (TextView) view.findViewById(R.id.textView1);
        textView1.setTypeface(typeFace);
        mHint.setTypeface(typeFace);
        mAirQuality.setTypeface(typeFace);
        mCity.setTypeface(typeFace);
        mChangeChart1.setTypeface(typeFace);
        mChangeChart2.setTypeface(typeFace);
    }

    private void cacheInitial() {
        String access_token = aCache.getAsString(Const.Cache_Access_Token);
        String user_name = aCache.getAsString(Const.Cache_User_Name);
        String user_nickname = aCache.getAsString(Const.Cache_User_Nickname);
        String user_gender = aCache.getAsString(Const.Cache_User_Gender);
        String density = aCache.getAsString(Const.Cache_PM_Density);
        String pm_hour = aCache.getAsString(Const.Cache_PM_LastHour);
        String pm_day = aCache.getAsString(Const.Cache_PM_LastDay);
        String pm_week = aCache.getAsString(Const.Cache_PM_LastWeek);
        String longitude = aCache.getAsString(Const.Cache_Longitude);
        String latitude = aCache.getAsString(Const.Cache_Latitude);
        String city = aCache.getAsString(Const.Cache_City);
        if (ShortcutUtil.isStringOK(access_token))
            Const.CURRENT_ACCESS_TOKEN = access_token;
        if (ShortcutUtil.isStringOK(user_name))
            Const.CURRENT_USER_NAME = user_name;
        if (ShortcutUtil.isStringOK(user_nickname))
            Const.CURRENT_USER_NICKNAME = user_nickname;
        if (ShortcutUtil.isStringOK(user_gender))
            Const.CURRENT_USER_GENDER = user_gender;
        if (ShortcutUtil.isStringOK(density))
            PMDensity = Double.valueOf(density);
        if (ShortcutUtil.isStringOK(pm_hour))
            PMBreatheHour = Double.valueOf(pm_hour);
        if (ShortcutUtil.isStringOK(pm_day))
            PMBreatheDay = Double.valueOf(pm_day);
        if (ShortcutUtil.isStringOK(pm_week))
            PMBreatheWeekAvg = Double.valueOf(pm_week);
        if (ShortcutUtil.isStringOK(longitude))
            currentLongitude = longitude;
        if (ShortcutUtil.isStringOK(latitude))
            currentLatitude = latitude;
        if (ShortcutUtil.isStringOK(city)) {
            currentCity = city;
            mAddCity.setVisibility(View.GONE);
        }else {
            currentCity = "null";
            mAddCity.setVisibility(View.VISIBLE);
        }
        /*********Chart Data Initial**********/
        Object chart1 = aCache.getAsObject(Const.Cache_Chart_1);
        Object chart2 = aCache.getAsObject(Const.Cache_Chart_2);
        Object chart3 = aCache.getAsObject(Const.Cache_Chart_3);
        Object chart4 = aCache.getAsObject(Const.Cache_Chart_4);
        Object chart5 = aCache.getAsObject(Const.Cache_Chart_5);
        Object chart6 = aCache.getAsObject(Const.Cache_Chart_6);
        Object chart7 = aCache.getAsObject(Const.Cache_Chart_7);
        Object chart8 = aCache.getAsObject(Const.Cache_Chart_8);
        Object chart10 = aCache.getAsObject(Const.Cache_Chart_10);
        Object chart12 = aCache.getAsObject(Const.Cache_Chart_12);
        if (chart1 != null) chartData1 = (HashMap<Integer, Float>) chart1;
        if (chart2 != null) chartData2 = (HashMap<Integer, Float>) chart2;
        if (chart3 != null) chartData3 = (HashMap<Integer, Float>) chart3;
        if (chart4 != null) chartData4 = (HashMap<Integer, Float>) chart4;
        if (chart5 != null) chartData5 = (HashMap<Integer, Float>) chart5;
        if (chart6 != null) chartData6 = (HashMap<Integer, Float>) chart6;
        if (chart7 != null) {
            chartData7 = (HashMap<Integer, Float>) chart7;
            chart7Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_7_Date);
        }
        if (chart8 != null) chartData8 = (HashMap<Integer, Float>) chart8;
        if (chart10 != null) chartData10 = (HashMap<Integer, Float>) chart10;
        if (chart12 != null) {
            chartData12 = (HashMap<Integer, Float>) chart12;
            chart12Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_12_Date);
        }
        Object chart8obj = aCache.getAsObject(Const.Cache_Chart_8_Time);
        if(chart8obj != null)
            chart8Time = (ArrayList) chart8obj;
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
        mCity.setText(currentCity);
        text1Initial();
        text2Initial();
     }

    private void text1Initial(){
        mAirQuality.setText(DataGenerator.setAirQualityText(PMDensity));
        mAirQuality.setTextColor(DataGenerator.setAirQualityColor(PMDensity));
        mHint.setText(DataGenerator.setHeathHintText(PMDensity));
        mHint.setTextColor(DataGenerator.setHeathHintColor(PMDensity));
    }

    private void text2Initial(){
        String ugStr = mActivity.getResources().getString(R.string.str_ug);
        mHourPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheHour, 2)) +" "+ugStr);
        mDayPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheDay, 1)) + " "+ugStr);
        mWeekPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheWeekAvg, 1))+" "+ugStr);

    }

    private void chartInitial(int chart1_index, int chart2_index) {
        mChart1Title.setText(ChartsConst.Chart_title[chart1_index]);
        if (ChartsConst.Chart_type[chart1_index] == 0) {
            mChart1column.setVisibility(View.VISIBLE);
            mChart1line.setVisibility(View.INVISIBLE);
            setChartViewport(mChart1column, setChartDataByIndex(chart1_index));
        } else if (ChartsConst.Chart_type[chart1_index] == 1) {
            mChart1column.setVisibility(View.INVISIBLE);
            mChart1line.setVisibility(View.VISIBLE);
            setChartViewport(mChart1line, setChartDataByIndex(chart1_index));
        } else {
            mChart1column.setVisibility(View.INVISIBLE);
            mChart1line.setVisibility(View.INVISIBLE);
        }
        mChart2Title.setText(ChartsConst.Chart_title[chart2_index]);
        if (ChartsConst.Chart_type[chart2_index] == 0) {
            mChart2column.setVisibility(View.VISIBLE);
            mChart2line.setVisibility(View.INVISIBLE);
            setChartViewport(mChart2column, setChartDataByIndex(chart2_index));
        } else if (ChartsConst.Chart_type[chart2_index] == 1) {
            mChart2column.setVisibility(View.INVISIBLE);
            mChart2line.setVisibility(View.VISIBLE);
            setChartViewport(mChart2line, setChartDataByIndex(chart2_index));
        } else {
            mChart2column.setVisibility(View.INVISIBLE);
            mChart2line.setVisibility(View.INVISIBLE);
        }
    }

    private void taskInitial() {
        //clock task
        if (isClockTaskRun == false) {
            isClockTaskRun = true;
            clockTask = new ClockTask();
            clockTask.execute(1);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_air_quality:
                DialogGetDensity getDensity = new DialogGetDensity(mActivity);
                getDensity.show();
                break;
            case R.id.main_current_city:
                DialogGetCity dialog2 = new DialogGetCity(mActivity,mDataHandler);
                dialog2.show();
                break;
            case R.id.main_add_city:
                DialogGetCity dialog = new DialogGetCity(mActivity,mDataHandler);
                dialog.show();
                break;
            case R.id.main_profile:
                mProfile.setSelected(true);
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.toggle();
                break;
            case R.id.main_hot_map:
                Intent intent = new Intent(getActivity(), MapActivity.class);
                startActivity(intent);
                break;
            case R.id.main_refresh_chart:
                Intent intentR = new Intent(Const.Action_Refresh_Chart_ToService);
                mActivity.sendBroadcast(intentR);
                break;
            case R.id.main_chart_1_change:
                if (current_chart1_index == 7)
                    current_chart1_index = 1;
                 else
                    current_chart1_index += 2;
                boolean result1 = Const.Chart_Alert_Show[current_chart1_index];
                if(result1 == true){
                    mChart1Alert.setVisibility(View.VISIBLE);
                    mChart1Alert.setOnClickListener(this);
                }else mChart1Alert.setVisibility(View.GONE);
                mChart1Title.setText(ChartsConst.Chart_title[current_chart1_index]);
                chartInitial(current_chart1_index, current_chart2_index);
                break;
            case R.id.main_chart_2_change:
                if (current_chart2_index == 12)
                    current_chart2_index = 2;
                else
                    current_chart2_index += 2;
                if(current_chart2_index == 8) mViewMore2.setVisibility(View.VISIBLE);
                else mViewMore2.setVisibility(View.GONE);
                boolean result2 = Const.Chart_Alert_Show[current_chart2_index];
                if(result2 == true){
                    mChart2Alert.setVisibility(View.VISIBLE);
                    mChart2Alert.setOnClickListener(this);
                }else mChart2Alert.setVisibility(View.GONE);
                mChart2Title.setText(ChartsConst.Chart_title[current_chart2_index]);
                chartInitial(current_chart1_index, current_chart2_index);
                break;
            case R.id.main_chart_hint_2:
                Toast.makeText(mActivity.getApplicationContext(),Const.Info_Chart_Data_Lost,Toast.LENGTH_SHORT).show();
                break;
            case R.id.main_density_error:
                Toast.makeText(mActivity.getApplicationContext(),Const.Info_DB_Not_Location,Toast.LENGTH_SHORT).show();
                DialogGetDensity dialogGetDensity = new DialogGetDensity(mActivity);
                dialogGetDensity.show();
                break;
            case R.id.main_run_error:
                Toast.makeText(mActivity.getApplicationContext(),Const.Info_DB_Not_Running,Toast.LENGTH_SHORT).show();
                DialogGetLocation getLocation = new DialogGetLocation(mActivity);
                getLocation.show();
                break;
            case R.id.main_chart_1_alert:
                Toast.makeText(mActivity.getApplicationContext(),Const.Info_Data_Lost,Toast.LENGTH_SHORT).show();
                break;
            case R.id.main_chart_2_alert:
                Toast.makeText(mActivity.getApplicationContext(),Const.Info_Data_Lost,Toast.LENGTH_SHORT).show();
                break;
            case R.id.main_view_more_2:
                String start = mActivity.getResources().getString(R.string.str_no_data);
                String end = mActivity.getResources().getString(R.string.str_no_data);
                if(chart8Time.size() >= 2){
                    start  = chart8Time.get(0);
                    end = chart8Time.get(chart8Time.size() - 1);
                }
                String lastTwoHour = mActivity.getResources().getString(R.string.str_last_two_hour);
                String from = mActivity.getResources().getString(R.string.str_from);
                String to = mActivity.getResources().getString(R.string.str_to);
                DialogConfirm confirm = new DialogConfirm(mActivity,lastTwoHour,from+": "+start+to+": "+end);
                confirm.show();
                confirm.setAllDismissListener();
                break;
            default:
                break;
        }
    }

    /**
     * A light-weight thread to update the current time view
     */
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

        @Override
        protected void onCancelled() {
            isClockTaskRun = false;
            super.onCancelled();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            isClockTaskRun = false;
            super.onPostExecute(integer);
        }
    }


    /**
     * Get and Update Current City Name.
     *
     * @param lati  latitude
     * @param Longi longitude
     */
    private void searchCityRequest(String lati, final String Longi) {
        String url = HttpUtil.SearchCity_url;
        url = url + "&location=" + lati + "," + Longi + "&ak=" + Const.APP_MAP_KEY;
        Log.e(TAG,"searchCityRequest "+url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject result = response.getJSONObject("result");
                    JSONObject component = result.getJSONObject("addressComponent");
                    String cityName = component.getString("city");
                    FileUtil.appendStrToFile(-2,"5.search city success and city == "+cityName);
                    if (cityName != null && !cityName.trim().equals("")) {
                        Message msg = new Message();
                        msg.what = Const.Handler_City_Name;
                        msg.obj = cityName;
                        mDataHandler.sendMessage(msg);
                        aCache.put(Const.Cache_City, cityName);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String bg = aCache.getAsString(Const.Cache_Is_Background);
                if(ShortcutUtil.isStringOK(bg) && bg.equals("false"))
                   Toast.makeText(mActivity.getApplicationContext(), Const.ERROR_NO_CITY_RESULT, Toast.LENGTH_SHORT).show();
            }

        });
        VolleyQueue.getInstance(mActivity.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }


    /**
     * Receive the data from DBService
     */
    public class DBServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(Const.Action_DB_Running_State)){
                int state = intent.getIntExtra(Const.Intent_DB_Run_State,0);
                if(state == 1){
                    mDensityError.setVisibility(View.VISIBLE);
                    mDensityError.setOnClickListener(MainFragment.this);
                }else if(state == -1){
                    mRunError.setVisibility(View.VISIBLE);
                    mRunError.setOnClickListener(MainFragment.this);
                }else {
                    if(mDensityError.getVisibility() == View.VISIBLE)
                        mDensityError.setVisibility(View.GONE);
                    if(mRunError.getVisibility() == View.VISIBLE)
                        mRunError.setVisibility(View.GONE);
                }
                text1Initial();
            }
            if (intent.getAction().equals(Const.Action_DB_MAIN_PMDensity)) {
                //Update the density of PM
                if(mRunError.getVisibility() == View.VISIBLE)
                    mRunError.setVisibility(View.GONE);
                PMModel model = new PMModel();
                model.setPm25(intent.getStringExtra(Const.Intent_PM_Density));
                Message data = new Message();
                data.what = Const.Handler_PM_Density;
                data.obj = model;
                mDataHandler.sendMessage(data);

            } else if (intent.getAction().equals(Const.Action_DB_MAIN_PMResult)) {
                //Update the calculated data of PM
                PMModel model = new PMModel();
                model.setPm_breath_hour(intent.getStringExtra(Const.Intent_DB_PM_Hour));
                model.setPm_breath_today(intent.getStringExtra(Const.Intent_DB_PM_Day));
                model.setPm_breath_week(intent.getStringExtra(Const.Intent_DB_PM_Week));
                Message data = new Message();
                data.what = Const.Handler_PM_Data;
                data.obj = model;
                mDataHandler.sendMessage(data);
            } else if (intent.getAction().equals(Const.Action_Chart_Cache)) {
                chartData1 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_1);
                chartData2 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_2);
                chartData3 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_3);
                chartData4 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_4);
                chartData5 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_5);
                chartData6 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_6);
                chartData7 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_7);
                chart7Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_7_Date);
                chartData8 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_8);
                chart8Time = (ArrayList) aCache.getAsObject(Const.Cache_Chart_8_Time);
                chartData10 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_10);
                chartData12 = (HashMap<Integer, Float>) aCache.getAsObject(Const.Cache_Chart_12);
                chart12Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_12_Date);
                chartInitial(current_chart1_index, current_chart2_index);
            } else if (intent.getAction().equals(Const.Action_DB_MAIN_Location)) {
                String lati = intent.getStringExtra(Const.Intent_DB_PM_Lati);
                String longi = intent.getStringExtra(Const.Intent_DB_PM_Longi);
                int isRefresh = intent.getIntExtra(Const.Intent_DB_City_Ref,0);
                String last_lati = aCache.getAsString(Const.Cache_Latitude);
                String last_longi = aCache.getAsString(Const.Cache_Longitude);
                Log.e(TAG,"lati "+lati+" last_lati"+last_lati+" longi"+longi+" last_longi"+last_longi);
                if (last_lati == null || last_longi == null) {
                    aCache.put(Const.Cache_Latitude, lati);
                    aCache.put(Const.Cache_Longitude, longi);
                    searchCityRequest(lati, longi);
                } else {
                    if (last_lati.equals(lati) && last_longi.equals(longi)) {
                        //no change
                        //for safe, if current city == null. search City anyway
                        String city = mCity.getText().toString();
                        if(city == null || city.equals("null") || isRefresh == 1){
                            searchCityRequest(lati,longi);
                        }
                    } else {
                        aCache.put(Const.Cache_Latitude, lati);
                        aCache.put(Const.Cache_Longitude, longi);
                        searchCityRequest(lati, longi);
                    }
                }

            } else if (intent.getAction().equals(Const.Action_Chart_Result_1)) {
                HashMap data4 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart4_data);
                chartData4 = data4;
                aCache.put(Const.Cache_Chart_4, chartData4);
                HashMap data5 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart5_data);
                chartData5 = data5;
                aCache.put(Const.Cache_Chart_5, chartData5);
                HashMap data8 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart8_data);
                chartData8 = data8;
                aCache.put(Const.Cache_Chart_8, chartData8);
                chart8Time = (ArrayList) aCache.getAsObject(Const.Cache_Chart_8_Time);
                chartInitial(current_chart1_index, current_chart2_index);
            } else if (intent.getAction().equals(Const.Action_Chart_Result_2)) {
                HashMap data1 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart1_data);
                chartData1 = data1;
                aCache.put(Const.Cache_Chart_1, chartData1);
                HashMap data2 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart2_data);
                chartData2 = data2;
                aCache.put(Const.Cache_Chart_2, chartData2);
                HashMap data3 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart3_data);
                chartData3 = data3;
                aCache.put(Const.Cache_Chart_3, chartData3);
                HashMap data6 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart6_data);
                chartData6 = data6;
                aCache.put(Const.Cache_Chart_6, chartData6);
                HashMap data10 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart10_data);
                chartData10 = data10;
                aCache.put(Const.Cache_Chart_10, chartData10);
                chartInitial(current_chart1_index, current_chart2_index);
            } else if (intent.getAction().equals(Const.Action_Chart_Result_3)) {
                HashMap data7 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart7_data);
                ArrayList date7 = (ArrayList) intent.getExtras().getSerializable(Const.Intent_chart_7_data_date);
                chartData7 = data7;
                aCache.put(Const.Cache_Chart_7, chartData7);
                aCache.put(Const.Cache_Chart_7_Date, date7);
                HashMap data12 = (HashMap) intent.getExtras().getSerializable(Const.Intent_chart12_data);
                ArrayList date12 = (ArrayList) intent.getExtras().getSerializable(Const.Intent_chart_12_data_date);
                chartData12 = data12;
                aCache.put(Const.Cache_Chart_12, chartData12);
                aCache.put(Const.Cache_Chart_12_Date, date12);
                chartInitial(current_chart1_index, current_chart2_index);
            }
        }
    }

    private Object setChartDataByIndex(int index) {
        switch (index) {
            case 1:
                return DataGenerator.chart1DataGenerator(chartData1);
            case 2:
                return DataGenerator.chart2DataGenerator(chartData2);
            case 3:
                return DataGenerator.chart3DataGenerator(chartData3);
            case 4:
                return DataGenerator.chart4DataGenerator(chartData4);
            case 5:
                return DataGenerator.chart5DataGenerator(chartData5);
            case 6:
                return DataGenerator.chart6DataGenerator(chartData6);
            case 7:
                return DataGenerator.chart7DataGenerator(chartData7, chart7Date);
            case 8:
                return DataGenerator.chart8DataGenerator(chartData8);
            case 10:
                return DataGenerator.chart10DataGenerator(chartData10);
            case 12:
                return DataGenerator.chart12DataGenerator(chartData12, chart12Date);
        }
        return null;
    }

    private Viewport calChartViewport(AbstractChartView view,Object data){
        view.setViewportCalculationEnabled(true);
        if(view instanceof LineChartView){
            ((LineChartView) view).setLineChartData((LineChartData)data);
        }else if(view instanceof ColumnChartView) {
            ((ColumnChartView) view).setColumnChartData((ColumnChartData) data);
        }
        view.resetViewports();
        return view.getCurrentViewport();
    }

    private void setChartViewport(AbstractChartView view,Object data){
        final Viewport v = calChartViewport(view,data);
        v.top = v.top * 1.2f;
        v.bottom = 0;
        view.setMaximumViewport(v);
        view.setCurrentViewport(v);
        view.setViewportCalculationEnabled(false);
    }

    private void setTextSizeByWidth(){
        int width = Const.CURRENT_WIDTH;
        if(width == -1) return;
        if(width <= Const.Resolution_Small){
            mChart1Title.setTextSize(12);
            mChart2Title.setTextSize(12);
        }
    }

}
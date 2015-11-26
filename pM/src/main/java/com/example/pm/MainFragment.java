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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import app.model.ChartModel;
import app.model.PMModel;
import app.services.DBService;
import app.utils.ACache;
import app.utils.ChartsConst;
import app.utils.Const;
import app.utils.DataGenerator;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;
import app.view.widget.LoadingDialog;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.Viewport;
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

    Activity mActivity;
    ImageView mProfile;
    ImageView mHotMap;
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

    Double PMDensity;
    Double PMBreatheHour;
    Double PMBreatheDay;
    Double PMBreatheWeekAvg;
    int currentHour;
    int currentMin;
    private String currentLatitude;
    private String currentLongitude;
    private String currentCity;
    ClockTask clockTask;
    boolean isClockTaskRun = false;

    LoadingDialog loadingDialog;
    PMModel pmModel;
    ACache aCache;
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
    /**Charts data**/
    HashMap<Integer,Float> chartData1; //data for chart 1
    HashMap<Integer,Float> chartData2; //data for chart 2
    HashMap<Integer,Float> chartData3; //data for chart 3
    HashMap<Integer,Float> chartData4; //data for chart 4
    HashMap<Integer,Float> chartData5; //data for chart 5
    HashMap<Integer,Float> chartData6; //data for chart 6
    HashMap<Integer,Float> chartData7; //data for chart 7
    List<String> chart7Date;           //date(mm.dd) for chart 7
    HashMap<Integer,Float> chartData8; //data for chart 8
    HashMap<Integer,Float> chartData10; //data for chart 10
    HashMap<Integer,Float> chartData12; //data for chart 12
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
            if (msg.what == Const.Handler_PM_Density) {
                PMModel data = (PMModel) msg.obj;
                PMDensity = Double.valueOf(data.getPm25());
                dataInitial();
            }
            if (msg.what == Const.Handler_PM_Data) {
                PMModel data = (PMModel) msg.obj;
                PMBreatheHour = Double.valueOf(data.getPm_breath_hour());
                PMBreatheDay = Double.valueOf(data.getPm_breath_today());
                PMBreatheWeekAvg = Double.valueOf(data.getPm_breath_week());
                //meanwhile update the cache
                aCache.put(Const.Cache_PM_LastHour,PMBreatheHour);
                aCache.put(Const.Cache_PM_LastDay,PMBreatheDay);
                aCache.put(Const.Cache_PM_LastWeek,PMBreatheWeekAvg);
                dataInitial();
            }
        }
    };

    @Override
    public void onPause() {
        mActivity.unregisterReceiver(dbReceiver);
        aCache.put(Const.Cache_Is_Background, "true");
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mActivity != null) {
            dbReceiver = new DBServiceReceiver();
            intentFilter = new IntentFilter();
            intentFilter.addAction(Const.Action_DB_MAIN_PMDensity);
            intentFilter.addAction(Const.Action_DB_MAIN_PMResult);
            intentFilter.addAction(Const.Action_Chart_Cache);
            intentFilter.addAction(Const.Action_Chart_Result_1);
            intentFilter.addAction(Const.Action_Chart_Result_2);
            intentFilter.addAction(Const.Action_Chart_Result_3);
            aCache.put(Const.Cache_Is_Background,"false");
            mActivity.registerReceiver(dbReceiver, intentFilter);
        }
        super.onResume();
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
        currentCity = "无";
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
        chartData10 = new HashMap<>();
        chartData12 = new HashMap<>();
        pmModel = new PMModel();
        loadingDialog = new LoadingDialog(getActivity());
        aCache = ACache.get(mActivity);
        //GPS Task
        if (!ShortcutUtil.isServiceWork(mActivity, Const.Name_DB_Service)) {
            dbReceiver = new DBServiceReceiver();
            intentFilter = new IntentFilter();
            intentFilter.addAction(Const.Action_DB_MAIN_PMDensity);
            intentFilter.addAction(Const.Action_DB_MAIN_PMResult);
            intentFilter.addAction(Const.Action_Chart_Cache);
            intentFilter.addAction(Const.Action_Chart_Result_1);
            intentFilter.addAction(Const.Action_Chart_Result_2);
            intentFilter.addAction(Const.Action_Chart_Result_3);
            mActivity.registerReceiver(dbReceiver, intentFilter);
            intentFilter = new IntentFilter();
            Intent mIntent = new Intent(mActivity, DBService.class);
            mActivity.startService(mIntent);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        mProfile = (ImageView) view.findViewById(R.id.main_profile);
        mHotMap = (ImageView) view.findViewById(R.id.main_hot_map);
        mTime = (TextView) view.findViewById(R.id.main_current_time);
        mAirQuality = (TextView) view.findViewById(R.id.main_air_quality);
        mCity = (TextView) view.findViewById(R.id.main_current_city);
        mHint = (TextView) view.findViewById(R.id.main_hint);
        mHourPM = (TextView) view.findViewById(R.id.main_hour_pm);
        mDayPM = (TextView) view.findViewById(R.id.main_day_pm);
        mWeekPM = (TextView) view.findViewById(R.id.main_week_pm);
        mChart1column = (ColumnChartView) view.findViewById(R.id.main_chart_1_column);
        mChart1line = (LineChartView) view.findViewById(R.id.main_chart_1_line);
        mChart2column = (ColumnChartView) view.findViewById(R.id.main_chart_2_column);
        mChart2line = (LineChartView) view.findViewById(R.id.main_chart_2_line);
        mChangeChart1 = (TextView) view.findViewById(R.id.main_chart_1_change);
        mChangeChart2 = (TextView) view.findViewById(R.id.main_chart_2_change);
        mChart1Title = (TextView) view.findViewById(R.id.main_chart1_title);
        mChart2Title = (TextView) view.findViewById(R.id.main_chart2_title);
        setFonts(view);
        setListener();
        cacheInitial();
        dataInitial();
        chartInitial(current_chart1_index, current_chart2_index);
        taskInitial();
        return view;
    }

    private void setListener() {
        mProfile.setOnClickListener(this);
        mHotMap.setOnClickListener(this);
        mChangeChart1.setOnClickListener(this);
        mChangeChart2.setOnClickListener(this);
    }

    private void setFonts(View view) {
        Typeface typeFace = Typeface.createFromAsset(mActivity.getAssets(), "SourceHanSansCNLight.ttf");
        TextView textView1 = (TextView) view.findViewById(R.id.textView1);
        textView1.setTypeface(typeFace);
    }

    private void cacheInitial() {
        String access_token = aCache.getAsString(Const.Cache_Access_Token);
        String user_id = aCache.getAsString(Const.Cache_User_Id);
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
        if(ShortcutUtil.isStringOK(access_token)){
            Const.CURRENT_ACCESS_TOKEN = access_token;
        }if(ShortcutUtil.isStringOK(user_id)){
            Const.CURRENT_USER_ID = user_id;
        }if(ShortcutUtil.isStringOK(user_name)){
            Const.CURRENT_USER_NAME = user_name;
        }if (ShortcutUtil.isStringOK(user_nickname)){
            Const.CURRENT_USER_NICKNAME = user_nickname;
        }if(ShortcutUtil.isStringOK(user_gender)){
            Const.CURRENT_USER_GENDER = user_gender;
        }

        if (ShortcutUtil.isStringOK(density)){
            PMDensity = Double.valueOf(density);
        }if(ShortcutUtil.isStringOK(pm_hour)){
            PMBreatheHour = Double.valueOf(pm_hour);
        }if(ShortcutUtil.isStringOK(pm_day)){
            PMBreatheDay = Double.valueOf(pm_day);
        }if(ShortcutUtil.isStringOK(pm_week)){
            PMBreatheWeekAvg = Double.valueOf(pm_week);
        }if (ShortcutUtil.isStringOK(longitude)){
            currentLongitude = longitude;
        }if (ShortcutUtil.isStringOK(latitude)){
            currentLatitude = latitude;
        }if (ShortcutUtil.isStringOK(city)){
            currentCity = city;
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
        if(chart1 != null) chartData1 = (HashMap<Integer,Float>) chart1;
        if(chart2 != null) chartData2 = (HashMap<Integer,Float>) chart2;
        if(chart3 != null) chartData3 = (HashMap<Integer,Float>) chart3;
        if(chart4 != null) chartData4 = (HashMap<Integer,Float>) chart4;
        if(chart5 != null) chartData5 = (HashMap<Integer,Float>) chart5;
        if(chart6 != null) chartData6 = (HashMap<Integer,Float>) chart6;
        if(chart7 != null) {
            chartData7 = (HashMap<Integer,Float>) chart7;
            chart7Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_7_Date);
        }
        if(chart8 != null) chartData8 = (HashMap<Integer,Float>) chart8;
        if(chart10 != null) chartData10 = (HashMap<Integer,Float>) chart10;
        if(chart12 != null) {
            chartData12 = (HashMap<Integer,Float>) chart12;
            chart12Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_12_Date);
        }
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
        mAirQuality.setText(DataGenerator.setAirQualityText(PMDensity));
        mAirQuality.setTextColor(DataGenerator.setAirQualityColor(PMDensity));
        mHint.setText(DataGenerator.setHeathHintText(PMDensity));
        mHint.setTextColor(DataGenerator.setHeathHintColor(PMDensity));
        mHourPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheHour, 2)) + " 微克");
        mDayPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheDay, 1)) + " 微克");
        mWeekPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheWeekAvg, 1)) + " 微克");
    }

    private void chartInitial(int chart1_index, int chart2_index) {
        mChart1Title.setText(ChartsConst.Chart_title[chart1_index]);
        if (ChartsConst.Chart_type[chart1_index] == 0) {
            mChart1column.setVisibility(View.VISIBLE);
            mChart1line.setVisibility(View.INVISIBLE);
            mChart1column.setColumnChartData((ColumnChartData) setChartDataByIndex(chart1_index));
        } else if (ChartsConst.Chart_type[chart1_index] == 1) {
            mChart1column.setVisibility(View.INVISIBLE);
            mChart1line.setVisibility(View.VISIBLE);
            mChart1line.setLineChartData((LineChartData) setChartDataByIndex(chart1_index));
        } else {
            mChart1column.setVisibility(View.INVISIBLE);
            mChart1line.setVisibility(View.INVISIBLE);
        }

        mChart2Title.setText(ChartsConst.Chart_title[chart2_index]);
        if (ChartsConst.Chart_type[chart2_index] == 0) {
            mChart2column.setVisibility(View.VISIBLE);
            mChart2line.setVisibility(View.INVISIBLE);
            ColumnChartData tmp = (ColumnChartData) setChartDataByIndex(chart2_index);
            mChart2column.setColumnChartData(tmp);
        } else if (ChartsConst.Chart_type[chart2_index] == 1) {
            mChart2column.setVisibility(View.INVISIBLE);
            mChart2line.setVisibility(View.VISIBLE);
            mChart2line.setLineChartData((LineChartData) setChartDataByIndex(chart2_index));
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
            case R.id.main_profile:
                mProfile.setSelected(true);
                MainActivity mainActivity = (MainActivity) getActivity();
                mainActivity.toggle();
                break;
            case R.id.main_hot_map:

                break;
            case R.id.main_chart_1_change:
                if (current_chart1_index == 7) {
                    current_chart1_index = 1;
                } else {
                    current_chart1_index += 2;
                }
                mChart1Title.setText(ChartsConst.Chart_title[current_chart1_index]);
                chartInitial(current_chart1_index, current_chart2_index);
                break;
            case R.id.main_chart_2_change:
                if (current_chart2_index == 12) {
                    current_chart2_index = 2;
                } else {
                    current_chart2_index += 2;
                }
                mChart2Title.setText(ChartsConst.Chart_title[current_chart2_index]);
                chartInitial(current_chart1_index, current_chart2_index);
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
    }


    /**
     * Get and Update Current City Name.
     *
     * @param lati  latitude
     * @param Longi longitude
     */
    private void searchCityRequest(String lati, String Longi) {
        String url = HttpUtil.SearchCity_url;
        url = url + "&location=" + lati + "," + Longi + "&key=" + Const.APP_MAP_KEY;
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


    /**
     * Receive the data from DBService
     */
    public class DBServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Const.Action_DB_MAIN_PMDensity)) {
                //Update the density of PM
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
            }else if(intent.getAction().equals(Const.Action_Chart_Cache)){
                chartData1 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_1);
                chartData2 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_2);
                chartData3 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_3);
                chartData4 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_4);
                chartData5 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_5);
                chartData6 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_6);
                chartData7 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_7);
                chart7Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_7_Date);
                chartData8 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_8);
                chartData10 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_10);
                chartData12 = (HashMap<Integer,Float>) aCache.getAsObject(Const.Cache_Chart_12);
                chart12Date = (ArrayList) aCache.getAsObject(Const.Cache_Chart_12_Date);
                chartInitial(current_chart1_index,current_chart2_index);
            } else if(intent.getAction().equals(Const.Action_Chart_Result_1)){
//                Log.e("Action_Chart_Cache","Action_Chart_Cache");
                HashMap data4 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart4_data);
                chartData4 = data4;
                aCache.put(Const.Cache_Chart_4,chartData4);
                HashMap data5 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart5_data);
                chartData5 = data5;
                aCache.put(Const.Cache_Chart_5,chartData5);
                HashMap data8 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart8_data);
                chartData8 = data8;
                aCache.put(Const.Cache_Chart_8,chartData8);
                chartInitial(current_chart1_index,current_chart2_index);
            }else if(intent.getAction().equals(Const.Action_Chart_Result_2)){
                HashMap data1 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart1_data);
                chartData1 = data1;
                aCache.put(Const.Cache_Chart_1,chartData1);
                HashMap data2 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart2_data);
                chartData2 = data2;
                aCache.put(Const.Cache_Chart_2,chartData2);
                HashMap data3 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart3_data);
                chartData3 = data3;
                aCache.put(Const.Cache_Chart_3,chartData3);
                HashMap data6 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart6_data);
                chartData6 = data6;
                aCache.put(Const.Cache_Chart_6,chartData6);
                HashMap data10 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart10_data);
                chartData10 = data10;
                aCache.put(Const.Cache_Chart_10,chartData10);
                chartInitial(current_chart1_index,current_chart2_index);
            }else if(intent.getAction().equals(Const.Action_Chart_Result_3)){
                HashMap data7 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart7_data);
                ArrayList date7 = (ArrayList)intent.getExtras().getSerializable(Const.Intent_chart_7_data_date);
                chartData7 = data7;
                aCache.put(Const.Cache_Chart_7,chartData7);
                aCache.put(Const.Cache_Chart_7_Date,date7);
                HashMap data12 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart12_data);
                ArrayList date12 = (ArrayList)intent.getExtras().getSerializable(Const.Intent_chart_12_data_date);
                chartData12 = data12;
                aCache.put(Const.Cache_Chart_12,chartData12);
                aCache.put(Const.Cache_Chart_12_Date,date12);
                chartInitial(current_chart1_index, current_chart2_index);
            }
        }
    }

    private Object setChartDataByIndex(int index) {
        switch (index) {
            case 1:
//                return DataGenerator.chart1DataGenerator(DataGenerator.generateDataForChart1());
                  return DataGenerator.chart1DataGenerator(chartData1);
            case 2:
//                return DataGenerator.chart2DataGenerator(DataGenerator.generateDataForChart2());
                  return DataGenerator.chart2DataGenerator(chartData2);
            case 3:
//                return DataGenerator.chart3DataGenerator((int) DataGenerator.generateDataForChart3().keySet().toArray()[0],
//                        (float) DataGenerator.generateDataForChart3().values().toArray()[0]);
                 if(chartData3.isEmpty())
                     return DataGenerator.chart3DataGenerator(0,0.0f);
                  return DataGenerator.chart3DataGenerator((int) chartData3.keySet().toArray()[0], (float) chartData3.values().toArray()[0]);
            case 4:
//                return DataGenerator.chart4DataGenerator(DataGenerator.generateDataForChart4());
                  return DataGenerator.chart4DataGenerator(chartData4);
            case 5:
//                return DataGenerator.chart5DataGenerator(DataGenerator.generateDataForChart5());
                return DataGenerator.chart5DataGenerator(chartData5);
            case 6:
//                return DataGenerator.chart6DataGenerator(DataGenerator.generateDataForChart6());
                return  DataGenerator.chart6DataGenerator(chartData6);
            case 7:
//                return DataGenerator.chart7DataGenerator(DataGenerator.generateDataForChart7(), DataGenerator.generateChart7Date());
                return DataGenerator.chart7DataGenerator(chartData7,chart7Date);
            case 8:
//                return DataGenerator.chart8DataGenerator(DataGenerator.generateDataForChart8());
                return DataGenerator.chart8DataGenerator(chartData8);
            case 10:
//                return DataGenerator.chart10DataGenerator((int) DataGenerator.generateDataForChart10().keySet().toArray()[0],
//                        (float) DataGenerator.generateDataForChart10().values().toArray()[0]);
                if(chartData10.isEmpty())
                    return DataGenerator.chart10DataGenerator(0,0.0f);
                return DataGenerator.chart10DataGenerator((int) chartData10.keySet().toArray()[0], (float)chartData10.values().toArray()[0]);
            case 12:
                 return DataGenerator.chart12DataGenerator(chartData12, chart12Date);
//                return DataGenerator.chart12DataGenerator(DataGenerator.generateDataForChart12(), DataGenerator.generateChart12Date());
        }
        return null;
    }

}
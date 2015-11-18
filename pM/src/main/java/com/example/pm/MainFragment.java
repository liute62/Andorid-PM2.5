package com.example.pm;


import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Typeface;
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

import java.util.HashMap;

import app.model.PMModel;
import app.services.DBService;
import app.utils.ACache;
import app.utils.ChartsConst;
import app.utils.Const;
import app.utils.DBHelper;
import app.utils.DataGenerator;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;
import app.view.widget.LoadingDialog;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.view.ColumnChartView;
import lecho.lib.hellocharts.view.LineChartView;

public class MainFragment extends Fragment implements OnClickListener {

    Activity mActivity;
    ImageView mProfile;
    ImageView mHotMap;
    ColumnChartView mChart1;
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
    int PMDensity = 0;
    Double PMBreatheHour = 0.0;
    Double PMBreatheDay = 0.0;
    Double PMBreatheWeekAvg = 0.0;
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
    private DBService myService;
    private IntentFilter intentFilter;
    /**
     * Charts*
     */
    int current_chart1_index;
    int current_chart2_index;
    //    ChartsPagerAdapter mChartsPagerAdapter1;
//    ChartsPagerAdapter mChartsPagerAdapter2;
//    ViewPager chartViewpager1;
//    ViewPager chartViewpager2;
    ColumnChartView mChart1column;
    LineChartView mChart1line;
    ColumnChartView mChart2column;
    LineChartView mChart2line;
    /**Charts data**/
    HashMap<Integer,Float> chartData1;

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
                PMDensity = Integer.valueOf(data.getPm25());
                Log.e("mDataHandler", String.valueOf(PMDensity));
                dataInitial();
            }
            if (msg.what == Const.Handler_PM_Data) {
                PMModel data = (PMModel) msg.obj;
                PMBreatheHour = Double.valueOf(data.getPm_breath_hour());
                PMBreatheDay = Double.valueOf(data.getPm_breath_today());
                PMBreatheWeekAvg = Double.valueOf(data.getPm_breath_week());
                dataInitial();
            }
        }
    };

    @Override
    public void onPause() {
        mActivity.unregisterReceiver(dbReceiver);
        super.onPause();
    }

    @Override
    public void onResume() {
        if (mActivity != null) {
            dbReceiver = new DBServiceReceiver();
            intentFilter = new IntentFilter();
            intentFilter.addAction(Const.Action_DB_MAIN_PMDensity);
            intentFilter.addAction(Const.Action_DB_MAIN_PMResult);
            intentFilter.addAction(Const.Action_Chart_Result);
            mActivity.registerReceiver(dbReceiver, intentFilter);
        }
        super.onResume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        chartData1 = new HashMap<>();
        current_chart1_index = 1;
        current_chart2_index = 2;
        isClockTaskRun = false;
        pmModel = new PMModel();
        mActivity = getActivity();
        loadingDialog = new LoadingDialog(getActivity());
        aCache = ACache.get(mActivity);
        //GPS Task
        if (!ShortcutUtil.isServiceWork(mActivity, "app.services.DBService")) {
            dbReceiver = new DBServiceReceiver();
            intentFilter = new IntentFilter();
            intentFilter.addAction(Const.Action_DB_MAIN_PMDensity);
            intentFilter.addAction(Const.Action_DB_MAIN_PMResult);
            intentFilter.addAction(Const.Action_Chart_Result);
            mActivity.registerReceiver(dbReceiver, intentFilter);
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
        chartInitial(current_chart1_index, current_chart2_index);
        cacheInitial();
        setListener();
        dataInitial();
        taskInitial();
        return view;
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
            mChart2column.setColumnChartData((ColumnChartData) setChartDataByIndex(chart2_index));
        } else if (ChartsConst.Chart_type[chart2_index] == 1) {
            mChart2column.setVisibility(View.INVISIBLE);
            mChart2line.setVisibility(View.VISIBLE);
            mChart2line.setLineChartData((LineChartData) setChartDataByIndex(chart2_index));
        } else {
            mChart2column.setVisibility(View.INVISIBLE);
            mChart2line.setVisibility(View.INVISIBLE);
        }
    }

//    private void chartInitial(View view){
//        LayoutInflater lf = mActivity.getLayoutInflater().from(mActivity);
//        List<View> view1s = new ArrayList<>();
//        List<Integer> type1 = new ArrayList<>();
//        type1.add(1);
//        type1.add(2);
//        List<View> view2s = new ArrayList<>();
//        List<Integer> type2 = new ArrayList<>();
//        type2.add(1);
//        type2.add(2);
//        /**For the first row charts**/
//        chartViewpager1 = (ViewPager) view.findViewById(R.id.main_chart_viewpager_1);
//        View chartView1 = lf.inflate(R.layout.view_top_chart_1, null);
//        View chartView2 = lf.inflate(R.layout.view_top_chart_2, null);
//        view1s.add(chartView1);
//        view1s.add(chartView2);
//        mChartsPagerAdapter1 = new ChartsPagerAdapter(view1s,type1);
//        chartViewpager1.setAdapter(mChartsPagerAdapter1);
//        /**For the second row charts**/
//        chartViewpager2 = (ViewPager) view.findViewById(R.id.main_chart_viewpager_2);
//        View chartView3 = lf.inflate(R.layout.view_bottom_chart_1, null);
//        View chartView4 = lf.inflate(R.layout.view_bottom_chart_2, null);
//        view2s.add(chartView3);
//        view2s.add(chartView4);
//        mChartsPagerAdapter2 = new ChartsPagerAdapter(view2s,type2);
//        chartViewpager2.setAdapter(mChartsPagerAdapter2);
//    }

    private void chart1Initial() {

    }

    private void cacheInitial() {
        JSONObject object = aCache.getAsJSONObject(Const.Cache_PM_State);
        DBHelper dbHelper = new DBHelper(mActivity.getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        if (object == null) {
            Toast.makeText(mActivity.getApplicationContext(), Const.ERROR_NO_PM_DATA, Toast.LENGTH_SHORT).show();
        } else {
            //Set current PM state by Cache.
            try {
                Const.CURRENT_PM_MODEL = PMModel.parse(object);
                Message data = new Message();
                data.what = Const.Handler_PM_Density;
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

    private void setFonts(View view) {
        Typeface typeFace = Typeface.createFromAsset(mActivity.getAssets(), "SourceHanSansCNLight.ttf");
        TextView textView1 = (TextView) view.findViewById(R.id.textView1);
        textView1.setTypeface(typeFace);
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
        mCity.setText("无");
        mAirQuality.setText(DataGenerator.setAirQualityText(PMDensity));
        mAirQuality.setTextColor(DataGenerator.setAirQualityColor(PMDensity));
        mHint.setText(DataGenerator.setHeathHintText(PMDensity));
        mHint.setTextColor(DataGenerator.setHeathHintColor(PMDensity));
        mHourPM.setText(String.valueOf(ShortcutUtil.ugScale(PMBreatheHour, 2)) + " 微克");
        mDayPM.setText(String.valueOf(ShortcutUtil.ugToMg(PMBreatheDay, 2)) + " 毫克");
        mWeekPM.setText(String.valueOf(ShortcutUtil.ugToMg(PMBreatheWeekAvg, 1)) + " 毫克");
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
            Log.e("OnReceive",intent.getAction().toString());
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
            } else if(intent.getAction().equals(Const.Action_Chart_Result)){
                Log.e("Action_Chart_Result","Action_Chart_Result");
                HashMap data1 = (HashMap)intent.getExtras().getSerializable(Const.Intent_chart1_data);
                chartData1 = data1;
                chartInitial(current_chart1_index,current_chart2_index);
            }
        }
    }

    private Object setChartDataByIndex(int index) {
        switch (index) {
            case 1:
//                return DataGenerator.chart1DataGenerator(DataGenerator.generateDataForChart1());
                  return DataGenerator.chart1DataGenerator(chartData1);
            case 2:
                return DataGenerator.chart2DataGenerator(DataGenerator.generateDataForChart2());
            case 3:
                return DataGenerator.chart3DataGenerator((int) DataGenerator.generateDataForChart3().keySet().toArray()[0],
                        (float) DataGenerator.generateDataForChart3().values().toArray()[0]);
            case 4:
                return DataGenerator.chart4DataGenerator(DataGenerator.generateDataForChart4());
            case 5:
                return DataGenerator.chart5DataGenerator(DataGenerator.generateDataForChart5());
            case 6:
                return DataGenerator.chart6DataGenerator(DataGenerator.generateDataForChart6());
            case 7:
                return DataGenerator.chart7DataGenerator(DataGenerator.generateDataForChart7(), DataGenerator.generateChart7Date());
            case 8:
                return DataGenerator.chart8DataGenerator(DataGenerator.generateDataForChart8());
            case 10:
                return DataGenerator.chart10DataGenerator((int) DataGenerator.generateDataForChart10().keySet().toArray()[0],
                        (float) DataGenerator.generateDataForChart10().values().toArray()[0]);
            case 12:
                return DataGenerator.chart12DataGenerator(DataGenerator.generateDataForChart12(), DataGenerator.generateChart12Date());
        }
        return null;
    }

}
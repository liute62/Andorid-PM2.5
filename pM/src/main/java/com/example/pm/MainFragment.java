package com.example.pm;


import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
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

import app.model.HttpResult;
import app.utils.Const;
import app.utils.DataFaker;
import app.utils.HttpUtil;
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
    DataTask dataTask;
    boolean isDataTaskRun = false;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        isClockTaskRun = false;
        PMDensity = (int) (Math.random() * 300);
        Log.e("PM2.5:", String.valueOf(PMDensity));
        mActivity = getActivity();
        if (isDataTaskRun == false){
            dataTask = new DataTask();
            dataTask.execute(1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
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
        setListener();
        dataInitial();
        taskInitial();
        return view;
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

    private class DataTask extends AsyncTask<Integer,Integer,Integer>{

        LoadingDialog mDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isDataTaskRun = true;
            mDialog = new LoadingDialog(getActivity());
            mDialog.show();
        }

        @Override
        protected Integer doInBackground(Integer... integers) {
            HttpResult result = HttpUtil.SearchPMRequest(getActivity(), "131", "31");
            if(result.getResultBody() != null){
                return 1;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            isDataTaskRun = false;
            if(integer == 1) {
                mDialog.dismiss();
            }
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
}
package com.example.pm;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Map;

import app.utils.DataGenerator;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.view.ColumnChartView;

/**
 * Created by liuhaodong1 on 15/11/13.
 */
public class ChartsPagerAdapter extends PagerAdapter {


    private int type; //0: column chart, 1: line chart
    private List<Integer> typeList;
    private List<View> viewList;
    private ColumnChartData columnChartData;
    private LineChartData lineChartData;

    public ChartsPagerAdapter(List<View> mViews,List<Integer> mTypes){
        this.viewList = mViews;
        this.typeList = mTypes;
    }


    @Override
    public boolean isViewFromObject(View arg0, Object arg1) {

        return arg0 == arg1;
    }

    @Override
    public int getCount() {

        return viewList.size();
    }

    @Override
    public void destroyItem(ViewGroup container, int position,
                            Object object) {
        container.removeView((View)(viewList.get(position)));

    }

    @Override
    public int getItemPosition(Object object) {

        return super.getItemPosition(object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
         int type = typeList.get(position);
         ColumnChartView view = (ColumnChartView)viewList.get(position);
         container.addView(view);
         view.setColumnChartData(null);
         return viewList.get(position);
    }

 }

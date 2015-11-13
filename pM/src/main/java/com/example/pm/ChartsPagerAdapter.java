package com.example.pm;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import app.utils.DataGenerator;
import lecho.lib.hellocharts.view.ColumnChartView;

/**
 * Created by liuhaodong1 on 15/11/13.
 */
public class ChartsPagerAdapter extends PagerAdapter {


    private List<View> viewList;

    public ChartsPagerAdapter(List<View> mViews){
        this.viewList = mViews;
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
        container.removeView(viewList.get(position));

    }

    @Override
    public int getItemPosition(Object object) {

        return super.getItemPosition(object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        ColumnChartView view = (ColumnChartView)viewList.get(position);
        container.addView(view);
        view.setColumnChartData(DataGenerator.setColumnDataForChart1());
        return viewList.get(position);
    }

 }

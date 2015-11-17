package com.example.pm;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import app.utils.DataGenerator;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by liuhaodong1 on 15/11/13.
 */
public class ChartFragment extends Fragment {

    int pos;

    public ChartFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_top_chart_1, container, false);
        RelativeLayout layout = (RelativeLayout) rootView;
        LineChartView lineChartView = new LineChartView(getActivity());
        lineChartView.setLineChartData(DataGenerator.setDataForChart1());
        lineChartView.setZoomType(ZoomType.HORIZONTAL);
        /** Note: Chart is within ViewPager so enable container scroll mode. **/
        lineChartView.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        layout.addView(lineChartView);

        return rootView;
    }
}

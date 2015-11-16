package app.utils;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.ValueShape;
import lecho.lib.hellocharts.util.ChartUtils;

/**
 * Created by liuhaodong1 on 15/11/8.
 */
public class DataGenerator {

    private static DataGenerator instance = null;

    public static DataGenerator Instance() {
        if (instance == null) {
            instance = new DataGenerator();
        }
        return instance;
    }

    static boolean hasAxes = true;
    static boolean hasAxesNames = false;
    static boolean hasLines = true;
    static boolean hasPoints = true;
    static boolean isFilled = false;
    static boolean hasLabels = false;
    static boolean isCubic = false;
    static boolean hasLabelForSelected = false;
    static boolean pointsHaveDifferentColor = false;

    public static String setAirQualityText(int pm) {
        if (pm < 50) {
            return Const.airQuality[0];
        } else if (pm > 50 && pm < 100) {
            return Const.airQuality[1];
        } else if (pm > 100 && pm < 150) {
            return Const.airQuality[2];
        } else if (pm > 150 && pm < 200) {
            return Const.airQuality[3];
        } else if (pm > 200 && pm < 300) {
            return Const.airQuality[4];
        }
        return Const.airQuality[5];
    }

    public static int setAirQualityColor(int pm) {
        if (pm < 50) {
            return Color.GREEN;
        } else if (pm > 50 && pm < 100) {
            return Color.YELLOW;
        } else if (pm > 100 && pm < 150) {
            return Color.argb(255, 255, 165, 0); //Orange #FFA500
        } else if (pm > 150 && pm < 200) {
            return Color.RED;
        } else if (pm > 200 && pm < 300) {
            return Color.argb(255, 139, 35, 35); //Brown
        }
        return Color.BLACK;
    }

    public static String setHeathHintText(int pm) {
        if (pm < 50) {
            return Const.heathHint[0];
        } else if (pm > 50 && pm < 100) {
            return Const.heathHint[1];
        } else if (pm > 100 && pm < 150) {
            return Const.heathHint[2];
        }
        return Const.heathHint[3];
    }

    public static int setHeathHintColor(int pm) {
        if (pm < 50) {
            return Color.GREEN;
        } else if (pm > 50 && pm < 100) {
            return Color.YELLOW;
        } else if (pm > 100 && pm < 150) {
            return Color.argb(255, 255, 165, 0); //Orange
        }
        return Color.argb(255, 139, 35, 35); //Brown
    }

    public static String setRingState1Text() {
        return Const.ringState[0];
    }

    public static int setRingState1Color() {
        return Color.GRAY;
    }

    public static String setRingState2Text() {
        return Const.ringState2[0];
    }

    public static int setRingState2Color() {
        return Color.GRAY;
    }

    public static Map<Integer,Float> generateDataForChart1(){
        Map<Integer,Float> map = new HashMap<>();
        map.put(1,78.5f);
        map.put(10,23.5f);
        return map;
    }

    public static Map<Integer,Float> generateDataForChart2(){
        Map<Integer,Float> map = new HashMap<>();
        map.put(1,78.5f);
        map.put(9,43.5f);
        map.put(13,63.5f);
        map.put(19,13.5f);
        return map;
    }

    public static Map<Integer,Float> generateDataForChart3(){
        Map<Integer,Float> map = new HashMap<>();
        map.put(20,178.5f);
        return map;
    }

    public static Map<Integer,Float> generateDataForChart4(){
        Map<Integer,Float> map = new HashMap<>();
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][1]),78.5f);
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][3]),78.5f);
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][5]),78.5f);
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][7]),79.5f);
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][10]),88.5f);
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][12]),88.5f);
        map.put(Integer.valueOf(ChartsConst.Chart_X[4][19]),78.5f);
        return map;
    }

    public static LineChartData setDataForChart1() {
        int numberOfLines = 1;
        int numberOfPoints = 12;
        int maxNumberOfLines = 4;
        float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];
        ValueShape shape = ValueShape.CIRCLE;
        LineChartData data;

        //data generation
        for (int i = 0; i < maxNumberOfLines; ++i) {
            for (int j = 0; j < numberOfPoints; ++j) {
                randomNumbersTab[i][j] = (float) Math.random() * 100f;
            }
        }

        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < numberOfLines; ++i) {
            List<PointValue> values = new ArrayList<PointValue>();
            for (int j = 0; j < numberOfPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[i][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLORS[i]);
            line.setShape(shape);
            line.setCubic(isCubic);
            line.setFilled(isFilled);
            line.setHasLabels(hasLabels);
            line.setHasLabelsOnlyForSelected(hasLabelForSelected);
            line.setHasLines(hasLines);
            line.setHasPoints(hasPoints);
            if (pointsHaveDifferentColor) {
                line.setPointColor(ChartUtils.COLORS[(i + 1) % ChartUtils.COLORS.length]);
            }
            lines.add(line);
        }

        data = new LineChartData(lines);

        if (hasAxes) {
            Axis axisX = new Axis();
            Axis axisY = new Axis().setHasLines(true);
            if (hasAxesNames) {
                axisX.setName("Axis X");
                axisY.setName("Axis Y");
            }
            data.setAxisXBottom(axisX);
            data.setAxisYLeft(axisY);
        } else {
            data.setAxisXBottom(null);
            data.setAxisYLeft(null);
        }

        data.setBaseValue(Float.NEGATIVE_INFINITY);
        return data;
    }
}

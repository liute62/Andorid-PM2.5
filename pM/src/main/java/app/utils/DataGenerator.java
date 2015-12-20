package app.utils;

import android.graphics.Color;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lecho.lib.hellocharts.formatter.SimpleAxisValueFormatter;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
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

    public static String setAirQualityText(double pm) {
        return setAirQualityText((int) pm);
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

    public static int setAirQualityColor(double pm) {
        return setAirQualityColor((int) pm);
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

    public static String setHeathHintText(double pm) {
        return setHeathHintText((int) pm);
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

    public static int setHeathHintColor(double pm) {
        return setHeathHintColor((int) pm);
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

    //ex. 10:30am = 10*2+1 = 21
    public static Map<Integer, Float> generateDataForChart1() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(20, 3.5f);
        map.put(21, 4.5f);
        map.put(22, 5.5f);
        map.put(23, 4.3f);
        map.put(24, 3.8f);
        map.put(25, 7.9f);
        map.put(26, 4.3f);
        map.put(27, 2.4f);
        map.put(28, 8.5f);
        map.put(29, 6.5f);
        map.put(30, 3.4f);
        map.put(31, 3.9f);
        map.put(32, 6.7f);
        map.put(33, 7.8f);
        map.put(34, 4.3f);
        map.put(35, 7.2f);
        map.put(36, 8.1f);
        map.put(37, 1.9f);
        return map;
    }

    //ex. 10:30am = 10*2+1 = 21
    public static Map<Integer, Float> generateDataForChart2() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(20, 73.5f);
        map.put(21, 74.5f);
        map.put(22, 75.5f);
        map.put(23, 74.3f);
        map.put(24, 63.8f);
        map.put(25, 67.9f);
        map.put(26, 64.3f);
        map.put(27, 72.4f);
        map.put(28, 88.5f);
        map.put(29, 86.5f);
        map.put(30, 73.4f);
        map.put(31, 53.9f);
        map.put(32, 76.7f);
        map.put(33, 77.8f);
        map.put(34, 64.3f);
        map.put(35, 77.2f);
        map.put(36, 88.1f);
        map.put(37, 91.9f);
        return map;
    }

    //Ex. 20 means 20:00pm
    public static Map<Integer, Float> generateDataForChart3() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(0, 3.5f);
        map.put(21, 14.5f);
        map.put(22, 25.5f);
        map.put(23, 34.3f);
        map.put(24, 43.8f);
        map.put(25, 57.9f);
        map.put(26, 64.3f);
        map.put(27, 72.4f);
        map.put(28, 88.5f);
        map.put(29, 96.5f);
        map.put(30, 103.4f);
        map.put(31, 113.9f);
        map.put(32, 126.7f);
        map.put(33, 137.8f);
        map.put(34, 144.3f);
        map.put(35, 157.2f);
        map.put(36, 188.1f);
        return map;
    }

    //EX. 1 means 5min; 3 means 15min; 24 means 120min
    public static Map<Integer, Float> generateDataForChart4() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(1, 78.5f);
        map.put(2, 78.5f);
        map.put(3, 78.5f);
        map.put(4, 79.5f);
        map.put(5, 88.5f);
        map.put(8, 88.5f);
        map.put(10, 78.5f);
        map.put(23, 58.5f);
        map.put(24, 48.5f);
        return map;
    }

    //EX. 1 means 5min; 3 means 15min; 24 means 120min
    public static Map<Integer, Float> generateDataForChart5() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(1, 0.15f);
        map.put(2, 0.25f);
        map.put(3, 0.24f);
        map.put(5, 0.24f);
        map.put(7, 0.04f);
        map.put(8, 0.24f);
        map.put(12, 0.25f);
        map.put(14, 0.22f);
        map.put(15, 0.23f);
        map.put(19, 0.35f);
        return map;
    }

    //ex. 10:30am = 10*2+1 = 21
    public static Map<Integer, Float> generateDataForChart6() {
        Map<Integer, Float> map = new HashMap<>();
        for (int i = 0; i != 48; i++) {
            map.put(i, (float) Math.random() * 100);
        }
        return map;
    }

    //ex. 0 means day1
    public static Map<Integer, Float> generateDataForChart7() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(0, 428.5f);
        map.put(1, 143.5f);
        map.put(2, 293.5f);
        map.put(3, 353.5f);
        map.put(4, 193.5f);
        map.put(5, 353.5f);
        map.put(6, 553.5f);
        return map;
    }

    public static List<String> generateChart7Date() {
        List data = new ArrayList();
        data.add("11.11");
        data.add("11.12");
        data.add("11.13");
        data.add("11.14");
        data.add("11.15");
        data.add("11.16");
        data.add("11.17");
        return data;
    }

    //EX. 1 means 5min; 3 means 15min; 24 means 120min
    public static Map<Integer, Float> generateDataForChart8() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(1, 6.1f);
        map.put(2, 6.4f);
        map.put(3, 6.4f);
        map.put(5, 4.4f);
        map.put(7, 5.4f);
        map.put(8, 6.8f);
        map.put(12, 5.5f);
        map.put(14, 6.7f);
        map.put(15, 8.2f);
        map.put(19, 8.3f);
        map.put(21, 7.3f);
        map.put(22, 6.8f);
        map.put(24, 5.3f);
        return map;
    }

    public static Map<Integer, Float> generateDataForChart10() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(24, 278.5f);
        return map;
    }

    public static Map<Integer, Float> generateDataForChart12() {
        Map<Integer, Float> map = new HashMap<>();
        map.put(1, 28.5f);
        map.put(2, 43.5f);
        map.put(3, 93.5f);
        map.put(4, 53.5f);
        return map;
    }

    public static List<String> generateChart12Date() {
        List data = new ArrayList();
        data.add("11.11");
        data.add("11.12");
        data.add("11.13");
        data.add("11.14");
        data.add("11.15");
        data.add("11.16");
        data.add("11.17");
        return data;
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

    public static ColumnChartData chart1DataGenerator(Map<Integer, Float> maps) {
        ColumnChartData data;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numColumns = ChartsConst.Chart_X[1].length;
        for (int i = 0; i != numColumns; i++) {
            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[1][i]));
        }
        // Column can have many subcolumns, here by default I use 1 subcolumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = 0; i < numColumns; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue value;
            if (maps.containsKey(i)) {
                float input = maps.get(i).floatValue();
                value = new SubcolumnValue(input, ChartUtils.COLOR_BLUE);
            } else {
                value = new SubcolumnValue(0.0f);
            }

            values.add(value);
            Column column = new Column(values);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        data.setAxisYLeft(axisY);
        return data;
    }

    public static LineChartData chart2DataGenerator(Map<Integer, Float> map) {
       int maxIndex = ShortcutUtil.getMaxIndexFromMap(map);
//            int minIndex = ShortcutUtil.getMinIndexFromMap(map);
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numberOfLines = 1;
       // int numberOfPoints = ChartsConst.Chart_X[2].length;
            int numberOfPoints = maxIndex+1;
        for (int i = 0; i != numberOfPoints; i++) {
            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[2][i]));
        }
        int maxNumberOfLines = 1;
        float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];
        ValueShape shape = ValueShape.CIRCLE;
        LineChartData data;
        //data generation
        for (int j = 0; j < numberOfPoints; ++j) {
            if (map.containsKey(j)) {
                randomNumbersTab[0][j] = map.get(j).floatValue();
            } else {
                randomNumbersTab[0][j] = 0;
            }
        }
        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < numberOfLines; ++i) {
            List<PointValue> values = new ArrayList<PointValue>();
            for (int j = 0; j < numberOfPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[i][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLOR_BLUE);
            line.setShape(shape);
            line.setFilled(false);
            line.setHasLines(true);
            line.setHasPoints(false);
            line.setCubic(true);
            line.setHasLabelsOnlyForSelected(true);
            line.setStrokeWidth(2);
            lines.add(line);
        }

        data = new LineChartData(lines);
        Axis axisX = new Axis(axisValues);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setBaseValue(Float.NEGATIVE_INFINITY);
        return data;
    }

//    public static LineChartData chart2DataGenerator(Map<Integer, Float> map) {
//        List<AxisValue> axisValues = new ArrayList<AxisValue>();
//        int numberOfLines = 1;
//        int numberOfPoints = ChartsConst.Chart_X[2].length;
//        for (int i = 0; i != numberOfPoints; i++) {
//            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[2][i]));
//        }
//        int maxNumberOfLines = 1;
//        float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];
//        ValueShape shape = ValueShape.CIRCLE;
//        LineChartData data;
//        //data generation
//        for (int j = 0; j < numberOfPoints; ++j) {
//            if (map.containsKey(j)) {
//                randomNumbersTab[0][j] = map.get(j).floatValue();
//            } else {
//                randomNumbersTab[0][j] = 0;
//            }
//        }
//        List<Line> lines = new ArrayList<Line>();
//        for (int i = 0; i < numberOfLines; ++i) {
//            List<PointValue> values = new ArrayList<PointValue>();
//            for (int j = 0; j < numberOfPoints; ++j) {
//                values.add(new PointValue(j, randomNumbersTab[i][j]));
//            }
//
//            Line line = new Line(values);
//            line.setColor(ChartUtils.COLOR_BLUE);
//            line.setShape(shape);
//            line.setFilled(false);
//            line.setHasLines(true);
//            line.setHasPoints(false);
//            line.setHasLabelsOnlyForSelected(true);
//            line.setStrokeWidth(2);
//            lines.add(line);
//        }
//
//        data = new LineChartData(lines);
//        Axis axisX = new Axis(axisValues);
//        Axis axisY = new Axis().setHasLines(true);
//        data.setAxisXBottom(axisX);
//        data.setAxisYLeft(axisY);
//        data.setBaseValue(Float.NEGATIVE_INFINITY);
//        return data;
//    }

    public static LineChartData chart3DataGenerator(Map<Integer, Float> maps) {
        ArrayList<Integer> tmpKey = new ArrayList<>();
        LineChartData data;
        int num = 0;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numPoints = maps.size();
        for(Integer key: maps.keySet()){
            tmpKey.add(key);
            axisValues.add(null);
        }
        Collections.sort(tmpKey);
        for(Integer key: maps.keySet()) {
            int index = Arrays.binarySearch(tmpKey.toArray(),key);
            axisValues.set(index, new AxisValue(index).setLabel(ChartsConst.Chart_X[1][key]));
        }
        int maxNumberOfLines = 1;
        float[][] randomNumbersTab = new float[maxNumberOfLines][numPoints];
        //data generation
        for (Integer key: maps.keySet()) {
            int index = Arrays.binarySearch(tmpKey.toArray(),key);
            randomNumbersTab[0][index] = maps.get(key).floatValue();
        }

        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < maxNumberOfLines; ++i) {
            List<PointValue> values = new ArrayList<PointValue>();
            for (int j = 0; j < numPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[i][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLOR_BLUE);
            line.setFilled(true);
            line.setHasLines(true);
            line.setHasPoints(false);
            line.setHasLabelsOnlyForSelected(true);
            line.setStrokeWidth(2);
            lines.add(line);
        }

        data = new LineChartData(lines);
        Axis axisX = new Axis(axisValues);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setBaseValue(Float.NEGATIVE_INFINITY);
        return data;
    }

//    public static LineChartData chart3DataGenerator(int time, float result) {
//        Line line;
//        List<PointValue> values;
//        List<Line> lines = new ArrayList<Line>();
//        // Height line, add it as first line to be drawn in the background.
//        values = new ArrayList<PointValue>();
//        values.add(new PointValue(0, 0));
//        values.add(new PointValue(Float.valueOf(ChartsConst.Chart_X[1][time]), result));
//        line = new Line(values);
//        line.setColor(Color.BLUE);
//        line.setHasPoints(false);
//        line.setFilled(true);
//        line.setStrokeWidth(1);
//        lines.add(line);
//        // Data and axes
//        LineChartData data = new LineChartData(lines);
//        // Distance axis(bottom X) with formatter that will ad [km] to values, remember to modify max label charts
//        // value.
//        Axis distanceAxis = new Axis();
//        distanceAxis.setName("时间");
//        distanceAxis.setTextColor(ChartUtils.COLOR_ORANGE);
//        distanceAxis.setMaxLabelChars(4);
//        distanceAxis.setFormatter(new SimpleAxisValueFormatter().setAppendedText("点".toCharArray()));
//        distanceAxis.setHasLines(true);
//        distanceAxis.setInside(true);
//        data.setAxisXBottom(distanceAxis);
//        // Speed axis
//        data.setAxisYLeft(new Axis().setName("毫克").setHasLines(true).setMaxLabelChars(3)
//                .setTextColor(ChartUtils.COLOR_BLUE).setInside(true));
//
//        return data;
//    }

    public static LineChartData chart4DataGenerator(Map<Integer, Float> map) {
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numberOfLines = 1;
        int numberOfPoints = ChartsConst.Chart_X[4].length;
        for (int i = 0; i != numberOfPoints; i++) {
            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[4][i]));
        }
        int maxNumberOfLines = 1;
        float[][] randomNumbersTab = new float[maxNumberOfLines][numberOfPoints];
        ValueShape shape = ValueShape.CIRCLE;
        LineChartData data;
        //data generation
        for (int j = 0; j < numberOfPoints; ++j) {
            if (map.containsKey(j)) {
                randomNumbersTab[0][j] = map.get(j).floatValue();
            } else {
                randomNumbersTab[0][j] = 0;
            }
        }
        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < numberOfLines; ++i) {
            List<PointValue> values = new ArrayList<PointValue>();
            for (int j = 0; j < numberOfPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[i][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLOR_BLUE);
            line.setShape(shape);
            line.setFilled(false);
            line.setHasLines(true);
            line.setHasPoints(false);
            lines.add(line);
        }

        data = new LineChartData(lines);
        Axis axisX = new Axis(axisValues);
        axisX.setFormatter(new SimpleAxisValueFormatter().setAppendedText("分钟".toCharArray()));
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setBaseValue(Float.NEGATIVE_INFINITY);
        return data;
    }

    public static ColumnChartData chart5DataGenerator(Map<Integer, Float> maps) {
        ColumnChartData data;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numColumns = ChartsConst.Chart_X[5].length;
        for (int i = 0; i != numColumns; i++) {
            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[5][i]));
        }
        // Column can have many subcolumns, here by default I use 1 subcolumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = 0; i < numColumns; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue value;
            if (maps.containsKey(i)) {
                float input = maps.get(i).floatValue();
                value = new SubcolumnValue(input, ChartUtils.COLOR_BLUE);
            } else {
                value = new SubcolumnValue(0.0f);
            }

            values.add(value);
            Column column = new Column(values);
            column.setHasLabels(false);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        data.setAxisYLeft(axisY);
        return data;
    }

    public static ColumnChartData chart6DataGenerator(Map<Integer, Float> maps) {
        ColumnChartData data;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numColumns = ChartsConst.Chart_X[6].length;
        for (int i = 0; i != numColumns; i++) {
            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[6][i]));
        }
        // Column can have many subcolumns, here by default I use 1 subcolumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = 0; i < numColumns; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue value;
            if (maps.containsKey(i)) {
                float input = maps.get(i).floatValue();
                value = new SubcolumnValue(input, ChartUtils.COLOR_BLUE);
            } else {
                value = new SubcolumnValue(0.0f);
            }

            values.add(value);
            Column column = new Column(values);
            column.setHasLabels(false);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        data.setAxisYLeft(axisY);
        return data;
    }

    public static ColumnChartData chart7DataGenerator(Map<Integer, Float> maps, List<String> date) {
        ColumnChartData data;
        int numSubcolumns = 1;
        int numColumns = ChartsConst.Chart_X[7].length;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        if (!date.isEmpty()) {
            for (int i = numColumns - 1; i >= 0; i--) {
                if (date.get(i) == null) break;
                axisValues.add(numColumns - i - 1, new AxisValue(i).setLabel(date.get(numColumns - i - 1)));
            }
        }
        // Column can have many subcolumns, here by default I use 1 subcolumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = numColumns - 1; i >= 0; i--) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue value;
            if (maps.containsKey(i)) {
                float input = maps.get(i).floatValue();
                value = new SubcolumnValue(input, ChartUtils.COLOR_BLUE);
            } else {
                value = new SubcolumnValue(0.0f);
            }

            values.add(value);
            Column column = new Column(values);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);
        Axis axisX = new Axis(axisValues).setHasLines(true);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        return data;
    }

    public static ColumnChartData chart8DataGenerator(Map<Integer, Float> maps) {
        ColumnChartData data;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numColumns = ChartsConst.Chart_X[8].length;
        for (int i = 0; i != numColumns; i++) {
            axisValues.add(i, new AxisValue(i).setLabel(ChartsConst.Chart_X[8][i]));
        }
        // Column can have many subcolumns, here by default I use 1 subcolumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = 0; i < numColumns; ++i) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue value;
            if (maps.containsKey(i)) {
                float input = maps.get(i).floatValue();
                value = new SubcolumnValue(input, ChartUtils.COLOR_BLUE);
            } else {
                value = new SubcolumnValue(0.0f);
            }

            values.add(value);
            Column column = new Column(values);
            column.setHasLabels(false);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);
        Axis axisY = new Axis().setHasLines(true).setMaxLabelChars(4);
        data.setAxisXBottom(new Axis(axisValues).setHasLines(true));
        data.setAxisYLeft(axisY);
        return data;
    }

    public static LineChartData chart10DataGenerator(Map<Integer, Float> maps) {
        ArrayList<Integer> tmpKey = new ArrayList<>();
        LineChartData data;
        int num = 0;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();
        int numPoints = maps.size();
        for(Integer key: maps.keySet()){
            tmpKey.add(key);
            axisValues.add(null);
        }
        Collections.sort(tmpKey);
        for(Integer key: maps.keySet()) {
            int index = Arrays.binarySearch(tmpKey.toArray(),key);
            axisValues.set(index, new AxisValue(index).setLabel(ChartsConst.Chart_X[1][key]));
        }
        int maxNumberOfLines = 1;
        float[][] randomNumbersTab = new float[maxNumberOfLines][numPoints];
        //data generation
        for (Integer key: maps.keySet()) {
            int index = Arrays.binarySearch(tmpKey.toArray(),key);
            randomNumbersTab[0][index] = maps.get(key).floatValue();
        }

        List<Line> lines = new ArrayList<Line>();
        for (int i = 0; i < maxNumberOfLines; ++i) {
            List<PointValue> values = new ArrayList<PointValue>();
            for (int j = 0; j < numPoints; ++j) {
                values.add(new PointValue(j, randomNumbersTab[i][j]));
            }

            Line line = new Line(values);
            line.setColor(ChartUtils.COLOR_BLUE);
            line.setFilled(true);
            line.setHasLines(true);
            line.setHasPoints(false);
            line.setHasLabelsOnlyForSelected(true);
            line.setStrokeWidth(2);
            lines.add(line);
        }

        data = new LineChartData(lines);
        Axis axisX = new Axis(axisValues);
        Axis axisY = new Axis().setHasLines(true);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        data.setBaseValue(Float.NEGATIVE_INFINITY);
        return data;
    }

//    public static LineChartData chart10DataGenerator(int time, float result) {
//        Line line;
//        List<PointValue> values;
//        List<Line> lines = new ArrayList<Line>();
//
//        // Height line, add it as first line to be drawn in the background.
//        values = new ArrayList<PointValue>();
//        values.add(new PointValue(0, 0));
//        values.add(new PointValue(Float.valueOf(ChartsConst.Chart_X[1][time]), result));
//        line = new Line(values);
//        line.setColor(Color.BLUE);
//        line.setHasPoints(false);
//        line.setFilled(true);
//        line.setStrokeWidth(1);
//        lines.add(line);
//        // Data and axes
//        LineChartData data = new LineChartData(lines);
//        // Distance axis(bottom X) with formatter that will ad [km] to values, remember to modify max label charts
//        // value.
//        Axis distanceAxis = new Axis();
//        distanceAxis.setName("时间");
//        distanceAxis.setTextColor(ChartUtils.COLOR_ORANGE);
//        distanceAxis.setMaxLabelChars(4);
//        distanceAxis.setFormatter(new SimpleAxisValueFormatter().setAppendedText("点".toCharArray()));
//        distanceAxis.setHasLines(true);
//        distanceAxis.setInside(true);
//        data.setAxisXBottom(distanceAxis);
//        // Speed axis
//        data.setAxisYLeft(new Axis().setName("升").setHasLines(true).setMaxLabelChars(3)
//                .setTextColor(ChartUtils.COLOR_BLUE).setInside(true));
//
//        return data;
//    }

    public static ColumnChartData chart12DataGenerator(Map<Integer, Float> maps, List<String> date) {
        ColumnChartData data;
        int numSubcolumns = 1;
        int numColumns = ChartsConst.Chart_X[12].length;
        List<AxisValue> axisValues = new ArrayList<AxisValue>();

        if (!date.isEmpty()) {
            for (int i = numColumns - 1; i >= 0; i--) {
                if (date.get(i) == null) break;
                axisValues.add(numColumns - i - 1, new AxisValue(i).setLabel(date.get(numColumns - i - 1)));
            }
        }

        // Column can have many subcolumns, here by default I use 1 subcolumn in each of 8 columns.
        List<Column> columns = new ArrayList<Column>();
        List<SubcolumnValue> values;
        for (int i = numColumns - 1; i >= 0; i--) {
            values = new ArrayList<SubcolumnValue>();
            SubcolumnValue value;
            if (maps.containsKey(i)) {
                float input = maps.get(i).floatValue();
                //Log.e("chart12DataGenerator",String.valueOf(input));
                value = new SubcolumnValue(input, ChartUtils.COLOR_BLUE);
            } else {
                value = new SubcolumnValue(0.0f);
            }
            values.add(value);
            Column column = new Column(values);
            column.setHasLabelsOnlyForSelected(true);
            columns.add(column);
        }

        data = new ColumnChartData(columns);
        Axis axisX = new Axis(axisValues).setHasLines(true);
        Axis axisY = new Axis().setHasLines(true).setMaxLabelChars(4);
        data.setAxisXBottom(axisX);
        data.setAxisYLeft(axisY);
        return data;
    }

    public static double genDensityForTest(int currentHour) {
        //Log.e("currentHour", String.valueOf(currentHour));
        double pm1[] = new double[]{3, 0, 0, 0, 0, 0, 0, 0, 4, 7, 4, 3, 0, 492, 584, 619, 619, 618
                , 528, 552, 542, 434, 410, 223};
        return pm1[currentHour];
    }

}

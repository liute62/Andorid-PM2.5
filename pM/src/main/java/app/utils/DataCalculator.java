package app.utils;

import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;
import android.util.Log;

import com.example.pm.MainActivity;

import java.security.Key;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import app.Entity.State;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by Haodong Liu on 11/20/2015.
 */
public class DataCalculator {

    public static final String TAG = "DataCalculator";
    SQLiteDatabase db;
    private List<State> todayStates;
    private List<State> lastTwoHourStates;
    private List<List<State>> lastWeekStates;
    private ArrayList<String> lastWeekDate;
    private ArrayList<String> lastTwoHourTime;

    private static DataCalculator instance = null;

    public static DataCalculator getIntance(SQLiteDatabase db) {
        if (instance == null) {
            instance = new DataCalculator(db);
        }
        return instance;
    }

    private DataCalculator(SQLiteDatabase db) {
        lastTwoHourTime = new ArrayList<>();
        this.db = db;
        this.todayStates = calTodayStates();
        this.lastTwoHourStates = calLastTwoHourStates(); //Actually the time is set here before function be invoked. But it's ok.
        this.lastWeekStates = calLastWeekStates(); //only manually invoke this function
    }

    public void updateLastDayState() {
        this.todayStates = calTodayStates();
    }

    public void updateLastTwoHourState() {
        this.lastTwoHourStates = calLastTwoHourStates();
    }

    public void updateLastWeekState() {
        this.lastWeekStates = calLastWeekStates();
    }

    private List<State> calTodayStates() {
        if (db == null) return new ArrayList<State>();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);

        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        return states;
    }

    private List<State> calLastTwoHourStates() {
        if (db == null) return new ArrayList<State>();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        Time t = new Time();
        t.setToNow();
        int currentHour = t.hour;
        int currentMin = t.minute;
        int lastTwoHour = currentHour - 2;
        if (lastTwoHour < 0) {
            calendar.set(year, month, day, 0, 0, 0);
        } else {
            calendar.set(year, month, day, lastTwoHour, currentMin, 0);
        }
        Long lastTime = calendar.getTime().getTime();
        calendar.set(year, month, day, currentHour, currentMin, 59);
        Long nowTime = calendar.getTime().getTime();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", lastTime.toString(), nowTime.toString()).list();
        lastTwoHourTime = new ArrayList<>();
        for(int i = 0; i != states.size(); i++){
            lastTwoHourTime.add(ShortcutUtil.refFormatNowDate(Long.valueOf(states.get(i).getTime_point())));
        }
        return states;
    }

    private List<List<State>> calLastWeekStates() {
        List<List<State>> mData = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        int yearOrigin = calendar.get(Calendar.YEAR);
        int monthOrigin = calendar.get(Calendar.MONTH);
        int dayOrigin = calendar.get(Calendar.DAY_OF_MONTH);
        lastWeekDate = new ArrayList<>();
        //Log.d(TAG,"begin");
//        Log.e("yearOrigin",String.valueOf(yearOrigin));
        for (int i = 0; i != 7; i++) {
            int day = dayOrigin;
            int month = monthOrigin;
            int year = yearOrigin;
            if (day - i < 1) {
                month = month - 1;
                if (month < 1) {
                    year = year - 1;
                    month = 12;
                }
                day = Const.DayMaxOfTheMonth[month] + day - i;
                if(month == 2 && (year % 4 == 0)) day = 29 + day - i;
            } else {
                day = day - i;
            }
            lastWeekDate.add(i, String.valueOf((month + 1)>12?12:(month+1)) + "." + String.valueOf(day));
            calendar.set(year, month, day, 0, 0, 0);
            Long TodayNowTime = calendar.getTime().getTime();
            calendar.set(year, month, day, 23, 59, 59);
            Long TodayNextTime = calendar.getTime().getTime();
            List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", TodayNowTime.toString(), TodayNextTime.toString()).list();
            if (states == null || states.isEmpty()) {
                states = new ArrayList<>();
            }
            mData.add(i, states);
        }
        return mData;
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    public void setDb(SQLiteDatabase db) {
        this.db = db;
    }

    public void setLastWeekStates(List<List<State>> lastWeekStates) {
        this.lastWeekStates = lastWeekStates;
    }

    public void setTodayStates(List<State> todayStates) {
        this.todayStates = todayStates;
    }

    public void setLastTwoHourStates(List<State> lastTwoHourStates) {
        this.lastTwoHourStates = lastTwoHourStates;
    }

    public List<State> getTodayStates() {
        return todayStates;
    }

    public List<State> getLastTwoHourStates() {
        return lastTwoHourStates;
    }

    public ArrayList<String> getLastTwoHourTime() {
        return lastTwoHourTime;
    }

    public List<List<State>> getLastWeekStates() {
        return lastWeekStates;
    }

    public ArrayList<String> getLastWeekDate() {
        return lastWeekDate;
    }

    public void setLastWeekDate(ArrayList<String> lastWeekDate) {
        this.lastWeekDate = lastWeekDate;
    }

    /**
     * return a map contains today pm breathed of each time point
     **/
    public HashMap<Integer, Float> calChart1Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()) {
            return map;
        }
        for (int i = 0; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            float pm25;
            if (i == 0) {
                pm25 = Float.valueOf(state.getPm25());
            } else {
                pm25 = Float.valueOf(state.getPm25()) - Float.valueOf(states.get(i - 1).getPm25());
            }
            //now we get the index of time and the pm25 of that point
            if (map.containsKey(index)) {
                float tmp = map.get(index).floatValue() + pm25;
                map.put(index, tmp);
            } else {
                map.put(index, pm25);
            }
        }
        return map;
    }

    /**
     * return a map contains today pm density of each time point
     **/
    public HashMap<Integer, Float> calChart2Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()) {
            return map;
        }
        HashMap<Integer, Float> tmpMap = new HashMap<>();
        for (int i = 0; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            Float pm25Density;
            //Log.e("calChart2Data Density",String.valueOf(i)+" "+state.getDensity());
            pm25Density = Float.valueOf(state.getDensity());
            //now we get the index of time and the pm25 density of that point
            if (tmpMap.containsKey(index)) {
                //calculate the avg value
                Float last = tmpMap.get(index);
                tmpMap.put(index, (last + pm25Density) / 2);
            } else {
                //just put it
                tmpMap.put(index, pm25Density);
            }
        }
        return tmpMap;
    }

    /**
     * return a map contains today newest time point's pm breathed result
     **/
//    public HashMap<Integer, Float> calChart3Data() {
//        HashMap<Integer, Float> map = new HashMap<>();
//        if (db == null) return map;
//        List<State> states = todayStates;
//        if (states.isEmpty()) {
//            return map;
//        }
//        State state = states.get(states.size() - 1);
//        int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
//        Float pm25 = Float.valueOf(state.getPm25());
//        map.put(index, pm25);
//        return map;
//    }

    public HashMap<Integer,Float> calChart3Data(){
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()) {
            return map;
        }
        ArrayList<Integer> tmpIndex = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            float pm25;
            if (i == 0) {
                pm25 = Float.valueOf(state.getPm25());
            } else {
                pm25 = Float.valueOf(state.getPm25()) - Float.valueOf(states.get(i - 1).getPm25());
            }
            //now we get the index of time and the pm25 of that point
            if (map.containsKey(index)) {
                float tmp = map.get(index).floatValue() + pm25;
                map.put(index, tmp);
            } else {
                map.put(index, pm25);
            }
        }
        //now accumulated
        for(Integer key: map.keySet()){
            tmpIndex.add(key);
        }
        Collections.sort(tmpIndex); //now a ascending list
        HashMap<Integer, Float> result = new HashMap<>();
        float sum = 0;
        for(int i = 0; i != tmpIndex.size(); i ++){
            sum += map.get(tmpIndex.get(i));
            result.put(tmpIndex.get(i),sum);
        }
        return result;
    }

    /**
     * return a map contains today last two hour time point's pm density
     **/
    public HashMap<Integer, Float> calChart4Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = lastTwoHourStates;
        if (states.isEmpty()) {
            return map;
        }
        HashMap<Integer, Float> tmpMap = new HashMap<>();
        HashMap<Integer,Integer> mapSize = new HashMap<>();
        for (int i = 0; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfTwoHour(Long.valueOf(states.get(0).getTime_point()), Long.valueOf(state.getTime_point()));
            Float pm25Density;
            pm25Density = Float.valueOf(state.getDensity());
            //Log.e(TAG,"calChart4Data index"+String.valueOf(i)+" "+String.valueOf(index)+" "+pm25Density);
            //Log.e("calChart4Data Density",String.valueOf(i)+" "+state.getDensity());
            //now we get the index of time and the pm25 density of that point
            if (tmpMap.containsKey(index)) {
                Float last = tmpMap.get(index);
                int size = mapSize.get(index) + 1;
                tmpMap.put(index,last + pm25Density);
                mapSize.put(index,size);
            } else {
                tmpMap.put(index, pm25Density);
                mapSize.put(index,1);
            }
        }
        HashMap<Integer,Float> result = new HashMap<>();
        for(Integer i : tmpMap.keySet()){
            float density = tmpMap.get(i);
            int size = mapSize.get(i);
            result.put(i, density / size);
            //Log.e(TAG,"calChart4Data result "+result.get(i));
        }
        // TODO: 16/2/15 A bug that if all the data are the same, the line chart couldn't show
        // TODO: so that's why we always set data at index 0 to 0.0f in this situation, by that way, the chart could show.
        if(result.size() >= 2) {
            boolean isSet = true;
            float tmp = 0.0f;
            int index = 0;
            tmp = result.get(0).floatValue();
            for (Integer tmpKey : result.keySet()) {
                if(result.get(tmpKey).floatValue() != tmp){
                    isSet = false;
                    break;
                }
            }
            if(isSet)
            result.put(0,0.0f);
        }
        return result;
    }

    /**
     * return a map contains last two hour pm breathed of each time point
     **/
    public HashMap<Integer, Float> calChart5Data() {
        //Log.e("calChart5Data","begin");
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        //Log.e("calChart5Data","db != null");
        List<State> states = lastTwoHourStates;
        if (states.isEmpty()) {
            //Log.e("calChart5Data","states is empty");
            return map;
        }
        //Log.e("calChart5Data","state.size: "+String.valueOf(states.size()));
        HashMap<Integer, Float> tmpMap = new HashMap<>();
        if (states.size() == 1) {
            tmpMap.put(0, Float.valueOf(states.get(0).getPm25()));
            return tmpMap;
        }
        for (int i = 1; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfTwoHour(Long.valueOf(states.get(0).getTime_point()), Long.valueOf(state.getTime_point()));
            Float pmNow, pmLast;
            pmNow = Float.valueOf(state.getPm25());
            pmLast = Float.valueOf(states.get(i - 1).getPm25());
            Float result = pmNow - pmLast; //calculate the 1 min air breathed in
            //Log.e("calChart8Data",String.valueOf(index)+" "+String.valueOf(airNow)+" "+String.valueOf(i) + " "+String.valueOf(result));
            //now we get the index of time and the air  of that point
            if (tmpMap.containsKey(index)) {
                //calculate the sum, since we need 5 min air breathed in
                tmpMap.put(index, tmpMap.get(index) + result);
            } else {
                tmpMap.put(index, result);
            }
        }
        return tmpMap;
    }

    /**
     * return a map contains today air breathed of each time point
     **/
    public HashMap<Integer, Float> calChart6Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()) {
            return map;
        }
        Map<Integer, Float> tmpMap = new HashMap<>();
        Map<Integer,Integer> mapSize = new HashMap<>();
        for (int i = 0; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            float air;
            if (i == 0) {
                air = Float.valueOf(state.getVentilation_volume());
            } else {
                air = Float.valueOf(state.getVentilation_volume()) - Float.valueOf(states.get(i - 1).getVentilation_volume());
            }
            //now we get the index of time and the pm25 of that point
            //Log.e(TAG,"calChart6Data "+i+" "+index+" "+air);
            if(tmpMap.containsKey(index)){
                int airSize = mapSize.get(index) + 1;
                float airTotal = tmpMap.get(index);
                tmpMap.put(index,airTotal+air);
                mapSize.put(index,airSize);
            }else {
                mapSize.put(index,1);
                tmpMap.put(index, air);
            }
        }
        //now calculate the sum of value
        for (int i = 0; i != 48; i++) {
//            Log.d(TAG,"cal Chart 6 "+String.valueOf(i)+" "+String.valueOf(tmpMap.get(i)));
            if (tmpMap.containsKey(i)) {
                //Log.e(TAG,"i "+i+" "+"tmpMap "+tmpMap.get(i)+" mapSize "+mapSize.get(i));
                map.put(i,tmpMap.get(i) / mapSize.get(i));
                //map.put(i, ShortcutUtil.avgOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }


    /**
     * Return a map contains last week pm breathed of each day. today's index is 0
     **/
    public HashMap<Integer, Float> calChart7Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<List<State>> datas = getLastWeekStates();
        if (datas.isEmpty()) return map;
        for (int i = 0; i != datas.size(); i++) {
            List<State> state = datas.get(i);
            if (state.isEmpty())
                map.put(i, 0.0f);
            else
            map.put(i, Float.valueOf(state.get(state.size() - 1).getPm25()));
        }
        return map;
    }

    /**
     * return a map contains last two hour air breathed of each time point
     **/
    public HashMap<Integer, Float> calChart8Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = lastTwoHourStates;
        if (states.isEmpty()) {
            return map;
        }
        //Log.e("calChart8Data firsttime",ShortcutUtil.refFormatNowDate(Long.valueOf(states.get(0).getTime_point())));
        //Log.e("calChart8Data lasttime",ShortcutUtil.refFormatNowDate(Long.valueOf(states.get(states.size() - 1).getTime_point())));
//        for(int i = 0; i != states.size(); i++){
//            Log.e("calChart8Data",String.valueOf(i)+" "+ShortcutUtil.refFormatNowDate(Long.valueOf(states.get(i).getTime_point()))+" "+states.get(i).getVentilation_volume());
//        }
        HashMap<Integer, Float> tmpMap = new HashMap<>();
        if (states.size() == 1) {
            tmpMap.put(0, Float.valueOf(states.get(0).getVentilation_volume()));
            return tmpMap;
        }
        HashMap<Integer,Integer> tmpNumMap = new HashMap<>();
        for (int i = 1; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfTwoHour(Long.valueOf(states.get(0).getTime_point()), Long.valueOf(state.getTime_point()));
            Float airNow, airLast;
            airNow = Float.valueOf(state.getVentilation_volume());
            airLast = Float.valueOf(states.get(i - 1).getVentilation_volume());
            Float result = airNow - airLast; //calculate the 1 min air breathed in
            //Log.e("calChart8Data",String.valueOf(index)+" "+String.valueOf(airNow)+" "+String.valueOf(i) + " "+String.valueOf(result));
            //now we get the index of time and the air  of that point
            if (tmpMap.containsKey(index)) {
                //calculate the sum, but we need avg value of 5 min air breathed in
                tmpMap.put(index, tmpMap.get(index) + result);
                int tmp = tmpNumMap.get(index);
                tmp++;
                tmpNumMap.put(index,tmp);
            } else {
                tmpMap.put(index, result);
                tmpNumMap.put(index,1);
            }
        }
        //now calculate the avg value of each point
        for(Integer key : tmpMap.keySet()){
            Float sum = tmpMap.get(key);
            int num = tmpNumMap.get(key);
            tmpMap.put(key, sum / num);
        }
        //DebugUtil.printMap("sss",tmpNumMap);
        return tmpMap;
    }

    /**
     * return a map contains today newest time point's air breathed result
     **/
    public HashMap<Integer,Float> calChart10Data(){
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()) {
            return map;
        }
        ArrayList<Integer> tmpIndex = new ArrayList<>();
        for (int i = 0; i != states.size(); i++) {
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            float air;
            if (i == 0) {
                air = Float.valueOf(state.getVentilation_volume());
            } else {
                air = Float.valueOf(state.getVentilation_volume()) - Float.valueOf(states.get(i - 1).getVentilation_volume());
            }
            //now we get the index of time and the air of that point
            if (map.containsKey(index)) {
                float tmp = map.get(index).floatValue() + air;
                map.put(index, tmp);
            } else {
                map.put(index, air);
            }
        }
        //now accumulated
        for(Integer key: map.keySet()){
            tmpIndex.add(key);
        }
        Collections.sort(tmpIndex); //now a ascending list
        HashMap<Integer, Float> result = new HashMap<>();
        float sum = 0;
        for(int i = 0; i != tmpIndex.size(); i ++){
            sum += map.get(tmpIndex.get(i));
            result.put(tmpIndex.get(i),sum);
        }
        return result;
    }

//    public HashMap<Integer, Float> calChart10Data() {
//        HashMap<Integer, Float> map = new HashMap<>();
//        if (db == null) return map;
//        List<State> states = todayStates;
//        if (states.isEmpty()) {
//            return map;
//        }
//        State state = states.get(states.size() - 1);
//        int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
//        //Log.e("calChart10Data",String.valueOf(index));
//        Float air = Float.valueOf(state.getVentilation_volume());
//        map.put(index, air);
//        return map;
//    }

    /**
     * Return a map contains last week air breathed of each day. today's index is 0
     **/
    public HashMap<Integer, Float> calChart12Data() {
        HashMap<Integer, Float> map = new HashMap<>();
        if (db == null) return map;
        List<List<State>> datas = getLastWeekStates();
        if (datas.isEmpty()) return map;
        for (int i = 0; i != datas.size(); i++) {
            //Log.d(TAG,"calChart12Data: "+String.valueOf(i)+" "+String.valueOf(datas.get(i).size()));
            List<State> state = datas.get(i);
            if (state.isEmpty())
                map.put(i, 0.0f);
            else
                map.put(i, Float.valueOf(state.get(state.size() - 1).getVentilation_volume()));
        }
        return map;
    }
}

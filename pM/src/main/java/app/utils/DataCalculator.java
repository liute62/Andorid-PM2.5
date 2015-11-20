package app.utils;

import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by Haodong Liu on 11/20/2015.
 */
public class DataCalculator {
    SQLiteDatabase db;
    List<State> todayStates;
    List<State> lastTwoHourStates;

    private static DataCalculator instance = null;

    public static DataCalculator getIntance(SQLiteDatabase db){
        if (instance == null){
            return new DataCalculator(db);
        }
        return instance;
    }

    private DataCalculator(SQLiteDatabase db){
        this.db = db;
        this.todayStates = getTodayStates();
        this.lastTwoHourStates = getLastTwoHourStates(); //Actually the time is set here before function be invoked. But it's ok.
    }

    public void updateState(){
        this.todayStates = getTodayStates();
        this.lastTwoHourStates = getLastTwoHourStates(); //Actually the time is set here before function be invoked. But it's ok.
    }

    private List<State> getTodayStates(){
        if(db == null) return new ArrayList<State>();
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

    private List<State> getLastTwoHourStates(){
        if(db == null) return new ArrayList<State>();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        Time t = new Time();
        t.setToNow();
        int currentHour = t.hour;
        int currentMin = t.minute;
        int lastTwoHour = currentHour - 2;
        if(lastTwoHour < 0) lastTwoHour = 0;
        calendar.set(year, month, day, lastTwoHour, 0, 0);
        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, currentHour, currentMin, 59);
        Long nextTime = calendar.getTime().getTime();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        return states;
    }

    /**return a map contains today pm breathed of each time point**/
    public HashMap<Integer,Float> calChart1Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            float pm25;
            if(i == 0){
                pm25 = Float.valueOf(state.getPm25());
            }else {
                pm25 = Float.valueOf(state.getPm25()) - Float.valueOf(states.get(i-1).getPm25());
            }
            //now we get the index of time and the pm25 of that point
            tmpMap.put(index, pm25);
        }
        //now calculate the sum of value
        for (int i = 0; i != 48; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.sumOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }

    /**return a map contains today pm density of each time point**/
    public HashMap<Integer,Float> calChart2Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            Float pm25Density;
            pm25Density = Float.valueOf(state.getDensity());
            //now we get the index of time and the pm25 density of that point
            tmpMap.put(index, pm25Density);
        }
        //now calculate the avg of value
        for (int i = 0; i != 48; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.avgOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }

    /**return a map contains today newest time point's pm breathed result**/
    public HashMap<Integer,Float> calChart3Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        State state = states.get(states.size() - 1);
        int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
        Float pm25 = Float.valueOf(state.getPm25());
        map.put(index,pm25);
        return map;
    }

    /**return a map contains today last two hour time point's pm density**/
    public HashMap<Integer,Float> calChart4Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = lastTwoHourStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfTwoHour(Long.valueOf(states.get(0).getTime_point()),Long.valueOf(state.getTime_point()));
            Float pm25Density;
            pm25Density = Float.valueOf(state.getDensity());
            //now we get the index of time and the pm25 density of that point
            tmpMap.put(index, pm25Density);
        }
        //now calculate the sum of value
        for (int i = 0; i != 24; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.avgOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }

    /**return a map contains last two hour pm breathed of each time point**/
    public HashMap<Integer,Float> calChart5Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = lastTwoHourStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfTwoHour(Long.valueOf(states.get(0).getTime_point()),Long.valueOf(state.getTime_point()));
            Float pm25Density;
            pm25Density = Float.valueOf(state.getDensity());
            //now we get the index of time and the pm25 density of that point
            tmpMap.put(index, pm25Density);
        }
        //now calculate the sum of value
        for (int i = 0; i != 24; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.sumOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }

    /**return a map contains today air breathed of each time point**/
    public HashMap<Integer,Float> calChart6Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
            float air;
            if(i == 0){
                air = Float.valueOf(state.getVentilation_volume());
            }else {
                air = Float.valueOf(state.getVentilation_volume()) - Float.valueOf(states.get(i-1).getVentilation_volume());
            }
            //now we get the index of time and the pm25 of that point
            tmpMap.put(index, air);
        }
        //now calculate the sum of value
        for (int i = 0; i != 48; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.avgOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }


    public HashMap<Integer,Float> calChart7Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        return map;
    }

    /**return a map contains last two hour air breathed of each time point**/
    public HashMap<Integer,Float> calChart8Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = lastTwoHourStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        for(int i = 0; i != states.size(); i++){
            State state = states.get(i);
            int index = ShortcutUtil.timeToPointOfTwoHour(Long.valueOf(states.get(0).getTime_point()),Long.valueOf(state.getTime_point()));
            Float air;
            air = Float.valueOf(state.getVentilation_volume());
            //now we get the index of time and the air density of that point
            tmpMap.put(index, air);
        }
        //now calculate the sum of value
        for (int i = 0; i != 24; i++) {
            if (tmpMap.containsKey(i)) {
                map.put(i,ShortcutUtil.avgOfArrayNum(tmpMap.values().toArray()));
            }
        }
        return map;
    }

    /**return a map contains today newest time point's air breathed result**/
    public HashMap<Integer,Float> calChart10Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        State state = states.get(states.size() - 1);
        int index = ShortcutUtil.timeToPointOfDay(Long.valueOf(state.getTime_point()));
        Float air = Float.valueOf(state.getVentilation_volume());
        map.put(index,air);
        return map;
    }


    public HashMap<Integer,Float> calChart12Data(){
        HashMap<Integer,Float> map = new HashMap<>();
        if(db == null) return map;
        List<State> states = todayStates;
        if (states.isEmpty()){
            return map;
        }
        Map<Integer,Float> tmpMap = new HashMap<>();
        return map;
    }
}

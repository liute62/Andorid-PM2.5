package app.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import app.Entity.State;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBConstants;
import app.utils.DBHelper;
import app.utils.FileUtil;
import app.utils.ShortcutUtil;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 16/6/9.
 * This is a service intended to perform database related operations.
 *
 */
public class DataServiceUtil {

    public static final String TAG = "DataService";

    private static DataServiceUtil instance = null;

    private DBHelper dbHelper = null;

    private Context mContext;

    private State state = null;

    private double venVolToday;

    private long IDToday;

    private double PM25Density = -1;

    private double PM25Today;

    private int PM25Source;

    private ACache aCache;

    private int inOutDoor;

    private String avg_rate = "12";

    public static DataServiceUtil getInstance(Context context){
       if(instance == null){
           instance = new DataServiceUtil(context.getApplicationContext());
       }
        return instance;
    }

    private DataServiceUtil(Context context){
        mContext = context;
        aCache = ACache.get(context);
        DBInitial();
    }

    private void DBInitial() {

        if (null == dbHelper)
            dbHelper = new DBHelper(mContext.getApplicationContext());

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.set(year, month, day, 0, 0, 0);
        Long nowTime = calendar.getTime().getTime();
        calendar.set(year, month, day, 23, 59, 59);
        Long nextTime = calendar.getTime().getTime();
        /**Get states of today **/
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", nowTime.toString(), nextTime.toString()).list();
        if (states.isEmpty()) {
            state = null;
            PM25Today = 0.0;
            venVolToday = 0.0;
            IDToday = 0L;
        } else {
            State state = states.get(states.size() - 1);
            this.state = state;
            state.print();
            PM25Today = Double.parseDouble(state.getPm25());
            venVolToday = Double.parseDouble(state.getVentilation_volume());
            IDToday = state.getId() + 1;
        }
    }

    /**
     *
     */
    public void initDefaultData(){
        if (aCache.getAsString(Const.Cache_PM_Density) != null) {
            PM25Density = Double.valueOf(aCache.getAsString(Const.Cache_PM_Density));
        }
        if (aCache.getAsString(Const.Cache_PM_Source) != null) {
            int source;
            try {
                source = Integer.valueOf(aCache.getAsString(Const.Cache_PM_Source));
            } catch (Exception e) {
                FileUtil.appendErrorToFile(0, "error in parse source from string to integer");
                source = 0;
            }
            PM25Source = source;
        }
    }

    /**
     * density: (ug/m3)
     * breath:  (L/min)
     * Calculate today the number of pm2.5 breathed until now
     *
     * @param longi the longitude to be saved
     * @param lati  the latitude to be saved
     * @return the saved state
     */
    public State calculatePM25(double longi, double lati) {

        Log.e(TAG, "calculatePM25 " + System.currentTimeMillis());
        Double breath = 0.0;
        Double density = PM25Density;
        boolean isConnected = ShortcutUtil.isNetworkAvailable(mContext);

        Const.MotionStatus mMotionStatus = Const.MotionStatus.STATIC; // motionService.getMotionStatus();
        // int numStepsForRecord = motionService.getNumStepsForRecord();
        int numStepsForRecord = 0;
        double static_breath = ShortcutUtil.calStaticBreath(aCache.getAsString(Const.Cache_User_Weight));

        if (static_breath == 0.0) {
            static_breath = 6.6; // using the default one
        }
        if (mMotionStatus == Const.MotionStatus.STATIC) {
            breath = static_breath;
        } else if (mMotionStatus == Const.MotionStatus.WALK) {
            breath = static_breath * 2.1;
        } else if (mMotionStatus == Const.MotionStatus.RUN) {
            breath = static_breath * 6;
        }

        venVolToday += breath;
        breath = breath / 1000; //change L/min to m3/min
        PM25Today += density * breath;

        State state = new State(IDToday, aCache.getAsString(Const.Cache_User_Id), Long.toString(System.currentTimeMillis()),
                String.valueOf(longi),
                String.valueOf(lati),
                String.valueOf(inOutDoor),
                mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                Integer.toString(numStepsForRecord), avg_rate, String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today), String.valueOf(PM25Source), 0, isConnected ? 1 : 0);
        numStepsForRecord = 0;
        return state;
    }

    /**
     * DB Operations, insert a calculated pm state model to DB
     *
     * @param state
     */
    public void insertState(State state) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        state.print();
        cupboard().withDatabase(db).put(state);
        IDToday++;
        aCache.put(Const.Cache_Lastime_Timepoint, state.getTime_point());
    }

    /**
     *
     * @param state
     * @param upload
     */
    public void updateStateUpLoad(State state, int upload) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, upload);
        cupboard().withDatabase(db).update(State.class, values, "id = ?", state.getId() + "");
    }

    /**
     *
     * @return
     */
    public double getLastSevenDaysInOutRatio() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<State> states = new ArrayList<State>();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 1; i <= 7; i++) {
            Calendar nowTime = Calendar.getInstance();
            nowTime.add(Calendar.DAY_OF_MONTH, -i);
            nowTime.add(Calendar.MINUTE, -5);
            String left = formatter.format(nowTime.getTime());
            nowTime.add(Calendar.MINUTE, 10);
            String right = formatter.format(nowTime.getTime());
            List<State> temp = cupboard().withDatabase(db).query(State.class).withSelection("time_point > ? AND time_point < ?", left, right).list();
            states.addAll(temp);
        }
        int count = 0;
        for (State state : states) {
            if (state.getOutdoor().equals(LocationServiceUtil.Outdoor)) {
                count++;
            }
        }
        if (states.size() == 0) {
            return 0.5;
        }
        double ratio = count * 1.0 / states.size();
        return ratio;
    }

    public List<State> getPMDataForUpload(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_HAS_UPLOAD +
                "=? AND " + DBConstants.DB_MetaData.STATE_CONNECTION + "=?", "0", "1").list();
    }

    /**
     *
     */
    public void reset() {

    }

    public void cachePMResult(double density,int source){
        aCache.put(Const.Cache_PM_Density, density);
        aCache.put(Const.Cache_PM_Source, String.valueOf(source));
    }

    /**
     *
     * @param inOutDoor must be
     * @see LocationServiceUtil indoor and outdoor
     */
    public void cacheInOutdoor(int inOutDoor){
        aCache.put(Const.Cache_Indoor_Outdoor, String.valueOf(inOutDoor));
    }

    public void cacheLocation(Location location){
        aCache.put(Const.Cache_Latitude, location.getLatitude());
        aCache.put(Const.Cache_Longitude, location.getLongitude());
    }

    public void cacheLocation(double latitude,double longitude){
        aCache.put(Const.Cache_Latitude, latitude);
        aCache.put(Const.Cache_Longitude, longitude);
    }

    public void cacheMaxLocation(Location location){
        aCache.put(Const.Cache_Latitude, location.getLatitude());
        aCache.put(Const.Cache_Longitude, location.getLongitude());
    }

    public void cacheMaxLocation(double latitude,double longitude){
        aCache.put(Const.Cache_Last_Max_Lati, latitude);
        aCache.put(Const.Cache_Last_Max_Longi, longitude);
    }

    public void cacheLastUploadTime(long time){
        aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(time));
    }

    public State getCurrentState(){
        return state;
    }

    public void setCurrentState(State state){
        this.state = state;
    }

    public int getInOutDoor(){
        return 0;
    }

    public double getLatitude(){
        String lati = aCache.getAsString(Const.Cache_Latitude);
        if(ShortcutUtil.isStringOK(lati)){
            return Double.valueOf(lati);
        }
        return 0.0;
    }

    public double getLongitude(){
        String longi = aCache.getAsString(Const.Cache_Longitude);
        if(ShortcutUtil.isStringOK(longi)){
            return Double.valueOf(longi);
        }
        return 0.0;
    }

    public double getMaxLatitude(){
        String lati = aCache.getAsString(Const.Cache_Last_Max_Lati);
        if(ShortcutUtil.isStringOK(lati)){
            return Double.valueOf(lati);
        }
        return 0.0;
    }

    public double getMaxLongitude(){
        String longi = aCache.getAsString(Const.Cache_Last_Max_Longi);
        if(ShortcutUtil.isStringOK(longi)){
            return Double.valueOf(longi);
        }
        return 0.0;
    }

    public double getPM25Density() {
        return PM25Density;
    }

    public double getPM25Today() {
        return PM25Today;
    }

    public int getPM25Source() {
        return PM25Source;
    }

    public long getIDToday() {
        return IDToday;
    }

    public double getVenVolToday() {
        return venVolToday;
    }

    public DBHelper getDBHelper() {
        return dbHelper;
    }
}

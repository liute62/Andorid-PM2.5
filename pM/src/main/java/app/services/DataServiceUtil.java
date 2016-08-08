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
import app.utils.StableCache;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 16/6/9.
 * This is a service intended to perform database related operations.
 */
public class DataServiceUtil {

    public static final String TAG = "DataService";

    private static DataServiceUtil instance = null;

    private DBHelper dbHelper = null;

    private Context mContext;

    private State state = null; // a stable holding of current state of DB

    private double venVolToday;

    private long IDToday;

    private double PM25Density = -1; //a stable holding of pm density of cache.

    private double PM25Today;

    private int PM25Source; //a stable holding of pm density of cache.

    private ACache aCache;

    private StableCache stableCache;

    private int avg_rate = 12; //a stable holding of current hearth rate of cache.

    public static DataServiceUtil getInstance(Context context){
       if(instance == null){
           instance = new DataServiceUtil(context.getApplicationContext());
       }
        return instance;
    }

    private DataServiceUtil(Context context){
        mContext = context;
        aCache = ACache.get(context);
        stableCache = StableCache.getInstance(context);
        initDefaultData();
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
        PM25Density = getPMDensityFromCache();
        PM25Source = getPM25Source();
        avg_rate = getHearthRateFromCache();
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
    public State calculatePM25(double longi, double lati,int steps) {

        Log.e(TAG, "calculatePM25 " + System.currentTimeMillis());
        Double breath = 0.0;
        Double density = PM25Density;
        boolean isConnected = ShortcutUtil.isNetworkAvailable(mContext);

        Const.MotionStatus mMotionStatus = MotionServiceUtil.getMotionStatus(steps);
        double static_breath =
                ShortcutUtil.calStaticBreath(stableCache.getAsString(Const.Cache_User_Weight));

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

        try {
            state = new State(IDToday, String.valueOf(getUserIdFromCache()), Long.toString(System.currentTimeMillis()),
                    String.valueOf(longi),
                    String.valueOf(lati),
                    String.valueOf(getInOutDoorFromCache()),
                    mMotionStatus == Const.MotionStatus.STATIC ? "1" : mMotionStatus == Const.MotionStatus.WALK ? "2" : "3",
                    Integer.toString(steps), String.valueOf(avg_rate),
                    String.valueOf(venVolToday), density.toString(), String.valueOf(PM25Today),
                    String.valueOf(PM25Source), 0, isConnected ? 1 : 0);
        }catch (Exception e){
            FileUtil.appendErrorToFile(TAG,"calculatePM25 error in initialized state");
        }
        return state;
    }

    /**
     * DB Operations, insert a calculated pm state model to DB
     * @param state the state you want to insert
     */
    public void insertState(State state) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        state.print();
        cupboard().withDatabase(db).put(state);
        IDToday++;
    }

    /**
     * DB Operations, update the upload value of state
     * @param state the state to update
     * @param upload the value to update
     */
    public void updateStateUpLoad(State state, int upload) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, upload);
        cupboard().withDatabase(db).update(State.class, values, "id = ?", state.getId() + "");
    }

    /**
     * get last indoor and outdoor ratio during last seven days
     * @return the ratio of last week.
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

    /**
     * get list of data to upload from DB
     * @return a list of state for uploading
     */
    public List<State> getPMDataForUpload(){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        return cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_HAS_UPLOAD +
                "=? AND " + DBConstants.DB_MetaData.STATE_CONNECTION + "=?", "0", "1").list();
    }

    /*
    * reset the tmporary data for a new day after day surpass
     */
    public void reset(){
        IDToday = 0;
        PM25Today = 0;
        venVolToday = 0;
    }


    /*
    * refresh the temporary value of DataServiceUtil
     */
    public void refresh(){
        DBInitial();
    }

    /****************** Below here are cache operations ******************/
    /*
    * set user's id into cache
     */
    public void cacheUsrId(int id){
        aCache.put(Const.Cache_User_Id, String.valueOf(id));
    }

    /**
     * cache the pm density and source.
     * @param density the current pm density to cache
     * @param source the source of pm data to cache
     */
    public void cachePMResult(double density,int source){
        aCache.put(Const.Cache_PM_Density, density);
        aCache.put(Const.Cache_PM_Source, String.valueOf(source));
        PM25Density = density;
    }

    /**
     * cache the consecutive times for searching pm2.5 density and failed.
     * @param count the consecutive failed times for getting pm2.5 result
     */
    public void cacheSearchPMFailed(int count){
        aCache.put(Const.Cache_Search_PM_Failed_Count, String.valueOf(count));
    }

    /**
     * cache the in and outdoor status
     * @param inOutDoor must be 1 or 0, 1 means: outdoor 0 means: indoor
     * @see LocationServiceUtil indoor and outdoor
     */
    public void cacheInOutdoor(int inOutDoor){
        if(inOutDoor == LocationServiceUtil.Indoor ||
                inOutDoor == LocationServiceUtil.Outdoor)
            aCache.put(Const.Cache_Indoor_Outdoor, String.valueOf(inOutDoor));
        else FileUtil.appendErrorToFile(TAG,
                "cacheInOutdoor value == "+inOutDoor+" not meet the requirement");
    }

    /**
     * cache name of the city
     * @param city the name of city
     */
    public void cacheCityName(String city){
        aCache.put(Const.Cache_City,city);
    }

    /**
     * cache the current location
     * @param location the location to cache
     */
    public void cacheLocation(Location location){
        if(location != null) {
            aCache.put(Const.Cache_Latitude, location.getLatitude());
            aCache.put(Const.Cache_Longitude, location.getLongitude());
        }else {
            FileUtil.appendErrorToFile(TAG,
                    "cacheLocation location == NULL");
        }
    }

    /**
     * cache the current location
     * @param latitude the latitude of current location
     * @param longitude the longitude of current location
     */
    public void cacheLocation(double latitude,double longitude){
        aCache.put(Const.Cache_Latitude, latitude);
        aCache.put(Const.Cache_Longitude, longitude);
    }

    /*
    * cache if the insertion data process has surpass a day
    * if surpass, need to reset aimed temporary values.
    * isReset, true: isSurpass, false: not Surpass
     */
    public void cacheSurpass(boolean isReset){
        aCache.put(Const.Cache_Surpass_Reset,isReset);
    }

    /**
     * cache the maximum location.
     * @param location the maximum location to cache
     */
    public void cacheMaxLocation(Location location){
        aCache.put(Const.Cache_Last_Max_Lati, location.getLatitude());
        aCache.put(Const.Cache_Last_Max_Lati, location.getLongitude());
    }

    /*
    * cache the maximum location by latitude and longitude
     */
    public void cacheMaxLocation(double latitude,double longitude){
        aCache.put(Const.Cache_Last_Max_Lati, latitude);
        aCache.put(Const.Cache_Last_Max_Longi, longitude);
    }

    /**
     * cache the time for uploading to see if next time need to upload.
     * @param time the current time for uploading
     */
    public void cacheLastUploadTime(long time){
        aCache.put(Const.Cache_DB_Lastime_Upload, String.valueOf(time));
    }

    public void cacheLastSearchCityTime(long time){
        aCache.put(Const.Cache_Lasttime_Search_City,String.valueOf(time));
    }

    /**
     * cache the number of user's steps.
     * @param stepNum the number of user's steps
     */
    public void cacheStepNum(int stepNum){
        aCache.put(Const.Cache_Step_Num,String.valueOf(stepNum));
    }

    public void cacheHearthRate(int rate){
        aCache.put(Const.Cache_Hearth_Rate,String.valueOf(rate));
        avg_rate = rate;
    }

    public void cacheIsSearchDensity(boolean isTo){
        aCache.put(Const.Cache_Is_To_Search_Density,String.valueOf(isTo));
    }

    public void cacheHasStepCounter(boolean has){
        aCache.put(Const.Cache_Has_Step_Counter,String.valueOf(has));
    }

    /**
     * get the user's id from cache, if failed, return 0
     * @return the user id
     */
    public int getUserIdFromCache(){
        String usrIdStr = aCache.getAsString(Const.Cache_User_Id);
        if(ShortcutUtil.isStringOK(usrIdStr)){
            try{
                return Integer.valueOf(usrIdStr);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG,"getUserIdFromCache parsing error"
                        +" in/outdoor == "+usrIdStr);
                return 0;
            }
        }else {
            return 0;
        }
    }

    public double getPMDensityFromCache(){

        String tmp = aCache.getAsString(Const.Cache_PM_Density);
        if(ShortcutUtil.isStringOK(tmp)){
            try {
                return Double.valueOf(aCache.getAsString(Const.Cache_PM_Density));
            } catch (Exception e) {
                FileUtil.appendErrorToFile(TAG, "getPMDensityFromCache parsing error "+
                        tmp);
                cacheIsSearchDensity(true);
                return 0.0;
            }
        }else {
            FileUtil.appendErrorToFile(TAG, "getPMDensityFromCache PM Density == NULL");
            cacheIsSearchDensity(true);
        }
        return 0.0;
    }

    public int getPMSourceFromCache(){
        String tmp = aCache.getAsString(Const.Cache_PM_Source);
        if (ShortcutUtil.isStringOK(tmp)) {
            try {
                return Integer.valueOf(aCache.getAsString(Const.Cache_PM_Source));
            } catch (Exception e) {
                FileUtil.appendErrorToFile(TAG, "getPMSourceFromCache parsing error "+
                        tmp);
            }
        }
        return 0;
    }

    /**
     * get indoor and outdoor status from cache.
     * @return 0 means indoor, 1 means outdoor
     */
    public int getInOutDoorFromCache(){

        int result = LocationServiceUtil.Indoor;
        String str = aCache.getAsString(Const.Cache_Indoor_Outdoor);
        if(ShortcutUtil.isStringOK(str)){
            try {
                result =  Integer.valueOf(str);
            }catch (Exception e){
                result = LocationServiceUtil.Indoor;
                FileUtil.appendErrorToFile(TAG,"getInOutDoorFromCache parsing error"
                        +" in/outdoor == "+str);
            }
        }
        return result;
    }

    /**
     * get the value for is surpassing a day from cache
     * @return true : surpass, false: not surpass
     */
    public boolean isSurpassFromCache(){
        boolean reset = false;
        String tmp = null;
        try {
            tmp = aCache.getAsString(Const.Cache_Surpass_Reset);
            if(ShortcutUtil.isStringOK(tmp))
                reset = Boolean.valueOf(tmp);
        }catch (Exception e){
            FileUtil.appendErrorToFile(TAG,"isSurpassFromCache parsing error"
                    +" isSurpass == "+tmp);
            return false;
        }
        return reset;
    }

    /**
     * get the current longitude from cache.
     * @return 0.0 if not found
     */
    public double getLongitudeFromCache(){
        String longi = aCache.getAsString(Const.Cache_Longitude);
        if(ShortcutUtil.isStringOK(longi)){
            try{
                return Double.valueOf(longi);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG,"getLongitudeFromCache parsing error"
                        +" lati == "+longi);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * get the current latitude from cache.
     * @return 0.0 if not found
     */
    public double getLatitudeFromCache(){
        String lati = aCache.getAsString(Const.Cache_Latitude);
        if(ShortcutUtil.isStringOK(lati)){
            try{
                return Double.valueOf(lati);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG,"getLatitudeFromCache parsing error"
                        +" lati == "+lati);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * get the maximum latitude from cache.
     * @return 0.0 if not found
     */
    public double getMaxLatitudeFromCache(){
        String lati = aCache.getAsString(Const.Cache_Last_Max_Lati);
        if(ShortcutUtil.isStringOK(lati)){
            try{
                return Double.valueOf(lati);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG,"getMaxLatitudeFromCache parsing error"
                        +" lati == "+lati);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * get the maximum longitude from cache.
     * @return 0.0 if not found
     */
    public double getMaxLongitudeFromCache(){
        String longi = aCache.getAsString(Const.Cache_Last_Max_Longi);
        if(ShortcutUtil.isStringOK(longi)){
            try {
                return Double.valueOf(longi);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG,"getMaxLongitudeFromCache parsing error"
                        +" longi == "+longi);
                return 0.0;
            }
        }
        return 0.0;
    }

    /**
     * get the current city name from cache.
     * @return the string of city name, return "null" if failed
     */
    public String getCityNameFromCache(){
        String city = aCache.getAsString(Const.Cache_City);
        if(ShortcutUtil.isStringOK(city)){
            return city;
        }
        return "null";
    }

    /**
     * get the number of failed searching pm density from cache.
     * @return the consecutive search pm density failed count, if failed, 0
     */
    public int getSearchFailedCountFromCache(){
        int result = 0;
        String count = aCache.getAsString(Const.Cache_Search_PM_Failed_Count);
        if(ShortcutUtil.isStringOK(count)){
            try {
                result =  Integer.valueOf(count);
            }catch (Exception e){
                result = 0;
                FileUtil.appendErrorToFile(TAG,"getSearchFailedCountFromCache parsing error"
                +" count == "+count);
            }
        }
        return result;
    }

    /**
     * get the current step number of user
     * @return the current step number
     */
    public int getStepNumFromCache(){
        int result = 0;
        String num = aCache.getAsString(Const.Cache_Step_Num);
        if(ShortcutUtil.isStringOK(num)){
            try {
                result = Integer.valueOf(num);
            }catch (Exception e){
                result = 0;
                FileUtil.appendErrorToFile(TAG,"getStepNumFromCache parsing error "+
                num);
            }
        }
        return result;
    }

    /*
     * get the hearth rate of the user from cache
     * @return the hearth rate
     */
    public int getHearthRateFromCache(){
        int rate = 12;
        String str = aCache.getAsString(Const.Cache_Hearth_Rate);
        if(ShortcutUtil.isStringOK(str)){
            try {
                rate = Integer.valueOf(str);
            }catch (Exception e){
                rate = 12;
                FileUtil.appendErrorToFile(TAG,"getHearthRateFromCache parsing error "+
                str);
            }
        }
        return rate;
    }

    /**
     * check if it is time for uploading db data from cache.
     * @return true: is going to upload, false: otherwise
     */
    public boolean isToUpload() {
        String tmp = aCache.getAsString(Const.Cache_DB_Lastime_Upload);
        if (ShortcutUtil.isStringOK(tmp)) {
            try {
                long time = Long.valueOf(tmp);
                if (System.currentTimeMillis() - time > Const.Min_Upload_Check_Time) {
                    return true;
                }
            } catch (Exception e) {
                FileUtil.appendErrorToFile(TAG, "isToUpload failed, parsing error lasttime == "
                        +tmp);
                cacheLastUploadTime(System.currentTimeMillis());
                return false;
            }
        }else {
            cacheLastUploadTime(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public boolean isToSearchCity(){
        String tmp = aCache.getAsString(Const.Cache_Lasttime_Search_City);
        if(ShortcutUtil.isStringOK(tmp)){
            try {
                long time = Long.valueOf(tmp);
                if(System.currentTimeMillis() - time > Const.Min_Search_City_Time){
                    return true;
                }
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG, "isToSearchCity failed, parsing error lastime == "
                        +tmp);
                cacheLastUploadTime(System.currentTimeMillis());
                return false;
            }
        }else {
            cacheLastUploadTime(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public boolean isToSearchPMDensity(){
        String tmp = aCache.getAsString(Const.Cache_Is_To_Search_Density);
        if(ShortcutUtil.isStringOK(tmp)){
            try {
               return Boolean.valueOf(tmp);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG, "isToSearchPMDensity failed, parsing error tmp == "
                        +tmp);
                cacheIsSearchDensity(false);
                return false;
            }
        }
        return false;
    }

    public boolean isHasStepCounter(){
        String tmp = aCache.getAsString(Const.Cache_Has_Step_Counter);
        if(ShortcutUtil.isStringOK(tmp)){
            try {
                return Boolean.valueOf(tmp);
            }catch (Exception e){
                FileUtil.appendErrorToFile(TAG, "isHasStepCounter failed, parsing error tmp == "
                        +tmp);
                cacheHasStepCounter(false);
                return false;
            }
        }
        return false;
    }

    public State getCurrentState(){
        return state;
    }

    public void setCurrentState(State state){
        this.state = state;
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

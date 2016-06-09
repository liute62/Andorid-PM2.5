package app.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by liuhaodong1 on 16/6/9.
 * This is a service intended to perform database related operations.
 */
public class DataService {

    private DBHelper dbHelper = null;

    private Context mContext;

    private double venVolToday;

    private long IDToday;

    private double PM25Density;

    private double PM25Today;

    private int PM25Source;

    private ACache aCache;

    public DataService(Context context){
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
            PM25Today = 0.0;
            venVolToday = 0.0;
            IDToday = 0L;
        } else {
            State state = states.get(states.size() - 1);
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
            if (state.getOutdoor().equals(LocationService.Outdoor)) {
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

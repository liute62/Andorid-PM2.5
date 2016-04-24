package app.services;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.model.PMModel;
import app.utils.ACache;
import app.utils.CacheUtil;
import app.utils.Const;
import app.utils.DBConstants;
import app.utils.DBHelper;
import app.utils.FileUtil;
import app.utils.HttpUtil;
import app.utils.ShortcutUtil;
import app.utils.VolleyQueue;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by Jason on 2015/12/14.
 * Step 1: get all not connection
 * Step 2: get real density
 * Step 3: insert into the database with real density
 * Step 4: update the whole pm25
 * Step 5: upload the real state
 */
public class UpdateService {

    public static final String TAG = "UpdateService";
    private static UpdateService instance = null;
    private Context mContext;
    private DBHelper dbHelper;
    private ACache aCache;
    private CacheUtil cacheUtil;

    public synchronized static void run(Context context,ACache aCache,DBHelper dbHelper){
        if(instance == null){
            instance = new UpdateService(context,aCache,dbHelper);
        }
        instance.runInner();
    }

    private UpdateService(Context context,ACache aCache,DBHelper dbHelper){
        Log.e(TAG,"init updateService");
        this.mContext = context;
        this.aCache = aCache;
        this.dbHelper = dbHelper;
        cacheUtil = CacheUtil.getInstance(context);
    }

    //main process
    private void runInner() {
        boolean isConnected = isNetworkAvailable(mContext);
        if (!isConnected) {
            Log.e(TAG,"runInner, update is not start cause no network");
            return;
        }
        Log.e(TAG,"runInner, update starts with network");
        if (isConnected) {
            synchronized (this) {
                List<State> states = this.getAllNotConnection();
                Log.e(TAG,"runInner, not connection is "+states.size());
                if (states != null) {
                    while (states.size() > 0 && isConnected) {
                        State s = states.remove(0);
                        UpdateDensity(s);
                    }
                }
            }
        }
    }

    /*
    get all after state
     */
    private List<State> getAllAfterState(State state) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_TIME_POINT_COL + ">=?", state.getTime_point()).list();
        return states;
    }

    /*
    get all not connected
     */
    private List<State> getAllNotConnection() {
        Calendar nowTime = Calendar.getInstance();
        nowTime.add(Calendar.DAY_OF_MONTH, -7);
        nowTime.add(Calendar.MINUTE, -5);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String left = formatter.format(nowTime.getTime());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_CONNECTION + "=?", "0").list();
        return states;
    }

    private void updateStateDensity(State state,String density) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_DENSITY_COL, density);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    private void updateStateConnection(State state,int connection) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_CONNECTION, connection);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    private void updateStateHasUpload(State state,int hasUpload) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, hasUpload);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    private void updateStatePM25(State state,double pm25) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_PM25_COL, pm25);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    /*
    check the network is available
     */
    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
        } else {
            NetworkInfo[] info = cm.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /*
    update state with real density
     */
    public void UpdateDensity(final State state) {
        final String timePoint = ShortcutUtil.refFormatDateAndTimeInHour(Long.valueOf(state.getTime_point()));
        String density = aCache.getAsString(timePoint);
        if (density!=null) {
            FileUtil.appendStrToFile(-1,"cache has density, get from the cache "+timePoint);
            String inOutDoor = state.getOutdoor();
            if (inOutDoor.equals(LocationService.Indoor)) {
                density = Integer.valueOf(density) / 3 + "";
            }
            //update density
            updateStateDensity(state, density);
            //update connection
            updateStateConnection(state, 1);
            //update total pm25 volume
            updateTotalPM25(state, density);
            //upload state and check whether upload success
            state.setDensity(density);
            //have been upload before, upload again
            if (state.getUpload() == 1) {
                upload(state);
            }
        } else {
            FileUtil.appendStrToFile(-1,"cache has no density, get from the server "+timePoint);
            String url = HttpUtil.Search_PM_url + "?longitude=" + state.getLongtitude() + "&latitude=" + state.getLatitude()
                    + "&time_point=" + timePoint;
            url = url.replace(" ", "%20");
            Log.d("url", url);
            //FileUtil.appendStrToFile(-1,"2.UpdateDensity begin with url == "+url);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        Log.d("connection", "connection is ok now");
                        PMModel pmModel = PMModel.parse(response.getJSONObject("data"));
                        String mDensity = String.valueOf(pmModel.getPm25());
                        String inOutDoor = state.getOutdoor();
                        if (inOutDoor.equals(LocationService.Indoor)) {
                            mDensity = Integer.valueOf(mDensity) / 3 + "";
                        }
                        FileUtil.appendStrToFile(-1, "2.UpdateDensity success and density updated == " + mDensity);
                        //put in the cache
                        aCache.put(timePoint,mDensity);
                        //Log.e(TAG,"UpdateDensity new density "+mDensity);
                        //update density
                        updateStateDensity(state, mDensity);
                        //update connection
                        updateStateConnection(state, 1);
                        //update total pm25 volume
                        updateTotalPM25(state, mDensity);
                        //upload state and check whether upload success
                        state.setDensity(mDensity);
                        //have been upload before, upload again
                        if (state.getUpload() == 1) {
                            upload(state);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "JSONException error" + e.toString());
                        FileUtil.appendErrorToFile(0, " update density url JSONException error" + e.toString());
                        // TODO: 16/2/9  A Json parse bug
                        //org.json.JSONException: Value {"data":{"source":2,"time_point":"2016-02-09 00:00:00","PM25":"69","AQI":"93"},"message":"successfully get data","status":1}
                        //of type org.json.JSONObject cannot be converted to JSONArray
                        //Log.e(TAG,"UpdateDensity jsonError "+e.getMessage());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, "UpdateDensity error" + error.toString());
                    //Toast.makeText(mContext.getApplicationContext(), "cannot connect to the server", Toast.LENGTH_SHORT).show();
                }
            });

            VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
        }
    }

    /*
    update total pm2.5
     */
    private void updateTotalPM25(State state, String mDensity) {
        double breath = 0;
        Double density = new Double(Double.valueOf(mDensity)-Double.valueOf(state.getDensity()));
        Boolean mIndoor =  state.getOutdoor().equals("0")?true:false;
        Const.MotionStatus mMotionStatus = state.getStatus().equals("1")? Const.MotionStatus.STATIC:state.getStatus().equals("2")? Const.MotionStatus.WALK: Const.MotionStatus.RUN;
        double static_breath = ShortcutUtil.calStaticBreath(cacheUtil.getAsString(Const.Cache_User_Weight));
        if (mMotionStatus == Const.MotionStatus.STATIC) {
            breath = static_breath;
        } else if (mMotionStatus == Const.MotionStatus.WALK) {
            breath = static_breath * 2.1;
        } else if (mMotionStatus == Const.MotionStatus.RUN) {
            breath = static_breath * 6;
        }
        if (mIndoor) {
            density /= 3;
        }
        double PM25 = density*breath/60/1000;
        List<State> states = this.getAllAfterState(state);
        for (State s : states) {
            this.updateStatePM25(s,Double.valueOf(s.getPm25())+PM25);
        }
    }

    /*
    upload data
     */
    public void upload(final State state)  {
        String url = HttpUtil.Upload_url;

        JSONObject tmp = State.toJsonobject(state, aCache.getAsString(Const.Cache_User_Id));

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, tmp,  new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                updateStateHasUpload(state, 1);
                Log.d("upload","Upload Success");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("hasUpload:", "upload failed");
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=UTF-8");
                return headers;
            }
        };
        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

}

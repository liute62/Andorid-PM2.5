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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.Entity.State;
import app.utils.ACache;
import app.utils.Const;
import app.utils.DBConstants;
import app.utils.DBHelper;
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

    private static UpdateService instance = null;
    boolean isRunning;
    private Context mContext;
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private ACache aCache;

    public static void run(Context context,ACache aCache,DBHelper dbHelper){
        if(instance == null){
            instance = new UpdateService(context,aCache,dbHelper);
        }
        instance.runInner();
    }

    private UpdateService(Context context,ACache aCache,DBHelper dbHelper){
        this.mContext = context;
        this.aCache = aCache;
        this.dbHelper = dbHelper;
        db = dbHelper.getReadableDatabase();
    }

    //main process
    private void runInner() {
        boolean isConnected = isNetworkAvailable(mContext);
        if (!isConnected) {
            Log.d("connection","update is not start cause no network");
            return;
        }
        Log.d("connection","update starts with network");
        if (isConnected) {
            synchronized (this) {
                List<State> states = this.getAllNotConnection();
                Log.d("connection","not connection is "+states.size());
                if (states != null) {
                    while (states.size() > 0 && isConnected) {
                        State s = states.remove(0);
                        UpdateDensity(s);
                    }
                }
            }
        }
        isRunning = false;
    }

    /*
    get all not uploaded
     */
    private List<State> getAllNotConnection() {
        List<State> states = cupboard().withDatabase(db).query(State.class).withSelection(DBConstants.DB_MetaData.STATE_CONNECTION + "=?", "0").list();
        return states;
    }

    private void updateStateDensity(State state,String density) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_DENSITY_COL, density);
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    private void updateStateConnection(State state,int connection) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_CONNECTION, connection);
        cupboard().withDatabase(db).update(State.class,values,"id = ?",state.getId()+"");
    }

    private void updateStateHasUpload(State state,int hasUpload) {
        ContentValues values = new ContentValues();
        values.put(DBConstants.DB_MetaData.STATE_HAS_UPLOAD, hasUpload);
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
        String url = HttpUtil.Search_PM_url + "?longitude=" + state.getLongtitude() + "&latitude=" + state.getLatitude()
                + "&time_point=" + ShortcutUtil.refFormatNowDate(Long.valueOf(state.getTime_point())).substring(0, 19);
        Log.d("url",url);
        JsonArrayRequest jsonObjectRequest = new JsonArrayRequest(Request.Method.GET, url, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    Log.d("connection","connection is ok now");
                    String mDensity = String.valueOf(response.getJSONObject(0).getDouble("PM25"));
                    //update density
                    updateStateDensity(state, mDensity);
                    //update connection
                    updateStateConnection(state, 1);
                    //update total pm25 volume
                    updateTotalPM25(state,mDensity);
                    //upload state and check whether upload success
                    state.setDensity(mDensity);
                    upload(state);
                } catch (JSONException e) {
                    Log.e("error",e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("error",error.toString());
                Toast.makeText(mContext.getApplicationContext(), "cannot connect to the server", Toast.LENGTH_SHORT).show();
            }
        });

        VolleyQueue.getInstance(mContext.getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    /*
    update total pm2.5
     */
    private void updateTotalPM25(State state, String mDensity) {
        double breath = 0;
        Double density = new Double(Double.valueOf(mDensity)-Double.valueOf(state.getDensity()));
        Boolean mIndoor =  state.getOutdoor().equals("0")?true:false;
        Const.MotionStatus mMotionStatus = state.getStatus().equals("1")? Const.MotionStatus.STATIC:state.getStatus().equals("2")? Const.MotionStatus.WALK: Const.MotionStatus.RUN;
        double static_breath = ShortcutUtil.calStaticBreath(aCache.getAsString(Const.Cache_User_Weight));
        if (mMotionStatus == Const.MotionStatus.STATIC) {
            breath = static_breath;
        } else if (mMotionStatus == Const.MotionStatus.WALK) {
            breath = static_breath * 2.1;
        } else if (mMotionStatus == Const.MotionStatus.RUN) {
            breath = static_breath * 6;
        }

        double PM25 = density*breath/60/1000;
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
